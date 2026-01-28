# JBH Notification Module

## Overview

The **jbh-notification** module is responsible for registering and sending notifications to users through multiple channels:

- **EMAIL** - Email notifications via SMTP (Gmail, Outlook, etc.)
- **SMS** - SMS notifications (not yet implemented)
- **PUSH** - Push notifications (not yet implemented)
- **IN_APP** - In-app notifications stored in database

This module follows **Hexagonal Architecture** (Ports & Adapters) pattern, ensuring clean separation between business logic and infrastructure concerns.

---

## Architecture

```
jbh-notification/
└── jbh-notification-infra/
    ├── adapters/
    │   ├── in/rest/           # REST API endpoints
    │   └── out/
    │       ├── email/         # Email sender (Quarkus Mailer)
    │       └── persistence/   # Database persistence
    ├── ports/output/          # Output port interfaces
    ├── service/               # Business logic orchestration
    └── persistence/           # JPA entities
```

---

## API Endpoints

### Send Notification

```
POST /jbh-api/notifications/v1?type={NOTIFICATION_TYPE}
```

**Query Parameters:**

| Parameter | Type | Required | Default | Description                                         |
|-----------|------|----------|---------|-----------------------------------------------------|
| `type`    | enum | No       | `EMAIL` | Notification type: `EMAIL`, `SMS`, `PUSH`, `IN_APP` |

**Request Body:**

```json
{
  "recipientId": "550e8400-e29b-41d4-a716-446655440000",
  "recipientEmail": "recipient@example.com",
  "senderUserId": "550e8400-e29b-41d4-a716-446655440001",
  "senderEmail": "sender@example.com",
  "subject": "Welcome to JBH Finance",
  "message": "Thank you for signing up!",
  "metadata": {
    "templateId": "welcome-email",
    "priority": "high"
  }
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "recipientId": "550e8400-e29b-41d4-a716-446655440000",
    "recipientEmail": "recipient@example.com",
    "subject": "Welcome to JBH Finance",
    "notificationType": "EMAIL",
    "status": "PENDING",
    "read": false,
    "createdAt": "2024-01-15T10:30:00"
  },
  "message": "Notification processed successfully"
}
```

---

## Environment Variables

### Database Configuration

| Variable            | Required | Default                                        | Description         |
|---------------------|----------|------------------------------------------------|---------------------|
| `DATABASE_USERNAME` | No       | `postgres`                                     | PostgreSQL username |
| `DATABASE_PASSWORD` | No       | `postgres`                                     | PostgreSQL password |
| `DATABASE_URL`      | No       | `jdbc:postgresql://localhost:5432/jbh_finance` | JDBC connection URL |

### Email Configuration (Gmail SMTP)

| Variable             | Required | Default                   | Description                                  |
|----------------------|----------|---------------------------|----------------------------------------------|
| `GMAIL_USERNAME`     | Yes*     | -                         | Gmail address (e.g., `your-email@gmail.com`) |
| `GMAIL_APP_PASSWORD` | Yes*     | -                         | Gmail App Password (16 characters)           |
| `GMAIL_FROM`         | No       | `noreply@jbh-finance.com` | Sender "From" address                        |
| `GMAIL_HOST`         | No       | `smtp.gmail.com`          | SMTP host                                    |
| `GMAIL_PORT`         | No       | `465`                     | SMTP port (465 for SSL, 587 for TLS)         |

> *Required only when using real email sending (production or `real-email` profile)

---

## Email Provider Configuration

### Development Mode (Default)

By default, in development mode, the mailer runs in **mock mode**:

- Emails are **NOT sent** to real recipients
- Email content is **logged to console** for debugging
- No SMTP credentials required

```bash
# Start in dev mode (mock emails)
mvn quarkus:dev
```

Console output example:

```
INFO  [io.qua.mailer] Sending email to: recipient@example.com
INFO  [io.qua.mailer] Subject: Welcome to JBH Finance
INFO  [io.qua.mailer] Body: Thank you for signing up!
```

### Development with Real Emails

To test with real email sending locally, use the `real-email` profile:

```bash
# 1. Set Gmail credentials
export GMAIL_USERNAME=your-email@gmail.com
export GMAIL_APP_PASSWORD=abcd-efgh-ijkl-mnop

# 2. Run with real-email profile
mvn quarkus:dev -Dquarkus.profile=dev,real-email
```

### Production Mode

In production, emails are sent via Gmail SMTP by default. Ensure all required environment variables are configured:

```bash
export GMAIL_USERNAME=notifications@your-company.com
export GMAIL_APP_PASSWORD=your-app-password
export GMAIL_FROM=notifications@your-company.com
```

---

## Gmail App Password Setup

Gmail requires an **App Password** instead of your regular password when 2FA is enabled (recommended).

### Steps to Generate App Password:

