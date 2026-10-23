package storage

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
)

// SafeName keeps a display name and drops any directory the client sent.
func SafeName(name string) (string, error) {
	base := filepath.Base(strings.TrimSpace(name))
	base = strings.ReplaceAll(base, "\x00", "")
	if base == "" || base == "." || base == ".." {
		return "", fmt.Errorf("invalid file name")
	}
	if len(base) > 255 {
		base = base[:255]
	}
	return base, nil
}

// Save writes bytes under dir/<id> so the stored path never comes from the user.
func Save(dir, id string, src io.Reader) (int64, error) {
	if err := os.MkdirAll(dir, 0o750); err != nil {
		return 0, err
	}
	path := filepath.Join(dir, id)
	f, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0o640)
	if err != nil {
		return 0, err
	}
	defer f.Close()
	n, err := io.Copy(f, src)
	return n, err
}

func Open(path string) (*os.File, error) {
	return os.Open(path)
}
