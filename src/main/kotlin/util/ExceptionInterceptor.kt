package org.mcpq.main.util

import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.mcpq.main.MCPQPlugin.Companion.logger

/**
 * Log all exceptions thrown from gRPC endpoints, and adjust Status for known exceptions.
 * Adapted from https://github.com/grpc/grpc-kotlin/issues/141
 */
class ExceptionInterceptor : ServerInterceptor {

    /**
     * When closing a gRPC call, extract any error status information to top-level fields. Also
     * log the cause of errors.
     */
    private class ExceptionTranslatingServerCall<ReqT, RespT>(
        delegate: ServerCall<ReqT, RespT>
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {

        override fun close(status: Status, trailers: Metadata) {
            if (status.isOk) {
                return super.close(status, trailers)
            }
            logger.severe(status.cause.toString())
            super.close(status, trailers)
        }
    }

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        return next.startCall(ExceptionTranslatingServerCall(call), headers)
    }
}