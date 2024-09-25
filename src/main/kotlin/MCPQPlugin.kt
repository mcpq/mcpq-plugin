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
    var debug = false

    companion object {
        // everything in here is static
        val logger : Logger = Bukkit.getLogger()
    }

    override fun onEnable() {
        super.onEnable()
        logger.info { "Plugin is starting..." }
        saveDefaultConfig() // save a copy of the default config.yml if one does not exist

        debug = config.getBoolean("debug")
        val port = config.getInt("port")
        val host = config.getString("host")

        if (debug) { logger.warning { "Plugin in DEBUG mode!" } }

        logger.info("Try binding to $host:$port")
        val address = InetSocketAddress(host, port)
        val serverBuilder = NettyServerBuilder
            .forAddress(address)
            .addService(service)

        if (debug) serverBuilder.intercept(DebugServerInterceptor())
        serverBuilder.intercept(ExceptionInterceptor())
        server = serverBuilder.build()
        server!!.start()
        logger.info { "Plugin ready: gRPC server waiting on $host:$port" }
    }

    override fun onDisable() {
        super.onDisable()
        logger.info { "Plugin is shutting down..." }
        server?.shutdown()
        server = null
        logger.info { "Plugin stopped" }
    }
}
