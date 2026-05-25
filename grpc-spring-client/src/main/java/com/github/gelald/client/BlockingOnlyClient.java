package com.github.gelald.client;

import com.github.gelald.grpc.BlockingHelloRequest;
import com.github.gelald.grpc.BlockingOnlyServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class BlockingOnlyClient {

    private final BlockingOnlyServiceGrpc.BlockingOnlyServiceBlockingStub blockingStub;

    public BlockingOnlyClient(BlockingOnlyServiceGrpc.BlockingOnlyServiceBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    public void sayBlockingHello(String name) {
        BlockingHelloRequest request = BlockingHelloRequest.newBuilder().setName(name).build();
        var reply = blockingStub.sayBlockingHello(request);
        System.out.println("  -> " + reply.getMessage());
    }
}
