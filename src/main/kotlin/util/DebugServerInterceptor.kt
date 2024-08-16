package org.mcpq.main.util

import io.grpc.*
import org.mcpq.main.MCPQPlugin.Companion.logger

class DebugServerInterceptor : ServerInterceptor {

    private class ExceptionTranslatingServerCall<ReqT, RespT>(
        delegate: ServerCall<ReqT, RespT>?
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {

        override fun close(status: Status?, trailers: io.grpc.Metadata?) {
            val newStatus = if (!status?.isOk!!) {
                val cause = status.cause

                logger.info { "closing with status: ${status.code}" }
                logger.info { "closing with cause: ${status.cause}" }

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
                logger.warning { "closing" }
                status
            }

            super.close(newStatus, trailers)
        }
    }

    private class LoggingServerCallListener<ReqT>(
        delegate: ServerCall.Listener<ReqT>
    ) : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {

        override fun onMessage(message: ReqT) {
            logger.info { "message: $message" }
            try {
                super.onMessage(message)
            } catch (t: Throwable) {
                logger.warning { "error on message ${t.stackTraceToString()}" }
                throw t
            }
        }

        override fun onHalfClose() {
            logger.info { "half-close" }
            try {
                super.onHalfClose()
            } catch (t: Throwable) {
                logger.warning { "error on half-close ${t.stackTraceToString()}" }
                throw t
            }
        }

        override fun onCancel() {
            logger.info { "cancel" }
            try {
                super.onCancel()
            } catch (t: Throwable) {
                logger.warning { "error on cancel ${t.stackTraceToString()}" }
                throw t
            }
        }

        override fun onComplete() {
            logger.info { "complete" }
            try {
                super.onComplete()
            } catch (t: Throwable) {
                logger.warning { "error on complete ${t.stackTraceToString()}" }
                throw t
            }
        }

        override fun onReady() {
            logger.info { "ready" }
            try {
                super.onReady()
            } catch (t: Throwable) {
                logger.warning { "error on ready ${t.stackTraceToString()}" }
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
