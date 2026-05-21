# Kepsen 开发者与架构师 Wiki 目录 (Wiki Catalogue)

本文档是面向后端开发人员与系统架构师的官方 Wiki 目录与技术沉浸指南。旨在帮助高级架构师与新进开发者快速理解系统核心设计逻辑、掌握代码导航、进行本地研发和生产级架构评估。

---

## 1. 专家级架构沉浸指南 (Principal-Level Guide)

本章节面向资深技术专家与系统架构师，深入剖析 Kepsen 进程内拦截设计的底层核心逻辑。

### 1.1 核心设计哲学与抽象逻辑
Kepsen 的核心原理可以用如下的 Python 伪代码来进行逻辑抽象。在 gRPC 框架的请求生命周期中，我们在拦截器级别捕获网络上下文属性，提取并校验证书的加密学身份，再通过高速的 $O(1)$ 哈希索引进行访问控制决策。

```python
# 核心安全校验逻辑的 Python 抽象模型 (对比 Java 强类型实现)
class GrpcMtlsAclEngine:
    def __init__(self, acl_config, acl_rules):
        self.enabled = acl_config.get("enabled", True)
        self.default_action = acl_config.get("default_action", "deny")
        self.identity_source = acl_config.get("identity_source", "san-uri")
        
        # 预编译高速索引以保证 O(1) 检索
        self.exact_rules = {}   # "package.Service/Method" -> Set(allowed_clients)
        self.service_rules = {} # "package.Service" -> Set(allowed_clients)
        self.global_rules = set() # Set(allowed_clients) for "*"
        
        self._compile_rules(acl_rules)

    def _compile_rules(self, rules):
        for rule in rules:
            method = rule["method"]
            allowed = set(rule["allowed_clients"])
            if method == "*":
                self.global_rules.update(allowed)
            elif method.endswith("/*"):
                service_name = method[:-2]
                self.service_rules.setdefault(service_name, set()).update(allowed)
            else:
                self.exact_rules.setdefault(method, set()).update(allowed)

    def authorize(self, client_cert, full_method_name):
        if not self.enabled:
            return True # ACL 禁用时默认全部放行
            
        # 提取身份
        identity = self._extract_identity(client_cert)
        
        # 匹配优先级：精确匹配 -> 服务级通配符 -> 全局通配符 -> 默认动作
        if full_method_name in self.exact_rules:
            return identity in self.exact_rules[full_method_name]
            
        service_name = full_method_name.split("/")[0]
        if service_name in self.service_rules:
            return identity in self.service_rules[service_name]
            
        if self.global_rules:
            return identity in self.global_rules
            
        return self.default_action == "allow"

    def _extract_identity(self, cert):
        # 对应 ClientIdentityExtractor 的核心抽象
        if self.identity_source == "san-uri":
            return cert.get_subject_alternative_names_uri()[0]
        elif self.identity_source == "cn":
            return cert.get_subject_dn_common_name()
        return None
```

### 1.2 系统架构与部署模型
Kepsen 直接集成到应用进程中，其外部交互关系与网络拓扑如下所示：

```mermaid
graph TD
    ClientSvc[客户端服务]
    APIGateway[API 网关 / 边缘负载均衡]
    K8sSecret[Kubernetes Secret / 密文卷]
    ConfigMap[Kubernetes ConfigMap / 配置卷]
    
    subgraph CorePod [后端微服务 Pod]
        subgraph JavaProcess [JVM 应用进程]
            Netty[gRPC Netty 传输层]
            Interceptor[MtlsAclInterceptor]
            AclAuthorizer[MethodAclAuthorizer]
            BizImplementation[业务 gRPC 服务实现]
        end
    end

    ClientSvc -->|mTLS 请求 + SPIFFE SAN 证书| Netty
    APIGateway -->|边缘流量 (通常不带 mTLS 证书)| Netty
    K8sSecret -.->|磁盘挂载证书文件: certs/ca.crt, server.crt, server.key| Netty
    ConfigMap -.->|挂载 application.yml 规则配置| AclAuthorizer
    
    Netty --> Interceptor
    Interceptor --> AclAuthorizer
    AclAuthorizer -->|放行| BizImplementation
```

