package com.github.gelald.service;

import com.github.gelald.grpc.HelloReply;
import com.github.gelald.grpc.HelloRequest;
import com.github.gelald.grpc.HelloServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@GrpcService
public class HelloGrpcService extends HelloServiceGrpc.HelloServiceImplBase {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String message = "Hello, " + request.getName() + "!";
        HelloReply reply = HelloReply.newBuilder().setMessage(message).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void sayHelloServerStream(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        for (int i = 1; i <= 5; i++) {
            String message = "Hello, " + name + "! [" + i + "/5] " + LocalDateTime.now().format(FORMATTER);
            responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloClientStream(StreamObserver<HelloReply> responseObserver) {
        return new StreamObserver<>() {
            private final StringBuilder names = new StringBuilder();

            @Override
            public void onNext(HelloRequest request) {
                if (!names.isEmpty()) {
                    names.append(", ");
                }
                names.append(request.getName());
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                String message = "Hello to everyone: " + names + "!";
                responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloBiStream(StreamObserver<HelloReply> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(HelloRequest request) {
                String message = "Hello, " + request.getName() + "! " + LocalDateTime.now().format(FORMATTER);
                responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}