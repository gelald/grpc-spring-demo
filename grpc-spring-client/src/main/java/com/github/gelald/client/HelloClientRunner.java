package com.github.gelald.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class HelloClientRunner implements CommandLineRunner {

    private final HelloClient helloClient;

    public HelloClientRunner(HelloClient helloClient) {
        this.helloClient = helloClient;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("===== 1. Unary RPC =====");
        helloClient.sayHello("World");

        System.out.println("\n===== 2. Server Streaming RPC =====");
        helloClient.sayHelloServerStream("World");

        System.out.println("\n===== 3. Client Streaming RPC =====");
        helloClient.sayHelloClientStream("Alice", "Bob", "Charlie");

        System.out.println("\n===== 4. Bidirectional Streaming RPC =====");
        helloClient.sayHelloBiStream("Alice", "Bob", "Charlie");

        System.out.println("\n===== All demos completed =====");
    }
}
