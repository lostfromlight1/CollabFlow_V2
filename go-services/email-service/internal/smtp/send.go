package smtp

import (
	"crypto/tls"
	"fmt"
	"net"
	"net/smtp"
	"strings"
	"time"

	"github.com/collabflow/email-service/internal/config"
)

// Send delivers m via SMTP using cfg.
//
// Local MailHog: SMTP_HOST=localhost SMTP_PORT=1025 SMTP_TLS=false (no auth).
// Provider (Gmail etc.): set SMTP_USER / SMTP_PASSWORD and usually SMTP_TLS=true on port 587.
func Send(cfg config.Config, m Outbound) error {
	if strings.TrimSpace(m.To) == "" {
		return fmt.Errorf("smtp: empty To")
	}
	if strings.TrimSpace(cfg.SMTPHost) == "" {
		return fmt.Errorf("smtp: SMTP_HOST is required")
	}
	from := cfg.SMTPFrom
	if from == "" {
		from = "noreply@collabflow.local"
	}

	addr := net.JoinHostPort(cfg.SMTPHost, cfg.SMTPPort)
	msg := buildMIME(from, m)

	// No credentials → plain SendMail (MailHog / open relay lab).
	if cfg.SMTPUser == "" {
		return smtp.SendMail(addr, nil, from, []string{m.To}, msg)
	}

	auth := smtp.PlainAuth("", cfg.SMTPUser, cfg.SMTPPass, cfg.SMTPHost)
	if !cfg.SMTPTLS {
		return smtp.SendMail(addr, auth, from, []string{m.To}, msg)
	}

	return sendSTARTTLS(addr, cfg.SMTPHost, auth, from, m.To, msg)
}

func sendSTARTTLS(addr, host string, auth smtp.Auth, from, to string, msg []byte) error {
	conn, err := net.DialTimeout("tcp", addr, 15*time.Second)
	if err != nil {
		return fmt.Errorf("smtp dial: %w", err)
	}
	defer conn.Close()

	client, err := smtp.NewClient(conn, host)
	if err != nil {
		return fmt.Errorf("smtp client: %w", err)
	}
	defer client.Close()

	if ok, _ := client.Extension("STARTTLS"); ok {
		tlsConfig := &tls.Config{ServerName: host, MinVersion: tls.VersionTLS12}
		if err := client.StartTLS(tlsConfig); err != nil {
			return fmt.Errorf("smtp starttls: %w", err)
		}
	}
	if auth != nil {
		if ok, _ := client.Extension("AUTH"); ok {
			if err := client.Auth(auth); err != nil {
				return fmt.Errorf("smtp auth: %w", err)
			}
		}
	}
	if err := client.Mail(from); err != nil {
		return err
	}
	if err := client.Rcpt(to); err != nil {
		return err
	}
	w, err := client.Data()
	if err != nil {
		return err
	}
	if _, err := w.Write(msg); err != nil {
		return err
	}
	if err := w.Close(); err != nil {
		return err
	}
	return client.Quit()
}

func buildMIME(from string, m Outbound) []byte {
	var b strings.Builder
	b.WriteString("From: " + from + "\r\n")
	b.WriteString("To: " + m.To + "\r\n")
	b.WriteString("Subject: " + sanitizeHeader(m.Subject) + "\r\n")
	b.WriteString("MIME-Version: 1.0\r\n")
	b.WriteString("Content-Type: text/plain; charset=UTF-8\r\n")
	if m.CorrelationID != "" {
		b.WriteString("X-Correlation-Id: " + sanitizeHeader(m.CorrelationID) + "\r\n")
	}
	if m.Template != "" {
		b.WriteString("X-Template: " + sanitizeHeader(m.Template) + "\r\n")
	}
	if m.EventID != "" {
		b.WriteString("X-Event-Id: " + sanitizeHeader(m.EventID) + "\r\n")
	}
	b.WriteString("\r\n")
	b.WriteString(m.Body)
	return []byte(b.String())
}

func sanitizeHeader(s string) string {
	return strings.Map(func(r rune) rune {
		if r == '\r' || r == '\n' {
			return -1
		}
		return r
	}, s)
}
