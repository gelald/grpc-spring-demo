package com.github.gelald.client;

import com.github.gelald.grpc.DeadlineServiceGrpc;
import com.github.gelald.grpc.SlowProcessRequest;
import com.github.gelald.grpc.SlowProcessResponse;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
public class DeadlineDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DeadlineDemoRunner.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final DeadlineServiceGrpc.DeadlineServiceBlockingStub deadlineStub;

    public DeadlineDemoRunner(DeadlineServiceGrpc.DeadlineServiceBlockingStub deadlineStub) {
        this.deadlineStub = deadlineStub;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n===== 7. Deadline 传播验证 =====");

        // 场景 A: 设置足够大的 deadline，执行一次正常请求链
        // client deadline=10s, downstream 处理=1s, server 处理=0.3s -> 不会过期
        testDeadline(
                "场景A: 正常链路请求",
                10000,       // client deadline: 10s
                1000       // downstream 处理时间: 1s
        );

        // 场景 B: deadline 在 server 到期
        // client deadline=2s, downstream 处理=10s, server 处理 10/3 = 3s → deadline 在 server 中到期
        testDeadline(
                "场景B: deadline在downstream到期",
                2000,       // client deadline: 2s
                10000       // downstream 处理时间: 10s
        );

        // 场景 C: deadline 在 downstream 到期
        // client deadline=5s, downstream 处理=10s, server 处理 10/3 = 3s → deadline 在 downstream 中到期
        testDeadline(
                "场景C: 极短deadline验证",
                5000,       // client deadline: 5s
                10000       // downstream 处理时间: 10s
        );


        System.out.println("\n===== Deadline 验证完成 =====");
    }

    private void testDeadline(String scenarioName, long deadlineMs, int downstreamProcessingMs) {
        System.out.println("\n--- " + scenarioName + " ---");
        System.out.println("[Client] 设置 deadline=" + deadlineMs + "ms, downstream处理时间=" + downstreamProcessingMs + "ms, 时间=" + LocalDateTime.now().format(FORMATTER));

        SlowProcessRequest request = SlowProcessRequest.newBuilder()
                .setName(scenarioName)
                .setProcessingTimeMs(downstreamProcessingMs)
                .build();

        try {
            // withDeadlineAfter 会创建一个附带 deadline 的新 stub（不影响原始 bean）
            SlowProcessResponse response = deadlineStub
                    .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .slowProcess(request);

            System.out.println("[Client] 收到响应: " + response.getMessage()
                    + ", downstream看到的剩余deadline=" + response.getRemainingDeadlineMs() + "ms"
                    + ", 时间=" + LocalDateTime.now().format(FORMATTER));
        } catch (StatusRuntimeException e) {
            System.out.println("[Client] 请求失败: status=" + e.getStatus().getCode()
                    + ", message=" + e.getStatus().getDescription()
                    + ", 时间=" + LocalDateTime.now().format(FORMATTER));
            log.error("Deadline 到期详情", e);
        }
    }
}