### 1.3 核心技术决策与权衡 (Strategic Tradeoffs)
1. **高性能零内存分配设计**：
   通过在初始化时将 ACL 规则转化为不可变的 `LinkedHashMap` 和 `Set.copyOf`。在处理高频流量时，避免了对规则列表的线性轮询（O(N) 遍历），保证了每次 gRPC 方法调用在授权环节只需消耗几纳秒。
2. **轻量化独立性**：
   `kepsen-core` 不带有任何外部依赖，包括 Netty 和 gRPC 本身。这极大地保证了底层的稳定性，避免了多组件依赖冲突，并对 GraalVM Native Image 极为友好。
3. **不可变配置与重启模型**：
   在 Kepsen 中，Netty SSLContext 和 ACL 索引在启动时就被冻结。如果证书需要轮换或 ACL 策略变更，推荐采用主流的 Kubernetes 滚动部署（Rolling Update / Rollout），以防内存泄露并确保校验状态的一致性。

---

## 2. 新手成长路径 (Zero-to-Hero Learning Path)

本章节为新加入项目的研发工程师设计，通过渐进的认知序列，快速上手 Kepsen 系统的研发。

### 阶段一：基础技术前置知识 (Java gRPC mTLS & Netty 基础)
在深挖 Kepsen 代码前，请确保您掌握了以下前置知识：
* **mTLS (双向 TLS 认证)**：与传统的单向 TLS（仅客户端验证服务端身份）不同，mTLS 要求服务端也验证客户端提供的 X509 证书。
* **Subject Alternative Name (SAN)**：X509 证书扩展字段，用以声明该证书主体的可替代名称（如域名、IP 地址或 URI）。SPIFFE（Secure Production Identity Framework for Everyone）规范广泛使用 SAN URI 来表达微服务的工作负载身份（Workload Identity）。
* **gRPC 拦截器 (ServerInterceptor)**：gRPC-Java 提供的切面机制，可以在执行 RPC 方法前后读取、修改请求上下文元数据（Metadata）以及网络传输属性（Transport Attributes）。

