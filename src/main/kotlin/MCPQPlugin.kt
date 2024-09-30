package org.mcpq.main

import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.mcpq.main.util.DebugServerInterceptor
import org.mcpq.main.util.ExceptionInterceptor
import java.net.InetSocketAddress
import java.util.logging.Logger


class MCPQPlugin : JavaPlugin(), Listener {
    val service: CommandService = CommandService(this)
    var server: Server? = null
    var debug_mode_enabled = false

    companion object {
        // everything in here is static
        val logger : Logger = Bukkit.getLogger()
    }

    fun debug(message: () -> String) = debug_info(message)
    fun debug_info(message: () -> String) {
        if (debug_mode_enabled) {
            logger.info("[DEBUG] " + message())
        }
    }
    fun debug_warn(message: () -> String) {
        if (debug_mode_enabled) {
            logger.warning("[DEBUG] " + message())
        }
    }
    fun info(message: () -> String) {
        logger.info(message())
    }
    fun warn(message: () -> String) {
        logger.warning(message())
    }
    fun error(message: () -> String) {
        logger.severe(message())
    }

    override fun onEnable() {
        super.onEnable()
        info { "Plugin is starting..." }
        saveDefaultConfig() // save a copy of the default config.yml if one does not exist

        debug_mode_enabled = config.getBoolean("debug")
        val port = config.getInt("port")
        val host = config.getString("host")

        debug_warn { "Plugin in DEBUG mode!" }

        info { "Try binding to $host:$port" }
        val address = InetSocketAddress(host, port)
        val serverBuilder = NettyServerBuilder
            .forAddress(address)
            .addService(service)

        if (debug_mode_enabled) serverBuilder.intercept(DebugServerInterceptor(this))
        serverBuilder.intercept(ExceptionInterceptor(this))
        server = serverBuilder.build()
        server!!.start()
        info { "Plugin ready: gRPC server waiting on $host:$port" }
    }

    override fun onDisable() {
        super.onDisable()
        info { "Plugin is shutting down..." }
        server?.shutdown()
        server = null
        info { "Plugin stopped" }
    }
}
