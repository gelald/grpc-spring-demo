package com.github.gelald;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.BlockingStubFactory;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.client.SimpleStubFactory;

@SpringBootApplication
// target填写的是channel，和配置文件要一一对应
// BlockingStub  → sayHello(Unary)
@ImportGrpcClients(target = "grpc-demo-server", factory = BlockingStubFactory.class)
// AsyncStub(Stub) → sayHelloClientStream 等
@ImportGrpcClients(target = "grpc-demo-server", factory = SimpleStubFactory.class)
public class GrpcClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }
}
