package com.github.gelald.service;

import com.github.gelald.grpc.DeadlineServiceGrpc;
import com.github.gelald.grpc.SlowProcessRequest;
import com.github.gelald.grpc.SlowProcessResponse;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@GrpcService
public class ServerDeadlineService extends DeadlineServiceGrpc.DeadlineServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ServerDeadlineService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final DeadlineServiceGrpc.DeadlineServiceBlockingStub downstreamStub;

    public ServerDeadlineService(DeadlineServiceGrpc.DeadlineServiceBlockingStub downstreamStub) {
        this.downstreamStub = downstreamStub;
    }

    @Override
    public void slowProcess(SlowProcessRequest request, StreamObserver<SlowProcessResponse> responseObserver) {
        Deadline deadline = Context.current().getDeadline();
        long remainingMs = deadline != null ? deadline.timeRemaining(TimeUnit.MILLISECONDS) : -1;

        // 抽取 1/3 作为自己的处理时间，尝试模拟请求链路在此就中断
        int processingTimeMs = request.getProcessingTimeMs();
        log.info("[Server] 收到请求: name={}, 下游处理时间={}ms, 传播过来的剩余deadline={}ms, 时间={}",
                request.getName(), processingTimeMs, remainingMs,
                LocalDateTime.now().format(FORMATTER));

        try {
            // 模拟耗时处理
            TimeUnit.MILLISECONDS.sleep(processingTimeMs / 3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Server] 处理被中断, 时间={}", LocalDateTime.now().format(FORMATTER));
            responseObserver.onError(e);
            return;
        }

        try {
            // 不设自己的 deadline，直接转发到 downstream
            // gRPC Context 会自动传播 client 的 deadline
            SlowProcessResponse downstreamResponse = downstreamStub.slowProcess(request);
            responseObserver.onNext(downstreamResponse);
            responseObserver.onCompleted();
            log.info("[Server] downstream 处理完成，时间={}", LocalDateTime.now().format(FORMATTER));
        } catch (Exception e) {
            // deadline 到期时 downstream 会返回 DEADLINE_EXCEEDED，传播回 client
            log.error("[Server] 调用 downstream 失败: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}
