package com.github.gelald.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class HelloClientRunner implements CommandLineRunner {

    private final HelloClient helloClient;
    private final BlockingOnlyClient blockingOnlyClient;
    private final AsyncOnlyClient asyncOnlyClient;

    public HelloClientRunner(HelloClient helloClient,
                             BlockingOnlyClient blockingOnlyClient,
                             AsyncOnlyClient asyncOnlyClient) {
        this.helloClient = helloClient;
        this.blockingOnlyClient = blockingOnlyClient;
        this.asyncOnlyClient = asyncOnlyClient;
    }

    @Override
    public void run(String... args) throws Exception {
        // === HelloService（原有，涵盖 4 种通信模式）===
        System.out.println("===== 1. Unary RPC =====");
        helloClient.sayHello("World");

        System.out.println("\n===== 2. Server Streaming RPC =====");
        helloClient.sayHelloServerStream("World");

        System.out.println("\n===== 3. Client Streaming RPC =====");
        helloClient.sayHelloClientStream("Alice", "Bob", "Charlie");

        System.out.println("\n===== 4. Bidirectional Streaming RPC =====");
        helloClient.sayHelloBiStream("Alice", "Bob", "Charlie");

        // === BlockingOnlyService（仅用 BlockingStub）===
        System.out.println("\n===== 5. BlockingOnly — BlockingStub (Unary) =====");
        blockingOnlyClient.sayBlockingHello("World");

        // === AsyncOnlyService（仅用 AsyncStub）===
        System.out.println("\n===== 6. AsyncOnly — AsyncStub (Client Streaming) =====");
        asyncOnlyClient.sayAsyncHelloClientStream("Alice", "Bob", "Charlie");

        System.out.println("\n===== All demos completed =====");
    }
}
