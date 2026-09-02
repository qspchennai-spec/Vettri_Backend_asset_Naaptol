# Render Deployment Inspection

## Project

Vettri Asset Naaptol Spring Boot backend.

This document records the production configuration review and the minimum changes made for deployment. API routes, authentication, permission codes, database entities/table names, existing data, and third-party service URLs were preserved. No deployment or database migration was run.

## 1. Spring Boot and Java

- Spring Boot: `3.3.4`
- Required Java: `21`
- Maven Wrapper version: Maven `3.9.16`
- Application main class: `com.vikkash.assetmanagementv1.AssetManagementV1Application`
- Main source file: `src/main/java/com/vikkash/assetmanagementv1/AssetManagementV1Application.java`

## 2. Render Deployment Recommendation

### Recommended type: Docker

The existing `Dockerfile` can be used directly on Render. It:

- Uses `eclipse-temurin:21-jdk`
- Copies the project into `/app`
- Builds with Maven Wrapper
- Exposes port `8080`
- Starts the generated Spring Boot JAR

The Dockerfile already contains the required build and start behavior:

```dockerfile
RUN ./mvnw clean package -DskipTests
ENTRYPOINT ["java","-jar","target/asset-management-v1-0.0.1-SNAPSHOT.jar"]
```

Render should be configured as a Docker Web Service and should use the repository Dockerfile.

### Maven/Java alternative

If deploying without Docker, use:

Build Command:

```bash
./mvnw clean package -DskipTests
```

Start Command:

```bash
java -jar target/asset-management-v1-0.0.1-SNAPSHOT.jar
```

## 3. Environment Variables

### Required for startup

These properties have no fallback value and must be set on Render:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
CREDENTIAL_ENCRYPTION_SECRET
BREVO_API_KEY
MAIL_FROM
GROQ_API_KEY
FRONTEND_BASE_URL
AWS_S3_BUCKET_NAME
```

`CORS_ALLOWED_ORIGINS` may also be set when more than one frontend origin is required. If it is omitted, CORS uses `FRONTEND_BASE_URL`.

### Required for S3 file operations

The application uses Amazon S3 for invoice storage. Set these on Render:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_S3_REGION
```

Set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` for Render S3 access. The bucket has no Haoda fallback and must be supplied through `AWS_S3_BUCKET_NAME`.

### Strongly recommended production variables

```text
SEED_DEMO_DATA=false
APP_SEED_ADMIN_PASSWORD=<secure-production-password>
APP_SEED_EMPLOYEE_PASSWORD=<secure-initial-employee-password>
ADMIN_RECOVERY_EMAIL=<admin-recovery-email>
ADMIN_2FA_EMAIL=<admin-2fa-email>
ADMIN_ASSIGNMENT_NOTIFICATION_EMAIL=<notification-email>
FRONTEND_BASE_URL=<new-production-frontend-url>
COMPANY_DISPLAY_NAME=Vettri Asset - Naaptol
COMPANY_FILE_CENTER_NAME=Vettri Asset File Center
COMPANY_PULSE_NAME=Vettri Asset Pulse
```

`APP_SEED_EMPLOYEE_PASSWORD` is required when demo seeding is enabled and when an administrator creates or resets an employee password. It has no hard-coded fallback.

### Optional variables and defaults

```text
PORT=8080
AWS_S3_PRESIGNED_URL_EXPIRY_MINUTES=15
MAX_UPLOAD_SIZE=200MB
INVOICE_MAX_UPLOAD_MB=20
DOCUMENT_UPLOAD_DIR=uploads/asset-documents
FILE_CENTER_UPLOAD_DIR=uploads/file-center
FILE_CENTER_MAX_UPLOAD_MB=100
OCR_TESSDATA_PATH=
JWT_EXPIRATION_MS=86400000
MAIL_FROM_NAME=Haoda Asset
MAIL_CC=itsupport@haodapayments.com
OTP_EXPIRY_MINUTES=5
OTP_MAX_ATTEMPTS=3
OTP_RESEND_COOLDOWN_SECONDS=30
CREDENTIAL_UNLOCK_SECONDS=60
GOOGLE_CLIENT_ID=
CORS_ALLOWED_ORIGINS=<comma-separated-frontend-origins>
COMPANY_DISPLAY_NAME=Vettri Asset - Naaptol
COMPANY_FILE_CENTER_NAME=Vettri Asset File Center
COMPANY_PULSE_NAME=Vettri Asset Pulse
GROQ_MODEL=llama-3.3-70b-versatile
```

`GOOGLE_CLIENT_ID` is only needed if Google Sign-In is used.

## 4. PostgreSQL Configuration

PostgreSQL is configured in `src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
```

For Render, use the PostgreSQL database connection URL as `DB_URL`, together with the database username and password.

There are no hard-coded PostgreSQL credentials or hard-coded PostgreSQL connection URLs in the source.

## 5. Database Schema and Startup Data Changes

Hibernate is configured with:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Therefore, Hibernate automatically creates or updates database tables from the JPA entities. The database user must have sufficient schema permissions.

The project does not contain:

- Flyway migrations
- Liquibase migrations
- `schema.sql`
- `data.sql`

However, Java startup runners execute automatically:

1. `EmployeeEmploymentStatusMigration` backfills employee status and login fields.
2. `AssetEmployeeIdMigration` normalizes stored employee IDs.
3. `DataSeeder` seeds roles and permissions.
4. `DataSeeder` also creates demo admin/employees when `SEED_DEMO_DATA` is `true`.

For production, set:

```text
SEED_DEMO_DATA=false
```

Roles and permissions are still seeded even when demo identities are disabled.

## 6. Hard-Coded Credentials and Passwords

Database credentials are not hard-coded.

AWS, JWT, encryption, Brevo, and Groq secrets are read from environment variables.

There is no hard-coded employee password fallback. Employee creation, employee reset, and optional demo seeding use `APP_SEED_EMPLOYEE_PASSWORD`.

Demo seeding defaults to disabled with `SEED_DEMO_DATA=false`. The seeded admin password is read from `APP_SEED_ADMIN_PASSWORD`, or generated randomly only if demo seeding is explicitly enabled without that variable.

## 7. Remaining Haoda Occurrences

Remaining matches are:

- Runtime email template replacement tokens in `EmailService`. These are replaced using `COMPANY_DISPLAY_NAME`, `COMPANY_FILE_CENTER_NAME`, and `MAIL_FROM` before sending.
- Legacy comments and module documentation referring to Haoda File Center, Haoda Pulse, or HaodaAsset. They do not affect runtime behavior or the API contract.
- The ADMS controller comment containing the historical Render hostname. It is documentation only.
- Jio/Haoda invoice vendor detection in `InvoiceExtractionService`. This is retained intentionally to parse existing Haoda/Jio invoices and does not brand the application.
- Existing database permission labels or notification text may still contain old branding because existing production data is not modified. Newly seeded Pulse permission labels use `COMPANY_PULSE_NAME`.

Active production defaults for frontend URLs, notification emails, S3 bucket name, and employee passwords no longer use Haoda values.

## 8. CORS and Frontend URL

CORS is configured from the `CORS_ALLOWED_ORIGINS` environment variable. If it is omitted, the configured `FRONTEND_BASE_URL` is used.

Current allowed origins are:

```text
<the value of CORS_ALLOWED_ORIGINS, or FRONTEND_BASE_URL>
```

Set the exact Vercel frontend origin in `FRONTEND_BASE_URL`. Use `CORS_ALLOWED_ORIGINS` for a comma-separated list of exact origins.

Also set:

```text
FRONTEND_BASE_URL=<new-production-frontend-url>
```

This controls frontend links generated in QR codes and File Center links. It has no old Haoda URL fallback.

## 9. Localhost and Production URLs

There is no hard-coded localhost URL in runtime configuration. Local development can be supported by including `http://localhost:3000` in `CORS_ALLOWED_ORIGINS` when needed.

