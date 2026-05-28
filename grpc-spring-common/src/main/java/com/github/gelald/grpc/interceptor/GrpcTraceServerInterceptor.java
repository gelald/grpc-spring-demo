package com.github.gelald.grpc.interceptor;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC 服务端 trace 拦截器 —— 从请求 Metadata 中提取 traceId/spanId/parentSpanId，
 * 生成当前服务 spanId，存入 ThreadLocal 和 SLF4J MDC。
 *
 * <p>处理逻辑：
 * <ul>
 *   <li>从 gRPC Metadata 中提取 traceId、spanId（作为 parentSpanId）</li>
 *   <li>生成新的 spanId 作为当前服务 span</li>
 *   <li>调用完成后（{@link ServerCall#close}）自动清理 ThreadLocal 和 MDC</li>
 * </ul>
 *
 * <p>注册方式：各应用模块在 {@code @Configuration} 中通过 {@code @Bean + @GlobalServerInterceptor} 注册。
 * 不在 common 中自动注册的原因：
 * <ol>
 *   <li>common 模块没有完整 Spring 上下文，无法自动装配 Bean</li>
 *   <li>{@code @GlobalServerInterceptor} 注解定义在 spring-grpc-autoconfigure 中，common 不应依赖 starter</li>
 * </ol>
 */
public class GrpcTraceServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcTraceServerInterceptor.class);

    private static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("trace-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> SPAN_ID_KEY =
            Metadata.Key.of("span-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> PARENT_SPAN_ID_KEY =
            Metadata.Key.of("parent-span-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String traceId = headers.get(TRACE_ID_KEY);
        String incomingSpanId = headers.get(SPAN_ID_KEY);
        String incomingParentSpanId = headers.get(PARENT_SPAN_ID_KEY);

        if (traceId == null) {
            traceId = GrpcTraceContext.generateId();
            log.info("[gRPC Trace] 无上游 traceId，生成新的: traceId={}", traceId);
        }

        String currentSpanId = GrpcTraceContext.generateId();
        GrpcTraceContext.set(traceId, currentSpanId,
                incomingSpanId != null ? incomingSpanId : "");
        GrpcTraceContext.putToMdc();

        log.info("[gRPC Trace] 入站请求 {}: traceId={}, spanId={}, parentSpanId={}",
                call.getMethodDescriptor().getFullMethodName(),
                traceId, currentSpanId, incomingSpanId);

        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall
                .SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                try {
                    super.close(status, trailers);
                } finally {
                    log.info("[gRPC Trace] 请求完成 {}: traceId={}, spanId={}",
                            getMethodDescriptor().getFullMethodName(),
                            GrpcTraceContext.getTraceId(), GrpcTraceContext.getSpanId());
                    GrpcTraceContext.clear();
                    GrpcTraceContext.clearMdc();
                }
            }
        };

        return next.startCall(wrappedCall, headers);
    }
}
