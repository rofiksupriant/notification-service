# Security Guidelines

## 🔒 Credential Management

This project uses **[spring-dotenv](https://github.com/paulschwarz/spring-dotenv)** to automatically load environment variables from `.env` files during development.

### How It Works
1. Create a `.env` file in the project root (use `.env.example` as template)
2. Add your credentials in `KEY=value` format
3. Spring Boot automatically loads these as environment variables
4. No code changes needed - it just works!

### ✅ DO
- Store all sensitive credentials in environment variables
- Use `.env.example` as a template for local development
- Copy `.env.example` to `.env` and fill in your actual values
- Keep `.env` file in your local environment only
- Use secret management services (AWS Secrets Manager, Azure Key Vault) in production
- Rotate credentials regularly
- Use different credentials for each environment (dev, staging, production)

### ❌ DON'T
- Never commit `.env` file to version control
- Never hardcode credentials in YAML files
- Never commit `application-local.yml` or `application-dev.yml`
- Never share credentials via email, chat, or public channels
- Never use production credentials in development/test environments
- Never commit API keys, passwords, or tokens in code

## 📝 Environment Variables Required

The following environment variables must be configured before running the application.
All variables use the `NOTIF_` prefix to clearly namespace them for this notification service.

### Database
```bash
NOTIF_DB_USERNAME=postgres
NOTIF_DB_PASSWORD=your-secure-password
```

### Email (Gmail SMTP)
```bash
NOTIF_MAIL_USERNAME=your-email@gmail.com
NOTIF_MAIL_PASSWORD=your-gmail-app-password
```

**Note:** For Gmail, use [App Passwords](https://support.google.com/accounts/answer/185833), not your regular password.

### RabbitMQ
```bash
NOTIF_RABBITMQ_HOST=localhost
NOTIF_RABBITMQ_PORT=5672
NOTIF_RABBITMQ_USERNAME=guest
NOTIF_RABBITMQ_PASSWORD=guest
NOTIF_RABBITMQ_ENABLED=true
```

### API Security
```bash
NOTIF_API_SECRET=your-secret-key-minimum-32-characters
```

**Note:** Use a strong, randomly generated key. Never use simple values like "secret" or "password".

### WhatsApp Integration (Watzap.id)
```bash
NOTIF_WATZAP_API_KEY=your-watzap-api-key
NOTIF_WATZAP_NUMBER_KEY=your-watzap-number-key
```

**Note:** Get your credentials from [Watzap.id Dashboard](https://watzap.id/)

## 🔐 API Key Generation

Generate secure API keys using:

```bash
# Linux/Mac
openssl rand -base64 32

# PowerShell (Windows)
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))

# Python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

## 🛡️ Security Best Practices

### 1. Local Development
```bash
# Create .env from example
cp .env.example .env

# Edit with your local credentials
nano .env

# Verify .env is in .gitignore
git check-ignore .env  # Should output: .env
```

### 2. CI/CD Pipelines
- Use GitHub Secrets, GitLab CI/CD Variables, or similar
- Never log environment variables in CI output
- Use separate credentials for test environments

### 3. Production Deployment
- Use managed secret services (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault)
- Enable secret rotation
- Use IAM roles/managed identities when possible
- Encrypt secrets at rest and in transit

### 4. Code Reviews
- Review all PRs for accidentally committed credentials
- Use tools like `git-secrets` or `trufflehog` to scan for secrets
- Set up pre-commit hooks to prevent credential commits

## 🚨 If Credentials Are Exposed

1. **Immediate Actions:**
   - Revoke/rotate the exposed credentials immediately
   - Remove from git history: `git filter-branch` or `BFG Repo-Cleaner`
   - Force push after cleanup (notify team first!)
   - Update all environments with new credentials

2. **GitHub Actions:**
   - Delete exposed secret from GitHub Secrets
   - Update with new value
   - Re-run failed workflows

3. **Notification:**
   - Notify security team
   - Document the incident
   - Review what went wrong
   - Update processes to prevent recurrence

## 🔍 Audit Checklist

Before committing code:
- [ ] No hardcoded passwords or API keys
- [ ] All credentials use environment variables
- [ ] `.env` file is not committed
- [ ] `.env.example` has no real credentials (only placeholders)
- [ ] `application.yml` has no default credential values
- [ ] Test files use mock/test credentials only
- [ ] No sensitive data in logs or error messages

## 📚 Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [12-Factor App - Config](https://12factor.net/config)
- [GitHub Secret Scanning](https://docs.github.com/en/code-security/secret-scanning)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
