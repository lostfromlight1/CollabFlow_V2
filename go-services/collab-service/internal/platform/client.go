package platform

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
)

// Seeded platform role IDs (same as Java SystemRoles / previous collab AcceptInTx).
var (
	RoleOrgMember       = uuid.MustParse("a0000000-0000-4000-8000-000000000003")
	RoleWorkspaceMember = uuid.MustParse("a0000000-0000-4000-8000-000000000005")
	RoleProjectMember   = uuid.MustParse("a0000000-0000-4000-8000-000000000008")
)

// Client calls platform-service membership APIs over HTTP.
// Why HTTP (not shared-DB insert): platform owns organization_members / workspace_members /
// project_members — collab must not write those tables. Java analogy: RestClient / WebClient
// from one Spring service to another, with X-API-Key + forwarded Bearer JWT.
type Client struct {
	baseURL    string
	apiKey     string
	httpClient *http.Client
}

func NewClient(baseURL, apiKey string) *Client {
	return &Client{
		baseURL: strings.TrimRight(strings.TrimSpace(baseURL), "/"),
		apiKey:  apiKey,
		httpClient: &http.Client{
			Timeout: 15 * time.Second,
		},
	}
}

// AddMemberRequest matches Java com.collabflow.platform.dto.request.AddMemberRequest
// (and workspace/project variants: same userId + roleId shape).
type AddMemberRequest struct {
	UserID uuid.UUID `json:"userId"`
	RoleID uuid.UUID `json:"roleId"`
}

// AddOrgMember → POST /api/v1/organizations/{id}/members (ORG-007).
// bearerToken is the accept-request JWT (Authorization value without requiring "Bearer " prefix).
// 409 Conflict (already a member) is treated as success so accept is idempotent.
func (c *Client) AddOrgMember(ctx context.Context, orgID, userID, roleID uuid.UUID, bearerToken string) error {
	path := fmt.Sprintf("/api/v1/organizations/%s/members", orgID)
	return c.postMember(ctx, path, userID, roleID, bearerToken)
}

// AddWorkspaceMember → POST /api/v1/workspaces/{id}/members (WS-007).
func (c *Client) AddWorkspaceMember(ctx context.Context, workspaceID, userID, roleID uuid.UUID, bearerToken string) error {
	path := fmt.Sprintf("/api/v1/workspaces/%s/members", workspaceID)
	return c.postMember(ctx, path, userID, roleID, bearerToken)
}

// AddProjectMember → POST /api/v1/projects/{id}/members (PRJ-007).
func (c *Client) AddProjectMember(ctx context.Context, projectID, userID, roleID uuid.UUID, bearerToken string) error {
	path := fmt.Sprintf("/api/v1/projects/%s/members", projectID)
	return c.postMember(ctx, path, userID, roleID, bearerToken)
}

func (c *Client) postMember(ctx context.Context, path string, userID, roleID uuid.UUID, bearerToken string) error {
	if c.baseURL == "" {
		return fmt.Errorf("PLATFORM_SERVICE_URI is not configured")
	}
	if c.apiKey == "" {
		return fmt.Errorf("API_KEY is not configured")
	}
	body, err := json.Marshal(AddMemberRequest{UserID: userID, RoleID: roleID})
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+path, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-API-Key", c.apiKey)
	token := strings.TrimSpace(bearerToken)
	token = strings.TrimPrefix(token, "Bearer ")
	if token == "" {
		return fmt.Errorf("missing Authorization bearer token for platform call")
	}
	req.Header.Set("Authorization", "Bearer "+token)

	res, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("platform request: %w", err)
	}
	defer res.Body.Close()
	raw, _ := io.ReadAll(io.LimitReader(res.Body, 4096))

	// 201 Created or 200 OK
	if res.StatusCode == http.StatusCreated || res.StatusCode == http.StatusOK {
		return nil
	}
	// Already a member — accept may be retried
	if res.StatusCode == http.StatusConflict {
		return nil
	}
	msg := strings.TrimSpace(string(raw))
	if msg == "" {
		msg = res.Status
	}
	return fmt.Errorf("platform %s returned %d: %s", path, res.StatusCode, msg)
}
