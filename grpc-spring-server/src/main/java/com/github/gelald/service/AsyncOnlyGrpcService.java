package com.github.gelald.service;

import com.github.gelald.grpc.AsyncHelloReply;
import com.github.gelald.grpc.AsyncHelloRequest;
import com.github.gelald.grpc.AsyncOnlyServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class AsyncOnlyGrpcService extends AsyncOnlyServiceGrpc.AsyncOnlyServiceImplBase {

    @Override
    public StreamObserver<AsyncHelloRequest> sayAsyncHelloClientStream(StreamObserver<AsyncHelloReply> responseObserver) {
        return new StreamObserver<>() {
            private final StringBuilder names = new StringBuilder();

            @Override
            public void onNext(AsyncHelloRequest request) {
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
                String message = "Async Hello to everyone: " + names + "!";
                responseObserver.onNext(AsyncHelloReply.newBuilder().setMessage(message).build());
                responseObserver.onCompleted();
            }
        };
    }
}
