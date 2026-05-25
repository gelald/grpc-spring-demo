package com.github.gelald.client;

import com.github.gelald.grpc.AsyncHelloReply;
import com.github.gelald.grpc.AsyncHelloRequest;
import com.github.gelald.grpc.AsyncOnlyServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class AsyncOnlyClient {

    private final AsyncOnlyServiceGrpc.AsyncOnlyServiceStub asyncStub;

    public AsyncOnlyClient(AsyncOnlyServiceGrpc.AsyncOnlyServiceStub asyncStub) {
        this.asyncStub = asyncStub;
    }

    public void sayAsyncHelloClientStream(String... names) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<AsyncHelloRequest> requestObserver = asyncStub.sayAsyncHelloClientStream(new StreamObserver<>() {
            @Override
            public void onNext(AsyncHelloReply reply) {
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
            requestObserver.onNext(AsyncHelloRequest.newBuilder().setName(name).build());
            System.out.println("  <- Sent: " + name);
            Thread.sleep(200);
        }
        requestObserver.onCompleted();

        latch.await(10, TimeUnit.SECONDS);
    }
}
