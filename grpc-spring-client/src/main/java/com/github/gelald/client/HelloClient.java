package com.github.gelald.client;

import com.github.gelald.grpc.HelloReply;
import com.github.gelald.grpc.HelloRequest;
import com.github.gelald.grpc.HelloServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class HelloClient {

    private final HelloServiceGrpc.HelloServiceBlockingStub blockingStub;
    private final HelloServiceGrpc.HelloServiceStub asyncStub;

    public HelloClient(GrpcChannelFactory channels) {
        var channel = channels.createChannel("hello-service");
        this.blockingStub = HelloServiceGrpc.newBlockingStub(channel);
        this.asyncStub = HelloServiceGrpc.newStub(channel);
    }

    public void sayHello(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply reply = blockingStub.sayHello(request);
        System.out.println("  -> " + reply.getMessage());
    }

    public void sayHelloServerStream(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        var it = blockingStub.sayHelloServerStream(request);
        while (it.hasNext()) {
            HelloReply reply = it.next();
            System.out.println("  -> " + reply.getMessage());
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