# Kepsen

A multi-module Java library that adds **mTLS mutual authentication** and **method-level ACL authorization** to gRPC servers, with first-class integrations for Spring Boot and Micronaut.

## Overview

```
kepsen-core                  — framework-agnostic ACL engine and mTLS config model
kepsen-grpc                  — gRPC ServerInterceptor and Netty TLS configurer
kepsen-spring-boot-starter   — Spring Boot auto-configuration
kepsen-micronaut             — Micronaut @Factory and @ConfigurationProperties wiring
```

When a gRPC call arrives the interceptor:

1. Requires a valid mTLS client certificate (rejects with `UNAUTHENTICATED` otherwise).
2. Extracts the client identity from the certificate (SAN URI, CN, or SAN-URI-then-CN fallback).
3. Evaluates the ACL rules for the called method and either forwards the call or closes it with `PERMISSION_DENIED`.

## Requirements

- Java 21+
- Gradle 8+

## Modules

### `kepsen-core`

Framework-agnostic. Contains:

| Class | Purpose |
|---|---|
| `AclConfig` | Global ACL settings (enabled, default-action, identity-source) |
| `AclRule` | A named rule binding a method pattern to a set of allowed client identities |
| `MethodAclAuthorizer` | Evaluates rules; supports exact method, service wildcard (`/MyService/*`), and global wildcard (`*`) |
| `ClientIdentityExtractor` | Reads SAN URI or CN from an `X509Certificate` |
| `MtlsConfig` | Paths to cert-chain, private-key, and trust-cert-collection |

### `kepsen-grpc`

Depends on `kepsen-core` and `grpc-api`.

| Class | Purpose |
|---|---|
| `MtlsAclInterceptor` | `ServerInterceptor` that enforces mTLS presence and ACL rules |
| `NettyMtlsServerConfigurer` | Configures Netty's SSL context from a `MtlsConfig` |

### `kepsen-spring-boot-starter`

Auto-configures `MtlsAclInterceptor` and `GrpcServerConfigurer` as Spring beans. Registers the interceptor globally via `@GrpcGlobalServerInterceptor`.

### `kepsen-micronaut`

Wires the same components via Micronaut's `@Factory` and `@ConfigurationProperties`.

## Configuration

### Spring Boot (`application.yml`)

```yaml
mtls:
  server:
    enabled: true
    cert-chain: classpath:certs/server.crt
    private-key: classpath:certs/server.key
    trust-cert-collection: classpath:certs/ca.crt

service-acl:
  enabled: true
  default-action: deny          # deny | allow
  identity-source: san-uri      # san-uri | cn | san-uri-then-cn
  rules:
    allow-billing:
      method: "billing.BillingService/Charge"
      allowed-clients:
        - "spiffe://example.com/ns/default/sa/payment-svc"
    allow-all-reporting:
      method: "reporting.ReportingService/*"
      allowed-clients:
        - "spiffe://example.com/ns/default/sa/dashboard"
```

### Micronaut (`application.yml`)

Same keys. ACL rules are declared as a list under `service-acl.rules`:

```yaml
mtls:
  server:
    enabled: true
    cert-chain: certs/server.crt
    private-key: certs/server.key
    trust-cert-collection: certs/ca.crt

service-acl:
  enabled: true
  default-action: deny
  identity-source: san-uri
  rules:
    - name: allow-billing
      method: "billing.BillingService/Charge"
      allowed-clients:
        - "spiffe://example.com/ns/default/sa/payment-svc"
```

## Method Pattern Syntax

| Pattern | Matches |
|---|---|
| `pkg.ServiceName/MethodName` | Exact method |
| `pkg.ServiceName/*` | All methods on a service |
| `*` | Every method on every service |

Rules are evaluated in declaration order. The first matching rule wins. If no rule matches, `default-action` applies.

## Identity Sources

| Value | Description |
|---|---|
| `san-uri` | First URI SAN from the client certificate (e.g. SPIFFE ID) |
| `cn` | Common Name from the Subject DN |
| `san-uri-then-cn` | SAN URI if present, CN as fallback |

## Building

```bash
./gradlew build
```

Run tests only:

```bash
./gradlew test
```

## License

Apache 2.0
