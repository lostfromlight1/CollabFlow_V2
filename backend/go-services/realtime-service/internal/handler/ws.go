package handler

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"time"

	"github.com/collabflow/realtime-service/internal/acl"
	"github.com/collabflow/realtime-service/internal/auth"
	"github.com/collabflow/realtime-service/internal/config"
	"github.com/collabflow/realtime-service/internal/hub"
	"github.com/collabflow/realtime-service/internal/redisx"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/gorilla/websocket"
)

const (
	// Idle clients are dropped if no read (including pong) within this window.
	wsReadIdle = 90 * time.Second
	// Server → client WebSocket ping interval (browser auto-replies with pong).
	wsPingEvery = 30 * time.Second
	wsWriteWait = 10 * time.Second
)

var upgrader = websocket.Upgrader{
	// Local MVP — tighten origins when you add a real frontend domain.
	CheckOrigin: func(r *http.Request) bool { return true },
}

type WSHandler struct {
	hub       *hub.Hub
	bus       *redisx.Bus
	acl       *acl.Checker
	jwtSecret string
}

func NewWSHandler(h *hub.Hub, bus *redisx.Bus, checker *acl.Checker, jwtSecret string) *WSHandler {
	return &WSHandler{hub: h, bus: bus, acl: checker, jwtSecret: jwtSecret}
}

type clientIn struct {
	Action string `json:"action"` // subscribe | unsubscribe | ping
	Room   string `json:"room"`   // e.g. project:<uuid>, task:<uuid>, org:<uuid>
}

// WS upgrades to WebSocket. Auth: Authorization Bearer OR ?token=
// Optional org context: X-Organization-Id (must be an org the user belongs to).
// GET /api/v1/realtime/ws
func (h *WSHandler) WS(c *gin.Context) {
	token := c.GetHeader("Authorization")
	if token == "" {
		token = c.Query("token")
	}
	userID, err := auth.ParseUserID(h.jwtSecret, token)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized", "errorCode": "UNAUTHORIZED"})
		return
	}

	requestID := c.GetHeader("X-Request-Id")
	if requestID == "" {
		requestID = c.GetHeader("X-Request-ID")
	}

	orgIDStr := c.GetHeader("X-Organization-Id")
	if orgIDStr == "" {
		orgIDStr = c.Query("organizationId")
	}
	var orgID uuid.UUID
	if orgIDStr != "" {
		parsed, err := uuid.Parse(orgIDStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": "Invalid X-Organization-Id", "errorCode": "VALIDATION_FAILED"})
			return
		}
		ok, err := h.acl.IsOrgMember(c.Request.Context(), parsed, userID)
		if err != nil {
			log.Printf("acl org check error userId=%s requestId=%s err=%v", userID, requestID, err)
			c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": "ACL check failed", "errorCode": "INTERNAL_ERROR"})
			return
		}
		if !ok {
			c.JSON(http.StatusForbidden, gin.H{"success": false, "message": "Not an organization member", "errorCode": "FORBIDDEN"})
			return
		}
		orgID = parsed
	}

	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("ws upgrade userId=%s requestId=%s err=%v", userID, requestID, err)
		return
	}

	client := &hub.Client{
		Hub:       h.hub,
		Conn:      conn,
		Send:      make(chan []byte, 64),
		Rooms:     make(map[string]struct{}),
		UserID:    userID.String(),
		OrgID:     orgID.String(),
		RequestID: requestID,
	}
	if orgID == uuid.Nil {
		client.OrgID = ""
	}
	h.hub.Register(client)

	go h.writePump(client)
	h.readPump(client, userID)
}

