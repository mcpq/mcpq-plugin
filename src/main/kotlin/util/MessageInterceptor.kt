package org.mcpq.main.util

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.conversations.Conversation
import org.bukkit.conversations.ConversationAbandonedEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.plugin.Plugin
import org.mcpq.main.MCPQPlugin
import java.util.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


// adapted from:
// https://www.spigotmc.org/threads/how-do-i-get-the-output-of-dispatchcommand-command-when-called-by-callsyncmethod.354521/
class MessageInterceptor(val wrappedSender: ConsoleCommandSender, val plugin: MCPQPlugin) : ConsoleCommandSender {
    private val msgLog = StringBuilder()
    private val spigotWrapper = Spigot()

    fun getMessageLog(): String {
        return msgLog.toString()
    }

    fun getMessageLogStripColor(): String {
        return ChatColor.stripColor(msgLog.toString())
    }

    fun clearMessageLog() {
        msgLog.setLength(0)
    }

    inner class ServerInterceptor(private val server: Server) : Server by server {

        override fun broadcast(p0: String, p1: String): Int {
            plugin.debug { "ServerInterceptor.broadcast($p0, $p1)" }
            return server.broadcast(p0, p1)
        }

        override fun broadcastMessage(p0: String): Int {
            plugin.debug { "ServerInterceptor.broadcastMessage($p0)" }
            return server.broadcastMessage(p0)
        }

        override fun spigot(): Server.Spigot {
            plugin.debug { "ServerInterceptor.spigot()" }
            return server.spigot()
        }

        override fun getConsoleSender(): ConsoleCommandSender {
            plugin.debug { "ServerInterceptor.getConsoleSender()" }
            return this@MessageInterceptor
        }

        override fun dispatchCommand(p0: CommandSender, p1: String): Boolean {
            plugin.debug { "ServerInterceptor.dispatchCommand($p0, $p1)" }
            return server.dispatchCommand(p0, p1)
        }
    }

    inner class Spigot : CommandSender.Spigot() {
        override fun sendMessage(component: BaseComponent) {
            plugin.debug { "Spigot.sendMessage(component=$component)" }
            msgLog.append(BaseComponent.toLegacyText(component)).append('\n')
            wrappedSender.spigot().sendMessage(component)
        }

        override fun sendMessage(vararg components: BaseComponent?) {
            plugin.debug { "Spigot.sendMessage(components=$components)" }
            msgLog.append(BaseComponent.toLegacyText(*components)).append('\n')
            wrappedSender.spigot().sendMessage(*components)
        }

        override fun sendMessage(sender: UUID?, component: BaseComponent) {
            plugin.debug { "Spigot.sendMessage(sender=$sender, component=$component)" }
            msgLog.append(BaseComponent.toLegacyText(component)).append('\n')
            wrappedSender.spigot().sendMessage(sender, component)
        }

        override fun sendMessage(sender: UUID?, vararg components: BaseComponent?) {
            plugin.debug { "Spigot.sendMessage(sender=$sender, components=$components)" }
            msgLog.append(BaseComponent.toLegacyText(*components)).append('\n')
            wrappedSender.spigot().sendMessage(sender, *components)
        }
    }

    // ----------------------------
    // important overwrites

    override fun getServer(): Server {
        plugin.debug { "getServer()" }
        return wrappedSender.getServer()
        // TODO: Cannot cast to CraftServer!
        // plugin.debug { "getServer()  <-- Proxied!" }
        // return ServerInterceptor(wrappedSender.server)
    }

    override fun spigot(): CommandSender.Spigot {
        plugin.debug { "spigot()" }
        return spigotWrapper
    }

    // ----------------------------
    // message collection
    // TODO: these functions (sendMessage...) are only used by Bukkit commands, not by vanilla!
    // TODO: Would require CraftServer and CommandSourceStack to overwrite??

    override fun sendMessage(p0: String) {
        plugin.debug { "sendMessage($p0)" }
        msgLog.append(p0).append('\n')
        return wrappedSender.sendMessage(p0)
    }

    override fun sendMessage(vararg p0: String?) {
        plugin.debug { "sendMessage($p0)" }
        for (p in p0) {
            msgLog.append(p).append('\n')
        }
        return wrappedSender.sendMessage(*p0)
    }

