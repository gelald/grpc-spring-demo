package com.github.gelald.client;

import com.github.gelald.grpc.HelloServiceGrpc;
import com.github.gelald.service.HelloGrpcService;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户端单元测试 — 使用 InProcess gRPC 通道
 *
 * 核心思路：
 *   GrpcCleanupRule        → 自动管理 gRPC 资源的生命周期（关闭 server/channel）
 *   InProcessServerBuilder → 创建进程内 gRPC 服务器，不走网络栈，速度快
 *   直接复用 server 模块的 HelloGrpcService，避免重复编写测试桩
 */
class HelloClientTest {

    private final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private HelloClient helloClient;
    private ByteArrayOutputStream capturedOutput;

    @BeforeEach
    void setUp() throws Exception {
        // 每次测试使用唯一名称，避免重复注册
        var serverName = "test-server-" + ThreadLocalRandom.current().nextInt(100000);

        // 启动 in-process gRPC 服务器，直接复用真实的服务实现
        grpcCleanup.register(
                io.grpc.inprocess.InProcessServerBuilder
                        .forName(serverName)
                        .directExecutor()
                        .addService(new HelloGrpcService())
                        .build()
                        .start()
        );

        // 创建 in-process 通道 + stubs
        var channel = grpcCleanup.register(
                io.grpc.inprocess.InProcessChannelBuilder
                        .forName(serverName)
                        .directExecutor()
                        .build()
        );
        helloClient = new HelloClient(
                HelloServiceGrpc.newBlockingStub(channel),
                HelloServiceGrpc.newStub(channel)
        );

        // 捕获 System.out 输出以便断言
        capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput));
    }

    @Nested
    @DisplayName("sayHello — Unary RPC")
    class SayHelloTest {

        @Test
        @DisplayName("应打印正确的问候语")
        void shouldPrintGreeting() {
            helloClient.sayHello("World");

            assertTrue(capturedOutput.toString().contains("Hello, World!"));
        }
    }

    @Nested
    @DisplayName("sayHelloServerStream — 服务端流式 RPC")
    class SayHelloServerStreamTest {

        @Test
        @DisplayName("应收到并打印 5 条流式消息")
        void shouldPrintStreamMessages() {
            helloClient.sayHelloServerStream("World");

            String output = capturedOutput.toString();
            assertTrue(output.contains("[1/5]"));
            assertTrue(output.contains("[5/5]"));
        }
    }

    @Nested
    @DisplayName("sayHelloClientStream — 客户端流式 RPC")
    class SayHelloClientStreamTest {

        @Test
        @DisplayName("应将多个 name 聚合并打印")
        void shouldPrintAggregatedMessage() throws InterruptedException {
            helloClient.sayHelloClientStream("Alice", "Bob");

            Thread.sleep(1000);
            assertTrue(capturedOutput.toString().contains("Hello to everyone: Alice, Bob!"));
        }
    }

    @Nested
    @DisplayName("sayHelloBiStream — 双向流式 RPC")
    class SayHelloBiStreamTest {

        @Test
        @DisplayName("应收到并打印每条回显消息")
        void shouldPrintEchoMessages() throws InterruptedException {
            helloClient.sayHelloBiStream("Alice", "Bob");

            Thread.sleep(1000);
            String output = capturedOutput.toString();
            assertTrue(output.contains("Hello, Alice!"));
            assertTrue(output.contains("Hello, Bob!"));
        }
    }
}
