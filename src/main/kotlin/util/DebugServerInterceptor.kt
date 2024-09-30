package org.mcpq.main.util

import io.grpc.*
import org.mcpq.main.MCPQPlugin

class DebugServerInterceptor(val plugin: MCPQPlugin) : ServerInterceptor {

    private inner class ExceptionTranslatingServerCall<ReqT, RespT>(
        delegate: ServerCall<ReqT, RespT>?
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {

        override fun close(status: Status?, trailers: io.grpc.Metadata?) {
            val newStatus = if (!status?.isOk!!) {
                val cause = status.cause

                plugin.debug { "closing with status: ${status.code}" }
                plugin.debug { "closing with cause: ${status.cause}" }

                if (status.code == Status.Code.UNKNOWN) {
                    val newStatus = when (cause) {
                        is IllegalArgumentException -> Status.INVALID_ARGUMENT
                        is IllegalStateException -> Status.FAILED_PRECONDITION
//                        is NotFoundException -> Status.NOT_FOUND
//                        is ConflictException -> Status.ALREADY_EXISTS
//                        is UnauthenticationException -> Status.UNAUTHENTICATED
//                        is UnauthorizationException -> Status.PERMISSION_DENIED
                        else -> Status.UNKNOWN
                    }
                    newStatus.withDescription(cause?.message).withCause(cause)
                } else
                    status
            } else {
                plugin.debug_warn { "closing" }
                status
            }

            super.close(newStatus, trailers)
        }
    }

    private inner class LoggingServerCallListener<ReqT>(
        delegate: ServerCall.Listener<ReqT>
    ) : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {

        override fun onMessage(message: ReqT) {
            plugin.debug { "message: $message" }
            try {
                super.onMessage(message)
            } catch (t: Throwable) {
                plugin.debug_warn { "error on message ${t.stackTraceToString()}" }
                throw t
            }
        }

        override fun onHalfClose() {
            plugin.debug { "half-close" }
            try {
                super.onHalfClose()
            } catch (t: Throwable) {
                plugin.debug_warn { "error on half-close ${t.stackTraceToString()}" }
                throw t
            }
        }

        override fun onCancel() {
            plugin.debug { "cancel" }
            try {
                super.onCancel()
            } catch (t: Throwable) {
                plugin.debug_warn { "error on cancel ${t.stackTraceToString()}" }
                throw t
            }
        }

        override fun onComplete() {
            plugin.debug { "complete" }
            try {
                super.onComplete()
            } catch (t: Throwable) {
                plugin.debug_warn { "error on complete ${t.stackTraceToString()}" }
                throw t
            }
        }

        override fun onReady() {
            plugin.debug { "ready" }
            try {
                super.onReady()
            } catch (t: Throwable) {
                plugin.debug_warn { "error on ready ${t.stackTraceToString()}" }
                throw t
            }
        }
    }

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>?,
        headers: io.grpc.Metadata?,
        next: ServerCallHandler<ReqT, RespT>?
    ): ServerCall.Listener<ReqT> {
        return LoggingServerCallListener(next!!.startCall(ExceptionTranslatingServerCall(call), headers))
    }
}
