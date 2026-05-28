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
public class DownstreamDeadlineService extends DeadlineServiceGrpc.DeadlineServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(DownstreamDeadlineService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Override
    public void slowProcess(SlowProcessRequest request, StreamObserver<SlowProcessResponse> responseObserver) {
        // 通过 Context.current().getDeadline() 获取传播过来的 deadline
        Deadline deadline = Context.current().getDeadline();
        long remainingMs = deadline != null ? deadline.timeRemaining(TimeUnit.MILLISECONDS) : -1;

        log.info("[Downstream] 收到请求: name={}, 要求处理时间={}ms, 传播过来的剩余deadline={}ms, 时间={}",
                request.getName(), request.getProcessingTimeMs(), remainingMs,
                LocalDateTime.now().format(FORMATTER));

        try {
            // 模拟耗时处理
            TimeUnit.MILLISECONDS.sleep(request.getProcessingTimeMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Downstream] 处理被中断, 时间={}", LocalDateTime.now().format(FORMATTER));
            responseObserver.onError(e);
            return;
        }

        SlowProcessResponse response = SlowProcessResponse.newBuilder()
                .setMessage("[Downstream] 处理完成: " + request.getName() + " @ " + LocalDateTime.now().format(FORMATTER))
                .setRemainingDeadlineMs(remainingMs)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        log.info("[Downstream] 处理完成: name={}, 时间={}", request.getName(), LocalDateTime.now().format(FORMATTER));
    }
}