1. **Enable 2-Factor Authentication**
    - Go to [Google Account Security](https://myaccount.google.com/security)
    - Enable "2-Step Verification"

2. **Generate App Password**
    - Go to [App Passwords](https://myaccount.google.com/apppasswords)
    - Select "Mail" as the app
    - Select your device
    - Click "Generate"

3. **Copy the 16-character password**
    - Use this as `GMAIL_APP_PASSWORD`
    - Store securely (you won't be able to see it again)

---

## Alternative Email Providers

The module uses Quarkus Mailer which supports any SMTP server. To use a different provider, override the environment variables:

### Outlook / Microsoft 365

```bash
export GMAIL_HOST=smtp.office365.com
export GMAIL_PORT=587
export GMAIL_USERNAME=your-email@outlook.com
export GMAIL_APP_PASSWORD=your-password
```

### SendGrid

```bash
export GMAIL_HOST=smtp.sendgrid.net
export GMAIL_PORT=465
export GMAIL_USERNAME=apikey
export GMAIL_APP_PASSWORD=your-sendgrid-api-key
```

### Mailtrap (Testing)

```bash
export GMAIL_HOST=smtp.mailtrap.io
export GMAIL_PORT=587
export GMAIL_USERNAME=your-mailtrap-username
export GMAIL_APP_PASSWORD=your-mailtrap-password
```

---

## Database Schema

The module uses its own PostgreSQL schema: `notifications`

### Table: `notifications.notification`

| Column              | Type         | Nullable | Description                                    |
|---------------------|--------------|----------|------------------------------------------------|
| `id`                | UUID         | NO       | Primary key (auto-generated)                   |
| `recipient_id`      | UUID         | NO       | User ID of the recipient                       |
| `recipient_email`   | VARCHAR(255) | NO       | Email address of recipient                     |
| `sender_user_id`    | UUID         | YES      | User ID of sender (optional)                   |
| `sender_email`      | VARCHAR(255) | YES      | Email address of sender (optional)             |
| `subject`           | VARCHAR(500) | NO       | Notification subject                           |
| `message`           | TEXT         | NO       | Notification content                           |
| `notification_type` | VARCHAR(50)  | NO       | Type: EMAIL, SMS, PUSH, IN_APP                 |
| `status`            | VARCHAR(50)  | NO       | Status: PENDING, SENT, FAILED, CANCELLED, READ |
| `read`              | BOOLEAN      | NO       | Whether notification has been read             |
| `metadata`          | JSONB        | YES      | Additional metadata (flexible JSON)            |
| `sent_at`           | TIMESTAMP    | YES      | When notification was sent                     |
| `created_at`        | TIMESTAMP    | NO       | Creation timestamp                             |
| `updated_at`        | TIMESTAMP    | NO       | Last update timestamp                          |

---

## Notification Flow

```
1. API Request
   └── POST /jbh-api/notifications/v1?type=EMAIL

2. NotificationRestAdapter
   └── Validates request
   └── Maps to DTO

3. NotificationService
   └── Saves notification to DB (status: PENDING)
   └── Routes to appropriate sender based on type

4. QuarkusMailerAdapter (for EMAIL)
   └── Sends email asynchronously
   └── Updates status to SENT or FAILED

5. Response returned immediately
   └── Email sending happens in background
```

---

## Notification Statuses

| Status      | Description                                           |
|-------------|-------------------------------------------------------|
| `PENDING`   | Notification created, waiting to be processed         |
| `SENT`      | Notification successfully sent                        |
| `FAILED`    | Notification failed to send                           |
| `CANCELLED` | Notification was cancelled                            |
| `READ`      | Notification has been read by recipient (IN_APP only) |

---

## Local Development

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 14+ (or Docker)
- Gmail account with App Password (for real email testing)

### Quick Start

```bash
# 1. Start PostgreSQL (if using Docker)
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=jbh_finance \
  -p 5432:5432 postgres:14

# 2. Run in dev mode (mock emails)
cd jbh-notification/jbh-notification-infra
mvn quarkus:dev

# 3. Test the API
curl -X POST "http://localhost:8080/jbh-api/notifications/v1?type=EMAIL" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "550e8400-e29b-41d4-a716-446655440000",
    "recipientEmail": "test@example.com",
    "subject": "Test Email",
    "message": "This is a test notification"
  }'
```

---

## Troubleshooting

### Email not sending in dev mode

**Expected behavior**: In dev mode, emails are mocked by default. Check console logs for email content.

### Authentication failed with Gmail

1. Ensure 2FA is enabled on your Google account
2. Use App Password, not your regular password
3. Check that "Less secure app access" is not blocking (shouldn't be needed with App Password)

### Connection timeout to SMTP

1. Check firewall rules for port 465/587
2. Verify SMTP host and port are correct
3. Some corporate networks block SMTP ports

### Liquibase migration failed

1. Ensure the `notifications` schema exists or Liquibase can create it
2. Check database credentials are correct
3. Verify PostgreSQL is running and accessible

---

## Future Enhancements

- [ ] SMS notifications via Twilio
- [ ] Push notifications via Firebase Cloud Messaging
- [ ] Email templates with Qute/Freemarker
- [ ] Notification preferences per user
- [ ] Retry mechanism for failed notifications
- [ ] Rate limiting
- [ ] Batch sending
