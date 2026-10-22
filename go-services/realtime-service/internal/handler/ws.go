package handler

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	"github.com/collabflow/realtime-service/internal/auth"
	"github.com/collabflow/realtime-service/internal/hub"
	"github.com/collabflow/realtime-service/internal/redisx"
	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	// Local MVP — tighten origins when you add a real frontend domain.
	CheckOrigin: func(r *http.Request) bool { return true },
}

type WSHandler struct {
	hub       *hub.Hub
	bus       *redisx.Bus
	jwtSecret string
}

func NewWSHandler(h *hub.Hub, bus *redisx.Bus, jwtSecret string) *WSHandler {
	return &WSHandler{hub: h, bus: bus, jwtSecret: jwtSecret}
}

type clientIn struct {
	Action string `json:"action"` // subscribe | unsubscribe | ping
	Room   string `json:"room"`   // e.g. project:<uuid> or task:<uuid>
}

// WS upgrades to WebSocket. Auth: Authorization Bearer OR ?token=
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

	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("ws upgrade: %v", err)
		return
	}

	client := &hub.Client{
		Hub:    h.hub,
		Conn:   conn,
		Send:   make(chan []byte, 64),
		Rooms:  make(map[string]struct{}),
		UserID: userID.String(),
	}
	h.hub.Register(client)

	go h.writePump(client)
	h.readPump(client)
}

func (h *WSHandler) readPump(client *hub.Client) {
	defer func() {
		h.hub.Unregister(client)
	}()
	_ = client.Conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	client.Conn.SetPongHandler(func(string) error {
		_ = client.Conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		return nil
	})

	for {
		_, raw, err := client.Conn.ReadMessage()
		if err != nil {
			return
		}
		var msg clientIn
		if err := json.Unmarshal(raw, &msg); err != nil {
			continue
		}
		switch msg.Action {
		case "subscribe":
			h.hub.Subscribe(client, msg.Room)
			ack, _ := json.Marshal(map[string]any{"type": "subscribed", "room": msg.Room})
			select {
			case client.Send <- ack:
			default:
			}
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

func (h *WSHandler) writePump(client *hub.Client) {
	ticker := time.NewTicker(30 * time.Second)
	defer func() {
		ticker.Stop()
		_ = client.Conn.Close()
	}()
	for {
		select {
		case msg, ok := <-client.Send:
			_ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if !ok {
				_ = client.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := client.Conn.WriteMessage(websocket.TextMessage, msg); err != nil {
				return
			}
		case <-ticker.C:
			_ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// Broadcast is a manual test endpoint (JWT required at gateway).
// POST /api/v1/realtime/broadcast  {"room":"project:uuid","type":"test","data":{}}
func (h *WSHandler) Broadcast(c *gin.Context) {
	token := c.GetHeader("Authorization")
	if _, err := auth.ParseUserID(h.jwtSecret, token); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "Unauthorized", "errorCode": "UNAUTHORIZED"})
		return
	}
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
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "Published", "data": nil})
}

func Health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"success": true, "message": "OK", "data": gin.H{"status": "UP", "service": "realtime-service"}})
}
