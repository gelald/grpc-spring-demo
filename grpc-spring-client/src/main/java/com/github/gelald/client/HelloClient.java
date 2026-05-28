package com.github.gelald.client;

import com.github.gelald.grpc.HelloReply;
import com.github.gelald.grpc.HelloRequest;
import com.github.gelald.grpc.HelloServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class HelloClient {

    private final HelloServiceGrpc.HelloServiceBlockingStub blockingStub;
    private final HelloServiceGrpc.HelloServiceStub asyncStub;

    // ==================== 原手工注入方式（已注释，供对比） ====================
    // @Autowired
    // public HelloClient(GrpcChannelFactory channels) {
    //     var channel = channels.createChannel("hello-service");
    //     this.blockingStub = HelloServiceGrpc.newBlockingStub(channel);
    //     this.asyncStub = HelloServiceGrpc.newStub(channel);
    // }
    // =====================================================================

    // 新方式：直接注入由 @ImportGrpcClients 自动创建的 stub Bean
    // 同时兼容测试：测试中直接传入 stub 实例，走同一构造器
    public HelloClient(HelloServiceGrpc.HelloServiceBlockingStub blockingStub,
                       HelloServiceGrpc.HelloServiceStub asyncStub) {
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }

    public void sayHello(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply reply = blockingStub.sayHello(request);
        System.out.println("  -> " + reply.getMessage());
        log.info("sayHello 完结");
    }

    public void sayHelloServerStream(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        var it = blockingStub.sayHelloServerStream(request);
        while (it.hasNext()) {
            HelloReply reply = it.next();
            System.out.println("  -> " + reply.getMessage());
            log.info("sayHelloServerStream 返回");
        }
    }

    public void sayHelloClientStream(String... names) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloClientStream(new StreamObserver<>() {
            @Override
            public void onNext(HelloReply reply) {
                System.out.println("  -> " + reply.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("  -> Error: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("sayHelloClientStream 完结");
                latch.countDown();
            }
        });

        for (String name : names) {
            requestObserver.onNext(HelloRequest.newBuilder().setName(name).build());
            System.out.println("  <- Sent: " + name);
            Thread.sleep(200);
        }
        requestObserver.onCompleted();

        latch.await(10, TimeUnit.SECONDS);
    }

    public void sayHelloBiStream(String... names) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloBiStream(new StreamObserver<>() {
            @Override
            public void onNext(HelloReply reply) {
                System.out.println("  -> " + reply.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("  -> Error: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("  -> Server done.");
                log.info("sayHelloBiStream 完结");
                latch.countDown();
            }
        });

        for (String name : names) {
            requestObserver.onNext(HelloRequest.newBuilder().setName(name).build());
            System.out.println("  <- Sent: " + name);
            Thread.sleep(300);
        }
        requestObserver.onCompleted();

        latch.await(10, TimeUnit.SECONDS);
    }
}