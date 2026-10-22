package acl

import (
	"testing"

	"github.com/google/uuid"
)

func TestParseRoom(t *testing.T) {
	id := uuid.MustParse("11111111-1111-4111-8111-111111111111")

	prefix, got, err := parseRoom("project:" + id.String())
	if err != nil || prefix != "project" || got != id {
		t.Fatalf("project room: prefix=%s id=%s err=%v", prefix, got, err)
	}

	prefix, got, err = parseRoom("ORG:" + id.String())
	if err != nil || prefix != "org" || got != id {
		t.Fatalf("org room case: prefix=%s id=%s err=%v", prefix, got, err)
	}

	if _, _, err := parseRoom("project"); err == nil {
		t.Fatal("expected error for missing id")
	}
	if _, _, err := parseRoom("project:not-a-uuid"); err == nil {
		t.Fatal("expected error for bad uuid")
	}
}
