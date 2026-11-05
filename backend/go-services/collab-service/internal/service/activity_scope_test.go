package service

import (
	"testing"

	"github.com/collabflow/collab-service/internal/kafka"
)

func TestActivitySpec(t *testing.T) {
	action, entity, ok := activitySpec("project.member_added")
	if !ok || action != "MEMBER_JOINED" || entity != "PROJECT" {
		t.Fatalf("member added: %s %s %v", action, entity, ok)
	}
	action, entity, ok = activitySpec("organization.deleted")
	if !ok || action != "ORGANIZATION_DELETED" || entity != "ORGANIZATION" {
		t.Fatalf("org deleted: %s %s %v", action, entity, ok)
	}
	action, entity, ok = activitySpec("task.deleted")
	if !ok || action != "TASK_DELETED" || entity != "TASK" {
		t.Fatalf("task deleted: %s %s %v", action, entity, ok)
	}
	action, entity, ok = activitySpec("task.status_changed")
	if !ok || action != "TASK_UPDATED" || entity != "TASK" {
		t.Fatalf("status: %s %s %v", action, entity, ok)
	}
	_, _, ok = activitySpec("user.created")
	if ok {
		t.Fatal("user.created must stay without an activity mapping")
	}
}

func TestScopeIDsUsesPayloadOnly(t *testing.T) {
	org := "11111111-1111-1111-1111-111111111111"
	ws := "22222222-2222-2222-2222-222222222222"
	task := "33333333-3333-3333-3333-333333333333"
	project := "44444444-4444-4444-4444-444444444444"
	data := kafka.ScopeEventData{
		OrganizationID: org,
		WorkspaceID:    ws,
		ProjectID:      project,
		TaskID:         task,
	}
	gotOrg, entity, workspaceID, projectID, err := scopeIDs("task.deleted", data)
	if err != nil {
		t.Fatal(err)
	}
	if gotOrg.String() != org || entity.String() != task || workspaceID.String() != ws || projectID.String() != project {
		t.Fatalf("task scope org=%s entity=%s ws=%s prj=%s", gotOrg, entity, workspaceID, projectID)
	}
	_, entity, workspaceID, projectID, err = scopeIDs("organization.deleted", kafka.ScopeEventData{OrganizationID: org})
	if err != nil || entity.String() != org || workspaceID != nil || projectID != nil {
		t.Fatalf("org scope entity=%s ws=%v prj=%v err=%v", entity, workspaceID, projectID, err)
	}
}
