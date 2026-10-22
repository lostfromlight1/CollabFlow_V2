package config

import "os"

// Config holds runtime settings from environment variables.
// Java analogy: application.yml + @ConfigurationProperties.
// MinIO fields are placeholders — Wave 7 scaffold does not dial object storage yet.
type Config struct {
	HTTPPort      string
	MinIOEndpoint string
	MinIOBucket   string
	MinIOAccess   string
	MinIOSecret   string
	MinIOUseSSL   bool
}

// Load reads env with local defaults suitable for docker-compose later.
func Load() Config {
	return Config{
		HTTPPort:      getenv("DOCUMENT_HTTP_PORT", "9083"),
		MinIOEndpoint: getenv("MINIO_ENDPOINT", "localhost:9000"),
		MinIOBucket:   getenv("MINIO_BUCKET", "collabflow-documents"),
		MinIOAccess:   getenv("MINIO_ACCESS_KEY", "minioadmin"),
		MinIOSecret:   getenv("MINIO_SECRET_KEY", "minioadmin"),
		MinIOUseSSL:   getenv("MINIO_USE_SSL", "false") == "true",
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