### 阶段二：代码导航与目录结构
在克隆项目后，您可以通过以下结构快速定位核心逻辑：
* **核心校验器**：
  * [ClientIdentityExtractor.java](file:///f:/kepsen/kepsen-core/src/main/java/uk/sienne/grpcauth/core/ClientIdentityExtractor.java) - 实现证书 SAN URI 和 CN 身份解析。
  * [MethodAclAuthorizer.java](file:///f:/kepsen/kepsen-core/src/main/java/uk/sienne/grpcauth/core/MethodAclAuthorizer.java) - 实现方法通配符的高速内存检索引擎。
* **gRPC 插件适配层**：
  * [MtlsAclInterceptor.java](file:///f:/kepsen/kepsen-grpc/src/main/java/uk/sienne/grpcauth/grpc/MtlsAclInterceptor.java) - 实现 gRPC `ServerInterceptor`，控制握手后请求生命周期拦截。
  * [NettyMtlsServerConfigurer.java](file:///f:/kepsen/kepsen-grpc/src/main/java/uk/sienne/grpcauth/grpc/NettyMtlsServerConfigurer.java) - 将 PEM 证书文件转换为 Netty 的底层 `SslContext`。
* **微服务框架注入**：
  * [GrpcMtlsAutoConfiguration.java](file:///f:/kepsen/kepsen-spring-boot-starter/src/main/java/uk/sienne/grpcauth/spring/GrpcMtlsAutoConfiguration.java) - Spring Boot 自动装配入口。
  * [MicronautMtlsServerCustomizer.java](file:///f:/kepsen/kepsen-micronaut/src/main/java/uk/sienne/grpcauth/micronaut/MicronautMtlsServerCustomizer.java) - Micronaut 自动装配入口。

### 阶段三：开发、测试与持续集成
1. **本地环境初始化**：
   在开始修改前，需要运行脚本生成本地研发和自测专用的 TLS 证书：
   ```bash
   cd certs/dev
   ./generate.sh
   ```
2. **本地测试套件运行**：
   Kepsen 的每个核心模块都具备高覆盖率的单元测试：
   * [ClientIdentityExtractorTest.java](file:///f:/kepsen/kepsen-core/src/test/java/uk/sienne/grpcauth/core/ClientIdentityExtractorTest.java) - 验证各种异常和边界证书下的身份抽取准确度。
   * [MethodAclAuthorizerTest.java](file:///f:/kepsen/kepsen-core/src/test/java/uk/sienne/grpcauth/core/MethodAclAuthorizerTest.java) - 测试精确匹配、服务级通配符和全局通配符下的鉴权路由。
   * [MtlsAclInterceptorTest.java](file:///f:/kepsen/kepsen-grpc/src/test/java/uk/sienne/grpcauth/grpc/MtlsAclInterceptorTest.java) - 使用模拟的 `SSLSession` 校验完整的 gRPC 拦截响应。

   运行所有测试命令：
   ```bash
   ./gradlew test
   ```

---

## 3. 系统深度剖析 (Subsystem & Component Deep Dive)

本章节对 Kepsen 的三大子系统进行源码级的架构级拆解。

### 3.1 基础计算模型：`kepsen-core`
该模块包含双向证书通道的底座逻辑，专注于数据结构与核心计算。

#### `ClientIdentityExtractor` (身份提取器)
* **核心方法**：`extract(X509Certificate cert, String mode)`
* **源文件**：[ClientIdentityExtractor.java#L17-L35](file:///f:/kepsen/kepsen-core/src/main/java/uk/sienne/grpcauth/core/ClientIdentityExtractor.java#L17-L35)
* **逻辑设计**：
  利用 JDK 安全包内的 `cert.getSubjectAlternativeNames()` 读取扩展属性。
  * 针对 SAN URI（ASN.1 Tag 值定义为 6），通过遍历 `sans` 提取。
  * 针对 CN，使用 `LdapName` 解析 X500Principal 结构，提取第一个 `CN` 的 Rdn 值，防范恶意目录注入或格式错误。

#### `MethodAclAuthorizer` (高速授权决策引擎)
* **核心方法**：`isAllowed(String clientIdentity, String fullMethodName)`
* **源文件**：[MethodAclAuthorizer.java#L40-L78](file:///f:/kepsen/kepsen-core/src/main/java/uk/sienne/grpcauth/core/MethodAclAuthorizer.java#L40-L78)
* **设计细节**：
  * 精确匹配：直接用 `fullMethodName` 作为 Key，命中哈希表。
  * 服务级通配符匹配：提取 gRPC 方法前面的服务名，通过判断 `method.endsWith("/*")` 在编译阶段进行映射，检索 `serviceClients`。
  * 若全不匹配，退回到 `AclConfig` 内配置的 `default-action` 策略。

### 3.2 拦截与配置网络层：`kepsen-grpc`
连接 gRPC 传输框架与计算核心的桥梁。

#### `MtlsAclInterceptor` (gRPC 服务端拦截器)
* **核心方法**：`interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next)`
* **源文件**：[MtlsAclInterceptor.java#L53-L112](file:///f:/kepsen/kepsen-grpc/src/main/java/uk/sienne/grpcauth/grpc/MtlsAclInterceptor.java#L53-L112)
* **拦截机制**：
  1. 通过 gRPC 属性通道获取网络上下文中的 SSL 会话：`call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION)`。
  2. 若会话不存在且 `mtlsRequired` 为 true，以 `Status.UNAUTHENTICATED` 状态强行终止请求并响应描述 `mTLS is required`。
  3. 获取证书链的第一张证书：`session.getPeerCertificates()[0]`。
  4. 利用 `ClientIdentityExtractor` 进行主体身份解算，并利用 `MethodAclAuthorizer` 进行鉴权决策。
  5. 鉴权失败时，返回 `Status.PERMISSION_DENIED` 并拦截，且通过 `SHA-256` 算法哈希处理客户端的敏感身份名称后写入防审计追踪的安全日志中（[MtlsAclInterceptor.java#L125-L133](file:///f:/kepsen/kepsen-grpc/src/main/java/uk/sienne/grpcauth/grpc/MtlsAclInterceptor.java#L125-L133)）。

#### `NettyMtlsServerConfigurer` (Netty mTLS 安全自定义器)
* **核心方法**：`configure(NettyServerBuilder builder)`
* **源文件**：[NettyMtlsServerConfigurer.java#L25-L46](file:///f:/kepsen/kepsen-grpc/src/main/java/uk/sienne/grpcauth/grpc/NettyMtlsServerConfigurer.java#L25-L46)
* **底层实现**：
  * 使用 `GrpcSslContexts.configure()` 包装 Netty 的 `SslContextBuilder`。
  * 将 `certChain`、`privateKey`、`trustCa` 转化为 File 对象并注册到 Netty。
  * 设置 `.clientAuth(ClientAuth.REQUIRE)`，使得客户端在 TCP/TLS 连接层就必须发送合法合规的客户端证书。

### 3.3 自动装配层：`spring-boot-starter` & `micronaut`
为外部应用提供即插即用的自动装载与属性转换。

* **Spring Boot 的集成实现**：
  * [GrpcMtlsAutoConfiguration.java](file:///f:/kepsen/kepsen-spring-boot-starter/src/main/java/uk/sienne/grpcauth/spring/GrpcMtlsAutoConfiguration.java) 会将拦截器标记为 `@GrpcGlobalServerInterceptor`。这使得底层的 gRPC Spring Starter 会将该拦截器全局作用到所有注册的 gRPC 服务上。
* **Micronaut 的集成实现**：
  * [MicronautMtlsServerCustomizer.java](file:///f:/kepsen/kepsen-micronaut/src/main/java/uk/sienne/grpcauth/micronaut/MicronautMtlsServerCustomizer.java) 监听 `ServerBuilder` 的创建事件，在运行时动态转换为 `NettyServerBuilder` 并配置。

---

## 附录：核心技术词汇表 (Glossary of Terms)

1. **mTLS (Mutual TLS / 双向安全握手认证)**：网络连接两端的实体均通过数字证书验证对方合法性的 TLS 强化通道。
2. **SAN (Subject Alternative Name / 主题备用名称)**：X.509 规范扩展字段，用于标识证书支持的备用名（如 SPIFFE 工作负载身份标识）。
3. **SPIFFE (Secure Production Identity Framework for Everyone)**：一种云原生环境下的零信任工作负载安全身份框架规范。
4. **Common Name (CN)**：X.509 数字证书主题 DN 中的一属性，常用于表示服务的域名或主体助记名。
5. **DN (Distinguished Name / 可判别名称)**：LDAP 与加密证书体系中用于唯一标识一个条目的层级命名表示法。
6. **Rdn (Relative Distinguished Name / 相对可判别名称)**：构成完整 DN 的单个键值对元素（例如 `CN=example`）。
7. **ServerInterceptor (gRPC 服务端拦截器)**：gRPC-Java 切面接口，用于在方法调用前后运行横切性校验逻辑（如身份认证、授权、监控等）。
8. **NettyServerBuilder**：Netty 传输驱动下用于创建和装配 gRPC Server 实例的底层配置构建器。
9. **ClientAuth.REQUIRE**：Netty SSL 层选项，声明客户端必须发送受信任的 TLS 证书，否则直接拒绝 TLS TCP 连接。
10. **AOT (Ahead-Of-Time Compilation / 运行前编译)**：在运行前将 Java 字节码编译为机器码的技术，常配合 GraalVM 使用以获得极速启动和低资源消耗。
11. **GraalVM Native Image**：将 JVM 应用打包为独立的可执行二进制镜像，完全不依赖外部 JVM 的 AOT技术。
12. **ACL (Access Control List / 访问控制列表)**：声明了哪些网络主体、身份对特定资源和方法拥有哪些访问权限的白名单。
13. **Wildcard (通配符)**：用于匹配一组相似路径或方法的占位符。在 Kepsen 中，`pkg.Service/*` 匹配该服务下的所有方法。
14. **PEM (Privacy-Enhanced Mail)**：用于存储数字证书或私钥的标准 ASCII 文本文件格式，通常包含 `-----BEGIN CERTIFICATE-----`。
15. **TrustStore (信任库)**：存储受信任的第三方 CA 根证书的密钥库，用于验证外部传来的客户端证书的签名真伪。
16. **KeyStore (密钥库)**：存储自身私钥和证书链的安全介质，在 TLS 握手时将其呈现给外部对端以证明自身身份。
17. **CA (Certificate Authority / 证书签发机构)**：负责签发、分发和管理数字证书的权威公信机构。
18. **ASN.1 (Abstract Syntax Notation One)**：一种抽象语法标记规范，专门用于定义网络协议中的复杂数据结构（如证书格式）。
19. **PKI (Public Key Infrastructure / 公钥基础设施)**：利用公钥密码学理论对网络实体进行身份确认的技术体系与基础设施。
20. **Certificate Rotation (证书轮换)**：为避免证书到期失效或泄露风险，定期或在特定安全事件后生成新证书替换老证书的流程。
21. **Cipher Suite (密码套件)**：TLS 握手阶段双方用来协商安全特性的算法集组合（包括密钥交换、对称加密和哈希算法）。
22. **Forward Secrecy (前向保密)**：密码学通道的一个特性，保证即便服务端的长期私钥泄露，过去录制的网络加密流量也无法被破解。
23. **Transport Attributes (gRPC 传输属性)**：gRPC 通信流上附加的底层网络连接属性，例如客户端的对端 IP、端口以及 SSL 握手会话信息。
24. **ALPN (Application-Layer Protocol Negotiation)**：TLS 扩展协议，用于在握手阶段协商上层应用层协议类型（如 HTTP/2）。
25. **SSLContext (安全上下文)**：Netty/Java 内存中存储和管理安全配置、加密引擎和信任物料的核心会话上下文。
26. **BeanCreatedEventListener**：Micronaut 的事件响应切面，可以在特定的 Bean 声明创建后对其实行劫持和深度定制。
27. **ConfigMap**：Kubernetes 原生提供的卷挂载机制，专门用来把业务配置解耦到容器外进行注入和统一发布。
28. **Secret**：Kubernetes 原生提供的强加密存储和分发机制，常用来挂载服务端 TLS 证书、CA 根与敏感私钥。
29. **SSLSession**：底层 TLS 握手建立后，操作系统或网络驱动在内存中维持的、可供上层调用查询的加密信道属性会话。
30. **LdapName**：Java 提供的可对 RFC 2253 风格层级名称进行安全语法结构拆解的高安全解析器。
31. **X509Certificate**：国际电信联盟定义的、目前互联网微服务架构使用最为广泛的公钥证书标准规格。
32. **gRPC-Java**：谷歌开源的 gRPC 运行时库的 Java 标准官方实现。
33. **Apache License 2.0**：对商业和二次开发都极其友好的知名开源软件授权许可协议。
34. **Gradle**：面向企业级构建开发的高级、灵活的声明式打包发布构建工具。
35. **ClassReader / Reflection**：JVM 在运行时动态分析 and 读取类的特性的反射技术。Kepsen 通过精巧设计，已在此环节做到了几乎零反射开销。
36. **Classpath**：JVM 在运行时寻找类描述、字节码和配置文件资源的默认范围搜索路径。
37. **HexFormat**：JDK 17 后官方引入的、以极致性能将二进制数组转换为标准十六进制文本表示的高效格式化计算器。
38. **SPIRE (SPIFFE Runtime Environment)**：一整套云原生环境下能够根据负载特性进行工作负载身份（SVID）分发和自动化证书轮转的系统。
39. **Sidecar (边车模式)**：一种单机架构部署设计，在应用容器旁并列运行独立的代理网络进程，为应用容器无侵入地代理网络与安全通信。
40. **O(1) Complexity (常数时间复杂度)**：算法效率的最高指标，即数据规模不论如何增长，检索和操作消耗的时间开销均保持恒定。
