# Secrets and local configuration

The `application.properties` files contain configuration references only.
Supply secret values through environment variables; never commit `.env` files,
credentials, or production configuration overrides.

## Required secret variables

| Variable | Services | Notes |
| --- | --- | --- |
| `AUTH_JWT_SECRET` | All services | Shared Base64 key with at least 32 decoded bytes |
| `AUTH_DB_PASSWORD` | Auth | Auth database password |
| `CART_DB_PASSWORD` | Cart | Cart database password |
| `ORDER_DB_PASSWORD` | Order | Order database password |
| `PAYMENT_DB_PASSWORD` | Payment | Payment database password |
| `PRODUCT_DB_PASSWORD` | Product | Product database password |
| `INTERNAL_PAYMENT_TOKEN` | Order and Payment | Shared private token for internal service requests |
| `STRIPE_SECRET_KEY` | Payment | Stripe API secret |
| `STRIPE_WEBHOOK_SECRET` | Payment | Stripe webhook signing secret |
| `AWS_S3_BUCKET` | Product | S3 bucket name |

Database usernames default to `root` for local development and can be overridden
with `AUTH_DB_USERNAME`, `CART_DB_USERNAME`, `ORDER_DB_USERNAME`,
`PAYMENT_DB_USERNAME`, and `PRODUCT_DB_USERNAME`. Database URLs can be
overridden with the corresponding `*_DB_URL` variables. Other optional
settings are listed in `product/AUTHENTICATION.md`.

## Local development

Set secrets in the process environment or in your IDE's private run
configuration. Avoid entering actual secret values in shell history, scripts,
or files inside the repository.

The Product service's AWS SDK uses the standard credential provider chain:
environment credentials, an AWS CLI profile, or a workload role. Prefer an
AWS CLI profile for local use and an IAM instance/task role in deployed
environments. Do not add AWS access keys to `application.properties`.

## Production and CI

Store credentials and signing keys in a managed secret store (for example AWS
Secrets Manager), grant each workload access only to its own required secrets,
and inject the values as environment variables at runtime. Keep non-secret
configuration separate, restrict secret access, and rotate credentials
regularly. In GitHub Actions, use repository or environment secrets and pass
them to the service process as environment variables; never print them in
workflow logs.

## If a secret was committed or pushed

Removing a value from the current files does not remove it from Git history.
Immediately revoke and replace exposed AWS keys, database passwords, JWT
signing keys, and payment credentials as applicable. Then coordinate removal
from Git history and rotate dependent sessions/tokens; rewriting shared history
requires coordination with collaborators.
