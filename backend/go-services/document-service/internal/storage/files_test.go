package storage

import "testing"

func TestSafeNameStripsDirectories(t *testing.T) {
	got, err := SafeName(`..\secret\notes.txt`)
	if err != nil {
		t.Fatal(err)
	}
	if got != "notes.txt" {
		t.Fatalf("got %q", got)
	}
}

func TestSafeNameRejectsBlank(t *testing.T) {
	if _, err := SafeName("   "); err == nil {
		t.Fatal("expected error")
	}
}
