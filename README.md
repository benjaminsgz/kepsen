# Kepsen

[![Java](https://img.shields.io/badge/Java-21-ff6f00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8+-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![gRPC](https://img.shields.io/badge/gRPC-Java-244c5a?logo=grpc&logoColor=white)](https://grpc.io/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Starter-6db33f?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Micronaut](https://img.shields.io/badge/Micronaut-Adapter-1b1f23?logo=micronaut&logoColor=white)](https://micronaut.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

[中文文档](README.zh.md)

Kepsen is a multi-module Java library for adding **mTLS mutual authentication** and **method-level ACL authorization** to gRPC servers. It keeps the security logic framework-agnostic in `kepsen-core`, then provides thin integration layers for **Spring Boot** and **Micronaut**.

## What It Does

When a gRPC request reaches the server, Kepsen:

1. Requires a valid client certificate over mTLS.
2. Extracts the client identity from the certificate.
3. Matches the invoked method against configured ACL rules.
4. Allows the call through or rejects it with `PERMISSION_DENIED`.

This makes it a good fit for internal service-to-service gRPC traffic, especially when identities are carried in SPIFFE-style SAN URIs.

## Modules

```text
kepsen-core                  framework-agnostic ACL engine and config model
kepsen-grpc                  gRPC interceptor and Netty mTLS configurer
kepsen-spring-boot-starter   Spring Boot auto-configuration
kepsen-micronaut             Micronaut configuration and bean wiring
```

### `kepsen-core`

Pure Java, no framework dependency.

| Type | Purpose |
|---|---|
| `AclConfig` | Global ACL settings |
| `AclRule` | A named rule with method pattern and allowed clients |
| `MethodAclAuthorizer` | ACL evaluation engine |
| `ClientIdentityExtractor` | Extracts SAN URI or CN from `X509Certificate` |
| `MtlsConfig` | mTLS file-path configuration model |

### `kepsen-grpc`

gRPC-specific runtime pieces built on top of `kepsen-core`.

| Type | Purpose |
|---|---|
| `MtlsAclInterceptor` | Enforces mTLS presence and ACL checks |
| `NettyMtlsServerConfigurer` | Applies server keypair and trust material to `NettyServerBuilder` |

### `kepsen-spring-boot-starter`

Spring Boot integration that auto-registers:

- `MtlsAclInterceptor` as a global gRPC server interceptor
- `GrpcServerConfigurer` for Netty mTLS setup
- `@ConfigurationProperties` bindings for `service-acl` and `mtls.server`

### `kepsen-micronaut`

Micronaut integration that wires:

- `@ConfigurationProperties("mtls.server")`
- `@ConfigurationProperties("service-acl")`
- `@EachProperty("service-acl.rules")`
- a `BeanCreatedEventListener<ServerBuilder<?>>` for Netty mTLS

## Dependencies

### Spring Boot

```groovy
dependencies {
    implementation("uk.sienne:kepsen-spring-boot-starter:<version>")
    implementation("net.devh:grpc-server-spring-boot-starter:<version>")
}
```

### Micronaut

```groovy
dependencies {
    implementation("uk.sienne:kepsen-micronaut:<version>")
    implementation("io.micronaut.grpc:micronaut-grpc-runtime:<version>")
}
```

## Configuration

Kepsen uses the same config keys in both Spring Boot and Micronaut:

- `mtls.server.*`
- `service-acl.*`
- `service-acl.rules.*`

That means the same `application.yml` or `application.properties` layout can usually be reused across both frameworks.

### Spring Boot `application.yml`

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
    billing-charge:
      method: "billing.BillingService/Charge"
      allowed-clients:
        - "spiffe://example.com/ns/default/sa/payment-svc"
    reporting-all:
      method: "reporting.ReportingService/*"
      allowed-clients:
        - "spiffe://example.com/ns/default/sa/dashboard"
```

### Spring Boot `application.properties`

```properties
mtls.server.enabled=true
mtls.server.cert-chain=certs/server.crt
mtls.server.private-key=certs/server.key
mtls.server.trust-cert-collection=certs/ca.crt

service-acl.enabled=true
service-acl.default-action=deny
service-acl.identity-source=san-uri
service-acl.rules.billing-charge.method=billing.BillingService/Charge
service-acl.rules.billing-charge.allowed-clients=spiffe://example.com/ns/default/sa/payment-svc
service-acl.rules.reporting-all.method=reporting.ReportingService/*
service-acl.rules.reporting-all.allowed-clients=spiffe://example.com/ns/default/sa/dashboard
```

### Micronaut `application.yml`

Micronaut uses the same shape:

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
    billing-charge:
      method: "billing.BillingService/Charge"
      allowed-clients:
        - "spiffe://example.com/ns/default/sa/payment-svc"
    reporting-all:
      method: "reporting.ReportingService/*"
      allowed-clients:
        - "spiffe://example.com/ns/default/sa/dashboard"
```

### Micronaut `application.properties`

```properties
mtls.server.enabled=true
mtls.server.cert-chain=certs/server.crt
mtls.server.private-key=certs/server.key
mtls.server.trust-cert-collection=certs/ca.crt

service-acl.enabled=true
service-acl.default-action=deny
service-acl.identity-source=san-uri
service-acl.rules.billing-charge.method=billing.BillingService/Charge
service-acl.rules.billing-charge.allowed-clients=spiffe://example.com/ns/default/sa/payment-svc
service-acl.rules.reporting-all.method=reporting.ReportingService/*
service-acl.rules.reporting-all.allowed-clients=spiffe://example.com/ns/default/sa/dashboard
```

## Certificate Responsibility

Kepsen **does not issue or rotate certificates**.

The application using Kepsen is responsible for providing:

- the server certificate chain
- the server private key
- the trusted CA bundle used to verify client certificates

At runtime, Kepsen only reads those file locations and applies them to the gRPC Netty server.

### Development Certificates In This Repository

The files under `certs/dev/` are only development fixtures. They are generated by:

- [certs/dev/generate.sh](certs/dev/generate.sh)

That script creates:

- `ca.crt` / `ca.key`
- `server.crt` / `server.key`
- `service-a.crt` / `service-a.key`
- `service-b.crt` / `service-b.key`

These are useful for local testing and examples, but production certificates should come from your own PKI, CA, or certificate platform.

## ACL Rule Semantics

### Method Patterns

| Pattern | Meaning |
|---|---|
| `pkg.Service/Method` | Exact method match |
| `pkg.Service/*` | All methods on one service |
| `*` | All methods on all services |

### Identity Sources

| Value | Meaning |
|---|---|
| `san-uri` | First URI SAN from the client certificate |
| `cn` | Common Name from the subject DN |
| `san-uri-then-cn` | Prefer SAN URI, fall back to CN |

### Default Action

- `deny`: reject calls when no rule matches
- `allow`: allow calls when no rule matches

## Native Image Notes

Kepsen includes native-image metadata for:

- `kepsen-core`
- `kepsen-grpc`

The intent is to stay friendly to AOT and GraalVM Native Image use cases by keeping reflection minimal and pushing framework logic into thin adapter layers.

## Kubernetes Example

Spring Cloud Kubernetes deployment manifests are available under:

- [examples/spring-cloud-k8s](examples/spring-cloud-k8s)

The example mounts TLS material from a Kubernetes `Secret` and loads Spring/Kepsen configuration from a `ConfigMap`. Certificates and ACL changes require a pod rollout because Kepsen builds the Netty TLS context and ACL index at startup.

## Choosing Kepsen vs Alternatives

Kepsen is for teams that want application-level gRPC mTLS and method ACLs without adopting a full service mesh.

| Option | Use when | Cost |
|---|---|---|
| Kepsen | You run Java gRPC services and want a small in-process mTLS + method allowlist. | Each service must configure certificates and ACL rules. Certificate rotation needs rollout today. |
| Istio / Linkerd / Consul Connect | You want mesh-wide identity, mTLS, policy, telemetry, retries, and traffic controls. | Adds proxy/control-plane CPU, memory, latency, and operational complexity. |
| SPIFFE / SPIRE | You need workload identity and automatic SVID/trust bundle rotation. | Solves identity distribution, not method-level ACL by itself. Kepsen can consume SPIFFE-style SAN URIs. |
| Spring Security / Spring gRPC security | You already use Spring Security and want roles, annotations, OAuth2/JWT, or expression-based authorization. | More framework coupling and more request-path abstraction than a direct method allowlist. |

Do not use Kepsen behind a proxy that terminates client TLS before the request reaches the application. In that topology the gRPC `SSLSession` no longer carries the original client certificate.

## Build

```bash
./gradlew :kepsen-core:test
./gradlew publishToMavenLocal
./gradlew :kepsen-core:dependencies --configuration runtimeClasspath
```

The last command should show that `kepsen-core` has no runtime framework dependencies.

## License

Apache License 2.0. See [LICENSE](LICENSE).
