# Kepsen Documentation Portal / 文档门户

Welcome to the Kepsen documentation directory. This folder contains high-quality system architecture overviews, developer onboarding guides, and wiki catalogs designed for senior backend developers and system architects.

欢迎来到 Kepsen 文档目录。本文件夹包含了面向高级后端开发人员和系统架构师的高水平系统架构概览、开发者新手成长路径与维基目录。

---

## Documents / 核心文档列表

### 1. [System Architecture Overview / 系统架构设计与概览](ARCHITECTURE.zh.md)
* **Description / 描述**: Deep dive into the server-side mTLS pipeline, gRPC interceptor mechanics, memory-efficient O(1) ACL rule evaluation engine, and physical component containers. Includes comprehensive Mermaid sequence and topology diagrams.
* **中文说明**: 深入分析服务端 mTLS 拦截管道、gRPC 拦截器原理、内存高效的常数复杂度 O(1) ACL 规则评估引擎，以及项目组件和模块划分。附带详尽的 Mermaid 架构图与时序图。

### 2. [Developer Wiki & Onboarding Guide / 开发者与架构师 Wiki 目录](WIKI.zh.md)
* **Description / 描述**: Progressive onboarding paths for new engineers. Features a core abstract conceptual implementation written in Python pseudocode, package and code navigation links, glossary definitions, and test/CI instructions.
* **中文说明**: 面向新进研发人员的渐进式成长与沉浸路径。提供用 Python 编写的核心安全鉴权逻辑抽象、关键代码与包的直达链接、本地自测 TLS 证书生成、单元测试运行方式以及 40+ 核心技术词汇表。

---

## Repository References / 仓库核心代码直达

For quick access, here are the main entry points of Kepsen's components:
为了快速开发，以下是 Kepsen 各核心组件的主要入口：

* **Core Engine / 核心引擎**:
  * [ClientIdentityExtractor.java](../kepsen-core/src/main/java/uk/sienne/grpcauth/core/ClientIdentityExtractor.java) - Certificate identity parsing / 证书身份解析器
  * [MethodAclAuthorizer.java](../kepsen-core/src/main/java/uk/sienne/grpcauth/core/MethodAclAuthorizer.java) - High-speed O(1) in-memory ACL search engine / 高速 O(1) 内存 ACL 鉴权引擎
* **gRPC Adaptor / gRPC 网卡适配器**:
  * [MtlsAclInterceptor.java](../kepsen-grpc/src/main/java/uk/sienne/grpcauth/grpc/MtlsAclInterceptor.java) - Core interceptor filter / 核心 gRPC 拦截过滤器
  * [NettyMtlsServerConfigurer.java](../kepsen-grpc/src/main/java/uk/sienne/grpcauth/grpc/NettyMtlsServerConfigurer.java) - Netty TLS channel SSL context setup / Netty 通道 TLS 会话配置器
* **Spring Boot Integration / Spring Boot 自动装配**:
  * [GrpcMtlsAutoConfiguration.java](../kepsen-spring-boot-starter/src/main/java/uk/sienne/grpcauth/spring/GrpcMtlsAutoConfiguration.java) - Global auto-configuration starter bean / 全局装配 Bean 与拦截器自动挂载
* **Micronaut Integration / Micronaut 依赖注入**:
  * [MicronautMtlsServerCustomizer.java](../kepsen-micronaut/src/main/java/uk/sienne/grpcauth/micronaut/MicronautMtlsServerCustomizer.java) - Server builder bean listening and customization / Netty 端口构建器属性动态劫持注入
