# Kepsen

[English](README.md)

Kepsen 是一个多模块 Java 库，用来为 gRPC 服务端增加 **mTLS 双向认证** 和 **方法级 ACL 授权**。它把核心安全逻辑沉淀在 `kepsen-core`，再分别提供 **Spring Boot** 和 **Micronaut** 的薄适配层。

## 能力概览

当一个 gRPC 请求到达服务端时，Kepsen 会按顺序执行：

1. 要求客户端通过 mTLS 提供有效证书。
2. 从客户端证书中提取身份。
3. 将本次调用的方法与 ACL 规则做匹配。
4. 放行请求，或以 `PERMISSION_DENIED` 拒绝调用。

它特别适合内部服务之间的 gRPC 通信，尤其是客户端身份以 SPIFFE 风格 SAN URI 表达的场景。

## 模块说明

```text
kepsen-core                  与框架无关的 ACL 引擎和配置模型
kepsen-grpc                  gRPC 拦截器与 Netty mTLS 配置器
kepsen-spring-boot-starter   Spring Boot 自动装配
kepsen-micronaut             Micronaut 配置绑定与 Bean 装配
```

### `kepsen-core`

纯 Java 模块，不依赖具体框架。

| 类型 | 作用 |
|---|---|
| `AclConfig` | 全局 ACL 配置 |
| `AclRule` | 具名规则，定义方法模式和允许访问的客户端身份 |
| `MethodAclAuthorizer` | ACL 规则评估器 |
| `ClientIdentityExtractor` | 从 `X509Certificate` 中提取 SAN URI 或 CN |
| `MtlsConfig` | mTLS 文件路径配置模型 |

### `kepsen-grpc`

基于 `kepsen-core` 的 gRPC 运行时实现。

| 类型 | 作用 |
|---|---|
| `MtlsAclInterceptor` | 校验 mTLS 和 ACL 的 gRPC `ServerInterceptor` |
| `NettyMtlsServerConfigurer` | 把服务端证书和信任根配置到 `NettyServerBuilder` |

### `kepsen-spring-boot-starter`

Spring Boot 适配层，自动注册：

- 全局 gRPC 服务端拦截器 `MtlsAclInterceptor`
- Netty mTLS 配置器 `GrpcServerConfigurer`
- `service-acl` 和 `mtls.server` 的 `@ConfigurationProperties`

### `kepsen-micronaut`

Micronaut 适配层，负责装配：

- `@ConfigurationProperties("mtls.server")`
- `@ConfigurationProperties("service-acl")`
- `@EachProperty("service-acl.rules")`
- 用于配置 Netty mTLS 的 `BeanCreatedEventListener<ServerBuilder<?>>`

## 依赖示例

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

## 配置方式

Kepsen 在 Spring Boot 和 Micronaut 中使用同一套配置键：

- `mtls.server.*`
- `service-acl.*`
- `service-acl.rules.*`

也就是说，绝大多数情况下，同一份 `application.yml` 或 `application.properties` 可以在两个框架之间直接复用。

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

Micronaut 也使用同样的结构：

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

## 证书由谁提供

Kepsen **不会签发、下发或轮换证书**。

接入 Kepsen 的应用需要自行提供：

- 服务端证书链
- 服务端私钥
- 用于校验客户端证书的受信任 CA 证书集合

Kepsen 在运行时只负责读取这些路径，并将其应用到 gRPC Netty 服务端。

### 仓库中的开发证书

`certs/dev/` 下的文件仅用于本地开发和演示，由下面这个脚本生成：

- [certs/dev/generate.sh](certs/dev/generate.sh)

它会生成：

- `ca.crt` / `ca.key`
- `server.crt` / `server.key`
- `service-a.crt` / `service-a.key`
- `service-b.crt` / `service-b.key`

这些证书适合本地联调，不适合生产环境。生产环境应使用你们自己的 PKI、CA 或证书平台来签发和管理。

## ACL 规则语义

### 方法匹配模式

| 模式 | 含义 |
|---|---|
| `pkg.Service/Method` | 精确匹配单个方法 |
| `pkg.Service/*` | 匹配某个服务下的所有方法 |
| `*` | 匹配所有服务的所有方法 |

### 身份来源

| 值 | 含义 |
|---|---|
| `san-uri` | 使用客户端证书中的第一个 URI SAN |
| `cn` | 使用 Subject DN 中的 Common Name |
| `san-uri-then-cn` | 优先使用 SAN URI，不存在时回退到 CN |

### 默认动作

- `deny`：没有规则匹配时拒绝
- `allow`：没有规则匹配时允许

## Native Image

Kepsen 已经为以下模块补充了 native-image 元数据：

- `kepsen-core`
- `kepsen-grpc`

整体设计目标是尽量减少反射需求，并把框架相关逻辑限制在很薄的适配层中，以便更好地支持 AOT 和 GraalVM Native Image。

## 构建与验证

```bash
./gradlew :kepsen-core:test
./gradlew publishToMavenLocal
./gradlew :kepsen-core:dependencies --configuration runtimeClasspath
```

最后一个命令应显示 `kepsen-core` 没有运行时框架依赖。

## 许可证

Apache License 2.0，见 [LICENSE](LICENSE)。
