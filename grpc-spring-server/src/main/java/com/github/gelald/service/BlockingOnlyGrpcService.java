package com.github.gelald.service;

import com.github.gelald.grpc.BlockingHelloReply;
import com.github.gelald.grpc.BlockingHelloRequest;
import com.github.gelald.grpc.BlockingOnlyServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class BlockingOnlyGrpcService extends BlockingOnlyServiceGrpc.BlockingOnlyServiceImplBase {

    @Override
    public void sayBlockingHello(BlockingHelloRequest request, StreamObserver<BlockingHelloReply> responseObserver) {
        String message = "Blocking Hello, " + request.getName() + "!";
        BlockingHelloReply reply = BlockingHelloReply.newBuilder().setMessage(message).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
