# Kepsen

[English](README.md)

一个多模块 Java 库，为 gRPC 服务端提供 **mTLS 双向认证** 和 **方法级 ACL 授权**，原生支持 Spring Boot 和 Micronaut。

## 概述

```
kepsen-core                  — 框架无关的 ACL 引擎与 mTLS 配置模型
kepsen-grpc                  — gRPC ServerInterceptor 与 Netty TLS 配置器
kepsen-spring-boot-starter   — Spring Boot 自动配置
kepsen-micronaut             — Micronaut @Factory 与 @ConfigurationProperties 集成
```

当 gRPC 调用到达时，拦截器依次执行：

1. 要求客户端提供有效的 mTLS 证书（否则返回 `UNAUTHENTICATED`）。
2. 从证书中提取客户端身份（SAN URI、CN 或 SAN-URI-then-CN 回退）。
3. 对被调用方法评估 ACL 规则，放行或以 `PERMISSION_DENIED` 关闭调用。

## 环境要求

- Java 21+
- Gradle 8+

## 模块说明

### `kepsen-core`

框架无关的核心模块，包含：

| 类 | 职责 |
|---|---|
| `AclConfig` | 全局 ACL 设置（是否启用、默认动作、身份来源） |
| `AclRule` | 将方法模式与允许的客户端身份集合绑定的命名规则 |
| `MethodAclAuthorizer` | 规则评估器，支持精确方法、服务通配符（`/MyService/*`）和全局通配符（`*`） |
| `ClientIdentityExtractor` | 从 `X509Certificate` 中读取 SAN URI 或 CN |
| `MtlsConfig` | 证书链、私钥、信任证书集合的路径配置 |

### `kepsen-grpc`

依赖 `kepsen-core` 和 `grpc-api`。

| 类 | 职责 |
|---|---|
| `MtlsAclInterceptor` | 强制执行 mTLS 校验和 ACL 规则的 `ServerInterceptor` |
| `NettyMtlsServerConfigurer` | 根据 `MtlsConfig` 配置 Netty SSL 上下文 |

### `kepsen-spring-boot-starter`

将 `MtlsAclInterceptor` 和 `GrpcServerConfigurer` 自动注册为 Spring Bean，并通过 `@GrpcGlobalServerInterceptor` 全局生效。

### `kepsen-micronaut`

通过 Micronaut 的 `@Factory` 和 `@ConfigurationProperties` 完成相同组件的装配。

## 配置

### Spring Boot（`application.yml`）

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

### Micronaut（`application.yml`）

配置键相同，ACL 规则以列表形式声明在 `service-acl.rules` 下：

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

## 方法匹配模式

| 模式 | 匹配范围 |
|---|---|
| `pkg.ServiceName/MethodName` | 精确匹配单个方法 |
| `pkg.ServiceName/*` | 匹配某服务的所有方法 |
| `*` | 匹配所有服务的所有方法 |

规则按声明顺序评估，第一条匹配的规则生效。若无规则匹配，则使用 `default-action`。

## 身份来源

| 值 | 说明 |
|---|---|
| `san-uri` | 客户端证书中的第一个 URI SAN（例如 SPIFFE ID） |
| `cn` | Subject DN 中的 Common Name |
| `san-uri-then-cn` | 优先使用 SAN URI，不存在时回退到 CN |

## 构建

```bash
./gradlew build
```

仅运行测试：

```bash
./gradlew test
```

## 许可证

Apache 2.0
