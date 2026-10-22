package config

import "os"

// Config holds runtime settings from environment variables.
// Local default targets MailHog (docker compose): host localhost:1025, no TLS/auth.
type Config struct {
	HTTPPort     string
	KafkaBrokers string
	KafkaGroupID string
	SMTPHost     string
	SMTPPort     string
	SMTPUser     string
	SMTPPass     string
	SMTPFrom     string
	SMTPTLS      bool
}

// Load reads env with the same Kafka defaults as collab-service.
func Load() Config {
	return Config{
		HTTPPort:     getenv("EMAIL_HTTP_PORT", "9084"),
		KafkaBrokers: getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
		// Separate group from collab so both can coexist (each gets a copy of email.send).
		KafkaGroupID: getenv("EMAIL_KAFKA_GROUP_ID", getenv("KAFKA_GROUP_ID", "email-service")),
		SMTPHost:     getenv("SMTP_HOST", "localhost"),
		SMTPPort:     getenv("SMTP_PORT", "1025"),
		SMTPUser:     getenv("SMTP_USER", ""),
		SMTPPass:     getenv("SMTP_PASSWORD", ""),
		SMTPFrom:     getenv("SMTP_FROM", "noreply@collabflow.local"),
		// MailHog: false. Real providers on :587 usually true.
		SMTPTLS: getenv("SMTP_TLS", "false") == "true",
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