func (h *WSHandler) readPump(client *hub.Client, userID uuid.UUID) {
	defer func() {
		h.hub.Unregister(client)
	}()
	_ = client.Conn.SetReadDeadline(time.Now().Add(wsReadIdle))
	client.Conn.SetPongHandler(func(string) error {
		_ = client.Conn.SetReadDeadline(time.Now().Add(wsReadIdle))
		return nil
	})

	var optionalOrg *uuid.UUID
	if client.OrgID != "" {
		if id, err := uuid.Parse(client.OrgID); err == nil {
			optionalOrg = &id
		}
	}

	for {
		_, raw, err := client.Conn.ReadMessage()
		if err != nil {
			return
		}
		// Any inbound frame (including client ping JSON) refreshes idle deadline.
		_ = client.Conn.SetReadDeadline(time.Now().Add(wsReadIdle))

		var msg clientIn
		if err := json.Unmarshal(raw, &msg); err != nil {
			continue
		}
		switch msg.Action {
		case "subscribe":
			h.handleSubscribe(client, userID, msg.Room, optionalOrg)
		case "unsubscribe":
			h.hub.Unsubscribe(client, msg.Room)
		case "ping":
			pong, _ := json.Marshal(map[string]string{"type": "pong"})
			select {
			case client.Send <- pong:
			default:
			}
		}
	}
}

func (h *WSHandler) handleSubscribe(client *hub.Client, userID uuid.UUID, room string, optionalOrg *uuid.UUID) {
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	if err := h.acl.AllowSubscribe(ctx, userID, room, optionalOrg); err != nil {
		log.Printf("subscribe denied userId=%s room=%s requestId=%s reason=%v", client.UserID, room, client.RequestID, err)
		body, _ := json.Marshal(map[string]any{
			"type":    "error",
			"code":    "FORBIDDEN",
			"message": err.Error(),
			"room":    room,
		})
		select {
		case client.Send <- body:
		default:
		}
		return
	}

	h.hub.Subscribe(client, room)
	ack, _ := json.Marshal(map[string]any{"type": "subscribed", "room": room})
	select {
	case client.Send <- ack:
	default:
	}
}

func (h *WSHandler) writePump(client *hub.Client) {
	ticker := time.NewTicker(wsPingEvery)
	defer func() {
		ticker.Stop()
		_ = client.Conn.Close()
	}()
	for {
		select {
		case msg, ok := <-client.Send:
			_ = client.Conn.SetWriteDeadline(time.Now().Add(wsWriteWait))
			if !ok {
				_ = client.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := client.Conn.WriteMessage(websocket.TextMessage, msg); err != nil {
				return
			}
		case <-ticker.C:
			_ = client.Conn.SetWriteDeadline(time.Now().Add(wsWriteWait))
			if err := client.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// Broadcast is a manual test endpoint — only when GIN_MODE=debug or APP_ENV=dev.
// POST /api/v1/realtime/broadcast  {"room":"project:uuid","type":"test","data":{}}
func (h *WSHandler) Broadcast(c *gin.Context) {
	if !config.BroadcastAllowed() {
		c.JSON(http.StatusForbidden, gin.H{
			"success":   false,
			"message":   "Broadcast is disabled outside GIN_MODE=debug / APP_ENV=dev",
			"errorCode": "FORBIDDEN",
		})
		return
	}

	token := c.GetHeader("Authorization")
	userID, err := auth.ParseUserID(h.jwtSecret, token)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized", "errorCode": "UNAUTHORIZED"})
		return
	}
	requestID := c.GetHeader("X-Request-Id")

	var req struct {
		Room string `json:"room" binding:"required"`
		Type string `json:"type"`
		Data any    `json:"data"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"success": false, "message": err.Error(), "errorCode": "VALIDATION_FAILED"})
		return
	}
	if req.Type == "" {
		req.Type = "broadcast"
	}
	if err := h.bus.Publish(c.Request.Context(), req.Room, req.Type, req.Data); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"success": false, "message": err.Error(), "errorCode": "INTERNAL_ERROR"})
		return
	}
	log.Printf("broadcast published userId=%s room=%s type=%s requestId=%s", userID, req.Room, req.Type, requestID)
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "Published", "data": nil})
}

func Health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": gin.H{"status": "UP", "service": "realtime-service"}})
}
