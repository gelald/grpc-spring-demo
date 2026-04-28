# Spring gRPC Demo

基于 Spring Boot 4.0.6 + Spring gRPC 1.0.3 的 gRPC 入门示例项目。

## 项目结构

```
grpc-spring-demo/
├── grpc-spring-common/          # Proto 定义与编译
│   └── src/main/proto/
│       └── hello.proto
├── grpc-spring-server/          # gRPC 服务端
└── grpc-spring-client/          # gRPC 客户端
```

## 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | LTS |
| Maven | 3.9+ | 项目已包含 Maven Wrapper，无需预装 |

## 主要依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 4.0.6 | 基础框架 |
| Spring gRPC | 1.0.3 | gRPC 集成 |
| gRPC Java | 1.77.1 | gRPC 核心 |
| Protobuf | 4.33.4 | 序列化协议 |
| protobuf-maven-plugin | 4.0.3 | Proto 编译插件 |

## 模块依赖

### grpc-spring-common

| 依赖 | 作用 |
|------|------|
| `io.grpc:grpc-protobuf` | Protobuf 序列化支持 |
| `io.grpc:grpc-stub` | gRPC Stub 基类 |

负责存放 `.proto` 文件，通过 `protobuf-maven-plugin` 编译生成 Java 类，供 server 和 client 共享。

### grpc-spring-server

| 依赖 | 作用 |
|------|------|
| `grpc-spring-common` | 引入编译后的 Proto 类 |
| `spring-grpc-server-spring-boot-starter` | gRPC 服务端自动配置 |
| `io.grpc:grpc-services` | gRPC Reflection & Health Checking |

### grpc-spring-client

| 依赖 | 作用 |
|------|------|
| `grpc-spring-common` | 引入编译后的 Proto 类 |
| `spring-grpc-client-spring-boot-starter` | gRPC 客户端自动配置 |

## 快速开始

项目已包含 Maven Wrapper（绑定 Maven 3.9.9），拉取代码后无需手动安装 Maven，使用 `./mvnw`（Linux/macOS）或 `mvnw.cmd`（Windows）代替 `mvn` 即可。

### 1. 编译项目

```bash
./mvnw clean install
# Windows: mvnw.cmd clean install
```

### 2. 启动服务端

```bash
./mvnw -pl grpc-spring-server spring-boot:run
```

服务端默认监听 `9090` 端口，已开启 gRPC Reflection。

### 3. 启动客户端

```bash
./mvnw -pl grpc-spring-client spring-boot:run
```

客户端启动后自动调用 `sayHello("World")`，控制台输出 `Greeting: Hello, World!`。

### 4. 使用 grpcurl 验证（可选）

```bash
# 列出可用服务
grpcurl -plaintext localhost:9090 list

# 输出
com.github.gelald.grpc.HelloService
grpc.health.v1.Health
grpc.reflection.v1.ServerReflection
```

```bash
# 调用 HelloService
grpcurl -plaintext -d '{"name": "World"}' localhost:9090 com.github.gelald.grpc.HelloService/sayHello

# 输出
{
  "message": "Hello, World!"
}
```
