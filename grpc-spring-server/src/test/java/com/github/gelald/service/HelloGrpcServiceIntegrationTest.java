package com.github.gelald.service;

import com.github.gelald.grpc.HelloReply;
import com.github.gelald.grpc.HelloRequest;
import com.github.gelald.grpc.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 服务端集成测试 — 启动真实 Spring Context + gRPC 服务器
 *
 * 核心思路：
 *   spring.grpc.server.port=0  → 随机端口，避免端口冲突
 *   @Value("${local.grpc.port}") → 注入实际分配的端口
 *   通过 ManagedChannelBuilder 创建 stub，发起真实 gRPC 调用
 *
 * 与单元测试的区别：这里经过完整的网络层（序列化 → 网络 → 反序列化），
 * 验证 Spring 配置、服务注册、消息编解码是否正常工作。
 */
@SpringBootTest(properties = "spring.grpc.server.port=0")
class HelloGrpcServiceIntegrationTest {

    private static ManagedChannel channel;
    private static HelloServiceGrpc.HelloServiceBlockingStub blockingStub;
    private static HelloServiceGrpc.HelloServiceStub asyncStub;

    @BeforeAll
    static void setUp(@Value("${local.grpc.port}") int port) {
        channel = ManagedChannelBuilder
                .forAddress("localhost", port)
                .usePlaintext()
                .build();
        blockingStub = HelloServiceGrpc.newBlockingStub(channel);
        asyncStub = HelloServiceGrpc.newStub(channel);
    }

    @AfterAll
    static void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    // ===== Unary RPC =====

    @Nested
    @DisplayName("sayHello — Unary RPC（集成测试）")
    class SayHelloTest {

        @Test
        @DisplayName("应返回正确的问候语")
        void shouldReturnGreeting() {
            var request = HelloRequest.newBuilder().setName("World").build();
            HelloReply reply = blockingStub.sayHello(request);

            assertEquals("Hello, World!", reply.getMessage());
        }
    }

    // ===== Server Streaming RPC =====

    @Nested
    @DisplayName("sayHelloServerStream — 服务端流式 RPC（集成测试）")
    class SayHelloServerStreamTest {

        @Test
        @DisplayName("应收到 5 条流式响应")
        void shouldReceive5Messages() {
            var request = HelloRequest.newBuilder().setName("World").build();
            List<HelloReply> replies = new ArrayList<>();

            var it = blockingStub.sayHelloServerStream(request);
            while (it.hasNext()) {
                replies.add(it.next());
            }

            assertEquals(5, replies.size());
            assertTrue(replies.get(0).getMessage().contains("[1/5]"));
            assertTrue(replies.get(4).getMessage().contains("[5/5]"));
        }
    }

    // ===== Client Streaming RPC =====

    @Nested
    @DisplayName("sayHelloClientStream — 客户端流式 RPC（集成测试）")
    class SayHelloClientStreamTest {

        @Test
        @DisplayName("应将多个 name 聚合为一条消息")
        void shouldAggregateNames() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            List<HelloReply> replies = new ArrayList<>();

            StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloClientStream(new StreamObserver<>() {
                @Override
                public void onNext(HelloReply reply) {
                    replies.add(reply);
                }

                @Override
                public void onError(Throwable t) {
                    fail("不应发生错误: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    latch.countDown();
                }
            });

            requestObserver.onNext(HelloRequest.newBuilder().setName("Alice").build());
            requestObserver.onNext(HelloRequest.newBuilder().setName("Bob").build());
            requestObserver.onCompleted();

            assertTrue(latch.await(10, TimeUnit.SECONDS), "应在超时前完成");
            assertEquals(1, replies.size());
            assertEquals("Hello to everyone: Alice, Bob!", replies.get(0).getMessage());
        }
    }

    // ===== Bidirectional Streaming RPC =====

    @Nested
    @DisplayName("sayHelloBiStream — 双向流式 RPC（集成测试）")
    class SayHelloBiStreamTest {

        @Test
        @DisplayName("应实时回显每条消息")
        void shouldEchoMessages() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            List<HelloReply> replies = new ArrayList<>();

            StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloBiStream(new StreamObserver<>() {
                @Override
                public void onNext(HelloReply reply) {
                    replies.add(reply);
                }

                @Override
                public void onError(Throwable t) {
                    fail("不应发生错误: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    latch.countDown();
                }
            });

            requestObserver.onNext(HelloRequest.newBuilder().setName("Alice").build());
            requestObserver.onNext(HelloRequest.newBuilder().setName("Bob").build());
            requestObserver.onNext(HelloRequest.newBuilder().setName("Charlie").build());
            requestObserver.onCompleted();

            assertTrue(latch.await(10, TimeUnit.SECONDS), "应在超时前完成");
            assertEquals(3, replies.size());
            assertTrue(replies.get(0).getMessage().startsWith("Hello, Alice!"));
            assertTrue(replies.get(1).getMessage().startsWith("Hello, Bob!"));
            assertTrue(replies.get(2).getMessage().startsWith("Hello, Charlie!"));
        }
    }
}
