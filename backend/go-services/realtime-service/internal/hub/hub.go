package hub

import (
	"encoding/json"
	"log"
	"sync"

	"github.com/gorilla/websocket"
)

// Client is one WebSocket connection that can join many rooms.
type Client struct {
	Hub       *Hub
	Conn      *websocket.Conn
	Send      chan []byte
	Rooms     map[string]struct{}
	UserID    string
	OrgID     string // optional; from X-Organization-Id after membership check
	RequestID string // from X-Request-Id (gateway) for structured logs
	mu        sync.Mutex
}

// Hub keeps in-process room → clients (Java analogy: local STOMP session registry).
type Hub struct {
	mu         sync.RWMutex
	rooms      map[string]map[*Client]struct{}
	register   chan *Client
	unregister chan *Client
}

func New() *Hub {
	return &Hub{
		rooms:      make(map[string]map[*Client]struct{}),
		register:   make(chan *Client),
		unregister: make(chan *Client),
	}
}

func (h *Hub) Run() {
	for {
		select {
		case c := <-h.register:
			log.Printf("ws client registered userId=%s orgId=%s requestId=%s", c.UserID, c.OrgID, c.RequestID)
		case c := <-h.unregister:
			h.leaveAll(c)
			close(c.Send)
			_ = c.Conn.Close()
			log.Printf("ws client unregistered userId=%s requestId=%s", c.UserID, c.RequestID)
		}
	}
}

func (h *Hub) Register(c *Client)   { h.register <- c }
func (h *Hub) Unregister(c *Client) { h.unregister <- c }

func (h *Hub) Subscribe(c *Client, room string) {
	if room == "" {
		return
	}
	h.mu.Lock()
	defer h.mu.Unlock()
	if h.rooms[room] == nil {
		h.rooms[room] = make(map[*Client]struct{})
	}
	h.rooms[room][c] = struct{}{}
	c.mu.Lock()
	c.Rooms[room] = struct{}{}
	c.mu.Unlock()
	log.Printf("subscribe userId=%s orgId=%s room=%s requestId=%s", c.UserID, c.OrgID, room, c.RequestID)
}

func (h *Hub) Unsubscribe(c *Client, room string) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if m, ok := h.rooms[room]; ok {
		delete(m, c)
		if len(m) == 0 {
			delete(h.rooms, room)
		}
	}
	c.mu.Lock()
	delete(c.Rooms, room)
	c.mu.Unlock()
}

func (h *Hub) leaveAll(c *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	c.mu.Lock()
	rooms := make([]string, 0, len(c.Rooms))
	for r := range c.Rooms {
		rooms = append(rooms, r)
	}
	c.Rooms = make(map[string]struct{})
	c.mu.Unlock()
	for _, room := range rooms {
		if m, ok := h.rooms[room]; ok {
			delete(m, c)
			if len(m) == 0 {
				delete(h.rooms, room)
			}
		}
	}
}

// BroadcastLocal sends payload to every client in room on THIS process.
func (h *Hub) BroadcastLocal(room string, payload []byte) {
	h.mu.RLock()
	clients := h.rooms[room]
	h.mu.RUnlock()
	for c := range clients {
		select {
		case c.Send <- payload:
		default:
			// Slow client — drop rather than block the hub
			log.Printf("drop message for slow client userId=%s room=%s requestId=%s", c.UserID, room, c.RequestID)
		}
	}
}

// Outbound envelope written to the socket.
type OutMessage struct {
	Type string          `json:"type"`
	Room string          `json:"room"`
	Data json.RawMessage `json:"data"`
}

func (h *Hub) BroadcastJSON(room, eventType string, data any) {
	raw, err := json.Marshal(data)
	if err != nil {
		return
	}
	body, err := json.Marshal(OutMessage{Type: eventType, Room: room, Data: raw})
	if err != nil {
		return
	}
	h.BroadcastLocal(room, body)
}
