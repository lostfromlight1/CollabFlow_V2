package repository

import (
	"github.com/google/uuid"
	"gorm.io/gorm"
)

// scanOrganizationID reads one organization_id uuid from a raw SELECT.
//
// google/uuid.UUID is a [16]byte. Postgres (via pgx) returns a uuid column
// as text. database/sql then tries to fill that array one byte at a time and
// rejects the text ("converting driver.Value type string to a uint8").
// Read the text, then parse. Same mismatch as a JDBC driver handing back a
// String when the field is a raw byte[].
func scanOrganizationID(db *gorm.DB, query string, args ...any) (uuid.UUID, error) {
	var row struct {
		OrganizationID string `gorm:"column:organization_id"`
	}
	res := db.Raw(query, args...).Scan(&row)
	if res.Error != nil {
		return uuid.Nil, res.Error
	}
	if res.RowsAffected == 0 || row.OrganizationID == "" {
		return uuid.Nil, gorm.ErrRecordNotFound
	}
	id, err := uuid.Parse(row.OrganizationID)
	if err != nil {
		return uuid.Nil, err
	}
	return id, nil
}
