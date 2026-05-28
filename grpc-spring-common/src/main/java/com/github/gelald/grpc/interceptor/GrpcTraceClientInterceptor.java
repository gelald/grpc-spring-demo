package com.github.gelald.grpc.interceptor;

import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.CallOptions;
import io.grpc.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC 客户端 trace 拦截器 —— 在发起 RPC 调用前将 traceId/spanId/parentSpanId 注入到 Metadata 中。
 *
 * <p>生成逻辑：
 * <ul>
 *   <li>若当前线程 ThreadLocal 中没有 traceId（调用链入口），则生成新的 traceId 和 spanId</li>
 *   <li>为每次出站调用生成一个新的 spanId，当前 spanId 作为 parentSpanId 传播</li>
 *   <li>不主动清理 ThreadLocal —— 同线程串行调用自然共享同一个 traceId，
 *       线程终止时随 ThreadLocal 自动回收；服务端场景由 {@code GrpcTraceServerInterceptor} 负责清理</li>
 * </ul>
 *
 * <p>注册方式：各应用模块在 {@code @Configuration} 中通过 {@code @Bean + @GlobalClientInterceptor} 注册。
 * 不在 common 中自动注册的原因：
 * <ol>
 *   <li>common 模块没有完整 Spring 上下文，无法自动装配 Bean</li>
 *   <li>{@code @GlobalClientInterceptor} 注解定义在 spring-grpc-autoconfigure 中，common 不应依赖 starter</li>
 * </ol>
 */
public class GrpcTraceClientInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcTraceClientInterceptor.class);

    private static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("trace-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> SPAN_ID_KEY =
            Metadata.Key.of("span-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> PARENT_SPAN_ID_KEY =
            Metadata.Key.of("parent-span-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        String existingTraceId = GrpcTraceContext.getTraceId();
        boolean isRoot = (existingTraceId == null);

        final String traceId;
        if (isRoot) {
            traceId = GrpcTraceContext.generateId();
            String spanId = GrpcTraceContext.generateId();
            GrpcTraceContext.set(traceId, spanId, "");
            GrpcTraceContext.putToMdc();
            log.info("[gRPC Trace] 调用链入口: traceId={}, spanId={}", traceId, spanId);
        } else {
            traceId = existingTraceId;
        }

        final String currentSpanId = GrpcTraceContext.getSpanId();
        final String outboundSpanId = GrpcTraceContext.generateId();

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(TRACE_ID_KEY, traceId);
                headers.put(SPAN_ID_KEY, outboundSpanId);
                headers.put(PARENT_SPAN_ID_KEY, currentSpanId != null ? currentSpanId : "");

                log.info("[gRPC Trace] 出站调用 → {}: traceId={}, spanId={}, parentSpanId={}",
                        method.getFullMethodName(), traceId, outboundSpanId, currentSpanId);

                super.start(responseListener, headers);
            }
        };
    }
}