The backend does not contain a localhost PostgreSQL URL or localhost API URL that must be replaced for Render.

The following fixed external service URLs are intentional and should remain unchanged:

- Google token verification: `https://oauth2.googleapis.com/tokeninfo?id_token=`
- Brevo API: `https://api.brevo.com/v3/smtp/email`
- Groq API: `https://api.groq.com/openai/v1/chat/completions`

## 10. Final Render Checklist

- [ ] Create a Docker Web Service from the repository.
- [ ] Confirm Render uses the existing `Dockerfile`.
- [ ] Set Java/runtime behavior through the Dockerfile's Java 21 image.
- [ ] Configure the PostgreSQL `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- [ ] Set `JWT_SECRET`.
- [ ] Set `CREDENTIAL_ENCRYPTION_SECRET`.
- [ ] Set Brevo variables: `BREVO_API_KEY` and `MAIL_FROM`.
- [ ] Set `GROQ_API_KEY` if AI features are enabled.
- [ ] Set AWS S3 variables for file storage.
- [ ] Set `SEED_DEMO_DATA=false`.
- [ ] Set `APP_SEED_ADMIN_PASSWORD`.
- [ ] Set `APP_SEED_EMPLOYEE_PASSWORD` before employee creation/reset or demo seeding.
- [ ] Set company branding variables for Vettri Asset - Naaptol.
- [ ] Set `FRONTEND_BASE_URL` to the new Vercel URL.
- [ ] Set `CORS_ALLOWED_ORIGINS` if multiple frontend origins are required.
- [ ] Verify the Render service listens on Render's `PORT` value.

## 11. Files Changed

- `src/main/java/com/vikkash/assetmanagementv1/config/DataSeeder.java`
- `src/main/java/com/vikkash/assetmanagementv1/config/SecurityConfig.java`
- `src/main/java/com/vikkash/assetmanagementv1/service/AdminService.java`
- `src/main/java/com/vikkash/assetmanagementv1/service/AssetService.java`
- `src/main/java/com/vikkash/assetmanagementv1/service/EmailService.java`
- `src/main/java/com/vikkash/assetmanagementv1/service/EmployeeService.java`
- `src/main/java/com/vikkash/assetmanagementv1/service/NotificationService.java`
- `src/main/java/com/vikkash/assetmanagementv1/service/QrCodeService.java`
- `src/main/java/com/vikkash/assetmanagementv1/service/SharedFileService.java`
- `src/main/java/com/vikkash/assetmanagementv1/service/ai/AiAssistantOrchestrator.java`
- `src/main/resources/application.properties`
- `RENDER_DEPLOYMENT_INSPECTION.md`

## 12. Build Verification

Command run:

```bash
./mvnw clean package -DskipTests
```

Result: `BUILD SUCCESS`.