    override fun sendMessage(p0: UUID?, p1: String) {
        plugin.debug { "sendMessage($p0, $p1)" }
        msgLog.append(p1).append('\n')
        return wrappedSender.sendMessage(p0, p1)
    }

    override fun sendMessage(p0: UUID?, vararg p1: String?) {
        plugin.debug { "sendMessage($p0, $p1)" }
        for (p in p1) {
            msgLog.append(p).append('\n')
        }
        return wrappedSender.sendMessage(p0, *p1)
    }

    override fun sendRawMessage(p0: String) {
        plugin.debug { "sendRawMessage($p0)" }
        msgLog.append(p0).append('\n')
        return wrappedSender.sendRawMessage(p0)
    }

    override fun sendRawMessage(p0: UUID?, p1: String) {
        plugin.debug { "sendRawMessage($p0, $p1)" }
        msgLog.append(p1).append('\n')
        return wrappedSender.sendRawMessage(p0, p1)
    }

    // ----------------------------
    // others

    override fun isOp(): Boolean {
        plugin.debug { "isOp()" }
        return wrappedSender.isOp()
    }

    override fun setOp(p0: Boolean) {
        plugin.debug { "setOp($p0)" }
        return wrappedSender.setOp(p0)
    }

    override fun isPermissionSet(p0: String): Boolean {
        plugin.debug { "isPermissionSet($p0)" }
        return wrappedSender.isPermissionSet(p0)
    }

    override fun isPermissionSet(p0: Permission): Boolean {
        plugin.debug { "isPermissionSet($p0)" }
        return wrappedSender.isPermissionSet(p0)
    }

    override fun hasPermission(p0: String): Boolean {
        plugin.debug { "hasPermission($p0)" }
        return wrappedSender.hasPermission(p0)
    }

    override fun hasPermission(p0: Permission): Boolean {
        plugin.debug { "hasPermission($p0)" }
        return wrappedSender.hasPermission(p0)
    }

    override fun addAttachment(p0: Plugin, p1: String, p2: Boolean): PermissionAttachment {
        plugin.debug { "addAttachment($p0, $p1, $p2)" }
        return wrappedSender.addAttachment(p0, p1, p2)
    }

    override fun addAttachment(p0: Plugin): PermissionAttachment {
        plugin.debug { "addAttachment($p0)" }
        return wrappedSender.addAttachment(p0)
    }

    override fun addAttachment(p0: Plugin, p1: String, p2: Boolean, p3: Int): PermissionAttachment? {
        plugin.debug { "addAttachment($p0, $p1, $p2, $p3)" }
        return wrappedSender.addAttachment(p0, p1, p2, p3)
    }

    override fun addAttachment(p0: Plugin, p1: Int): PermissionAttachment? {
        plugin.debug { "addAttachment($p0, $p1)" }
        return wrappedSender.addAttachment(p0, p1)
    }

    override fun removeAttachment(p0: PermissionAttachment) {
        plugin.debug { "removeAttachment($p0" }
        return wrappedSender.removeAttachment(p0)
    }

    override fun recalculatePermissions() {
        plugin.debug { "recalculatePermissions()" }
        return wrappedSender.recalculatePermissions()
    }

    override fun getEffectivePermissions(): MutableSet<PermissionAttachmentInfo> {
        plugin.debug { "getEffectivePermissions()" }
        return wrappedSender.getEffectivePermissions()
    }

    override fun getName(): String {
        plugin.debug { "getName()" }
        return "OrderFulfiller"
    }

    override fun isConversing(): Boolean {
        plugin.debug { "isConversing()" }
        return wrappedSender.isConversing()
    }

    override fun acceptConversationInput(p0: String) {
        plugin.debug { "acceptConversationInput($p0)" }
        return wrappedSender.acceptConversationInput(p0)
    }

    override fun beginConversation(p0: Conversation): Boolean {
        plugin.debug { "beginConversation($p0)" }
        return wrappedSender.beginConversation(p0)
    }

    override fun abandonConversation(p0: Conversation) {
        plugin.debug { "abandonConversation($p0)" }
        return wrappedSender.abandonConversation(p0)
    }

    override fun abandonConversation(p0: Conversation, p1: ConversationAbandonedEvent) {
        plugin.debug { "abandonConversation($p0, $p1)" }
        return wrappedSender.abandonConversation(p0, p1)
    }
}