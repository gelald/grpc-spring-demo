package com.github.gelald.service;

import com.github.gelald.grpc.HelloReply;
import com.github.gelald.grpc.HelloRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 服务端单元测试 — 纯 Mockito，无需启动 Spring Context
 *
 * 核心思路：mock StreamObserver，捕获 onNext() 传入的响应消息，验证内容和调用次数。
 * 这是最轻量的测试方式，只关注业务逻辑本身。
 */
@ExtendWith(MockitoExtension.class)
class HelloGrpcServiceTest {

    private HelloGrpcService service;

    @BeforeEach
    void setUp() {
        service = new HelloGrpcService();
    }

    @SuppressWarnings("unchecked")
    private StreamObserver<HelloReply> mockObserver() {
        return mock(StreamObserver.class);
    }

    // ===== Unary RPC =====

    @Nested
    @DisplayName("sayHello — Unary RPC")
    class SayHelloTest {

        @Test
        @DisplayName("正常请求应返回 'Hello, {name}!'")
        void shouldReturnGreeting() {
            var observer = mockObserver();
            var request = HelloRequest.newBuilder().setName("World").build();

            service.sayHello(request, observer);

            var captor = ArgumentCaptor.forClass(HelloReply.class);
            verify(observer).onNext(captor.capture());
            verify(observer).onCompleted();
            // 确认只调用了一次 onNext
            verify(observer, times(1)).onNext(any());
            assertEquals("Hello, World!", captor.getValue().getMessage());
        }

        @Test
        @DisplayName("不同 name 应返回对应的问候语")
        void shouldReturnDifferentGreetingForDifferentName() {
            var observer = mockObserver();
            var request = HelloRequest.newBuilder().setName("Alice").build();

            service.sayHello(request, observer);

            var captor = ArgumentCaptor.forClass(HelloReply.class);
            verify(observer).onNext(captor.capture());
            assertEquals("Hello, Alice!", captor.getValue().getMessage());
        }
    }

    // ===== Server Streaming RPC =====

    @Nested
    @DisplayName("sayHelloServerStream — 服务端流式 RPC")
    class SayHelloServerStreamTest {

        @Test
        @DisplayName("应返回 5 条消息")
        void shouldReturn5Messages() {
            var observer = mockObserver();
            var request = HelloRequest.newBuilder().setName("World").build();

            service.sayHelloServerStream(request, observer);

            var captor = ArgumentCaptor.forClass(HelloReply.class);
            verify(observer, times(5)).onNext(captor.capture());
            verify(observer).onCompleted();

            List<HelloReply> replies = captor.getAllValues();
            assertEquals(5, replies.size());
        }

        @Test
        @DisplayName("每条消息应包含正确的序号 [i/5]")
        void shouldContainCorrectIndex() {
            var observer = mockObserver();
            var request = HelloRequest.newBuilder().setName("World").build();

            service.sayHelloServerStream(request, observer);

            var captor = ArgumentCaptor.forClass(HelloReply.class);
            verify(observer, times(5)).onNext(captor.capture());

            List<HelloReply> replies = captor.getAllValues();
            for (int i = 0; i < 5; i++) {
                String message = replies.get(i).getMessage();
                assertTrue(message.contains("[" + (i + 1) + "/5]"),
                        "第 " + (i + 1) + " 条消息应包含序号 [" + (i + 1) + "/5]");
            }
        }

        @Test
        @DisplayName("每条消息应包含 name")
        void shouldContainName() {
            var observer = mockObserver();
            var request = HelloRequest.newBuilder().setName("TestUser").build();

            service.sayHelloServerStream(request, observer);

            var captor = ArgumentCaptor.forClass(HelloReply.class);
            verify(observer, times(5)).onNext(captor.capture());

            for (HelloReply reply : captor.getAllValues()) {
                assertTrue(reply.getMessage().contains("TestUser"));
            }
        }
    }

    // ===== Client Streaming RPC =====

    @Nested
    @DisplayName("sayHelloClientStream — 客户端流式 RPC")
    class SayHelloClientStreamTest {

        @Test
        @DisplayName("应将多个 name 聚合成一条消息返回")
        void shouldAggregateNames() {
            var observer = mockObserver();
            // sayHelloClientStream 返回一个请求观察者，客户端通过它发送消息
            StreamObserver<HelloRequest> requestObserver = service.sayHelloClientStream(observer);

            requestObserver.onNext(HelloRequest.newBuilder().setName("Alice").build());
            requestObserver.onNext(HelloRequest.newBuilder().setName("Bob").build());
            requestObserver.onNext(HelloRequest.newBuilder().setName("Charlie").build());
            requestObserver.onCompleted();

            var captor = ArgumentCaptor.forClass(HelloReply.class);
            verify(observer).onNext(captor.capture());
            verify(observer).onCompleted();
            assertEquals("Hello to everyone: Alice, Bob, Charlie!", captor.getValue().getMessage());
        }

        @Test
        @DisplayName("单个 name 也应正常工作")
        void shouldWorkWithSingleName() {
            var observer = mockObserver();
            StreamObserver<HelloRequest> requestObserver = service.sayHelloClientStream(observer);

            requestObserver.onNext(HelloRequest.newBuilder().setName("Alice").build());
            requestObserver.onCompleted();

            var captor = ArgumentCaptor.forClass(HelloReply.class);
            verify(observer).onNext(captor.capture());
            assertEquals("Hello to everyone: Alice!", captor.getValue().getMessage());
        }

        @Test
        @DisplayName("onError 应转发给响应观察者")
        void shouldPropagateError() {
            var observer = mockObserver();
            StreamObserver<HelloRequest> requestObserver = service.sayHelloClientStream(observer);

            var error = new RuntimeException("test error");
            requestObserver.onError(error);

            verify(observer).onError(error);
            // 出错后不应再调用 onNext / onCompleted
            verify(observer, never()).onNext(any());
            verify(observer, never()).onCompleted();
        }
    }

    // ===== Bidirectional Streaming RPC =====

    @Nested
    @DisplayName("sayHelloBiStream — 双向流式 RPC")
    class SayHelloBiStreamTest {

        @Test
        @DisplayName("每条请求应立即回显一条响应")
        void shouldEchoEachMessage() {
            var observer = mockObserver();
            StreamObserver<HelloRequest> requestObserver = service.sayHelloBiStream(observer);

            requestObserver.onNext(HelloRequest.newBuilder().setName("Alice").build());
            requestObserver.onNext(HelloRequest.newBuilder().setName("Bob").build());
            requestObserver.onCompleted();

            var captor = ArgumentCaptor.forClass(HelloReply.class);
            verify(observer, times(2)).onNext(captor.capture());
            verify(observer).onCompleted();

            List<HelloReply> replies = captor.getAllValues();
            assertTrue(replies.get(0).getMessage().startsWith("Hello, Alice!"));
            assertTrue(replies.get(1).getMessage().startsWith("Hello, Bob!"));
        }

        @Test
        @DisplayName("onError 应转发给响应观察者")
        void shouldPropagateError() {
            var observer = mockObserver();
            StreamObserver<HelloRequest> requestObserver = service.sayHelloBiStream(observer);

            var error = new RuntimeException("test error");
            requestObserver.onError(error);

            verify(observer).onError(error);
            verify(observer, never()).onNext(any());
            verify(observer, never()).onCompleted();
        }
    }
}
