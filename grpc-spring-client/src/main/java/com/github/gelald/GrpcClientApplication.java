package com.github.gelald;

import com.github.gelald.client.HelloClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GrpcClientApplication implements CommandLineRunner {

    private final HelloClient helloClient;

    public GrpcClientApplication(HelloClient helloClient) {
        this.helloClient = helloClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }

    @Override
    public void run(String... args) {
        helloClient.sayHello("World");
    }
}
