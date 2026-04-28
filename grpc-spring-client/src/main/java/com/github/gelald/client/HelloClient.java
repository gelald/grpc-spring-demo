package com.github.gelald.client;

import com.github.gelald.grpc.HelloReply;
import com.github.gelald.grpc.HelloRequest;
import com.github.gelald.grpc.HelloServiceGrpc;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Component
public class HelloClient {

    private final HelloServiceGrpc.HelloServiceBlockingStub helloServiceStub;

    public HelloClient(GrpcChannelFactory channels) {
        this.helloServiceStub = HelloServiceGrpc.newBlockingStub(
                channels.createChannel("hello-service"));
    }

    public void sayHello(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply reply = helloServiceStub.sayHello(request);
        System.out.println("Greeting: " + reply.getMessage());
    }
}
