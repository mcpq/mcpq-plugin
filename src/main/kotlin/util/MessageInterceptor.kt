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
import java.util.*
import java.util.logging.Logger
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


// TODO: remove logger.info
// adapted from:
// https://www.spigotmc.org/threads/how-do-i-get-the-output-of-dispatchcommand-command-when-called-by-callsyncmethod.354521/
class MessageInterceptor(val wrappedSender: ConsoleCommandSender, val logger: Logger) : ConsoleCommandSender {
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
            logger.info("ServerInterceptor.broadcast($p0, $p1)")
            return server.broadcast(p0, p1)
        }

        override fun broadcastMessage(p0: String): Int {
            logger.info("ServerInterceptor.broadcastMessage($p0)")
            return server.broadcastMessage(p0)
        }

        override fun spigot(): Server.Spigot {
            logger.info("ServerInterceptor.spigot()")
            return server.spigot()
        }

        override fun getConsoleSender(): ConsoleCommandSender {
            logger.info("ServerInterceptor.getConsoleSender()")
            return this@MessageInterceptor
        }

        override fun dispatchCommand(p0: CommandSender, p1: String): Boolean {
            logger.info("ServerInterceptor.dispatchCommand($p0, $p1)")
            return server.dispatchCommand(p0, p1)
        }
    }

    inner class Spigot : CommandSender.Spigot() {
        override fun sendMessage(component: BaseComponent) {
            logger.info("Spigot.sendMessage(component=$component)")
            msgLog.append(BaseComponent.toLegacyText(component)).append('\n')
            wrappedSender.spigot().sendMessage(component)
        }

        override fun sendMessage(vararg components: BaseComponent?) {
            logger.info("Spigot.sendMessage(components=$components)")
            msgLog.append(BaseComponent.toLegacyText(*components)).append('\n')
            wrappedSender.spigot().sendMessage(*components)
        }

        override fun sendMessage(sender: UUID?, component: BaseComponent) {
            logger.info("Spigot.sendMessage(sender=$sender, component=$component)")
            msgLog.append(BaseComponent.toLegacyText(component)).append('\n')
            wrappedSender.spigot().sendMessage(sender, component)
        }

        override fun sendMessage(sender: UUID?, vararg components: BaseComponent?) {
            logger.info("Spigot.sendMessage(sender=$sender, components=$components)")
            msgLog.append(BaseComponent.toLegacyText(*components)).append('\n')
            wrappedSender.spigot().sendMessage(sender, *components)
        }
    }

    // ----------------------------
    // important overwrites

    override fun getServer(): Server {
        logger.info("getServer()")
        return wrappedSender.getServer()
        // TODO: Cannot cast to CraftServer!
        // logger.info("getServer()  <-- Proxied!")
        // return ServerInterceptor(wrappedSender.server)
    }

    override fun spigot(): CommandSender.Spigot {
        logger.info("spigot()")
        return spigotWrapper
    }

    // ----------------------------
    // message collection
    // TODO: these functions (sendMessage...) are only used by Bukkit commands, not by vanilla!
    // TODO: Would require CraftServer and CommandSourceStack to overwrite??

    override fun sendMessage(p0: String) {
        logger.info("sendMessage($p0)")
        msgLog.append(p0).append('\n')
        return wrappedSender.sendMessage(p0)
    }

    override fun sendMessage(vararg p0: String?) {
        logger.info("sendMessage($p0)")
        for (p in p0) {
            msgLog.append(p).append('\n')
        }
        return wrappedSender.sendMessage(*p0)
    }

    override fun sendMessage(p0: UUID?, p1: String) {
        logger.info("sendMessage($p0, $p1)")
        msgLog.append(p1).append('\n')
        return wrappedSender.sendMessage(p0, p1)
    }

    override fun sendMessage(p0: UUID?, vararg p1: String?) {
        logger.info("sendMessage($p0, $p1)")
        for (p in p1) {
            msgLog.append(p).append('\n')
        }
        return wrappedSender.sendMessage(p0, *p1)
    }

    override fun sendRawMessage(p0: String) {
        logger.info("sendRawMessage($p0)")
        msgLog.append(p0).append('\n')
        return wrappedSender.sendRawMessage(p0)
    }

    override fun sendRawMessage(p0: UUID?, p1: String) {
        logger.info("sendRawMessage($p0, $p1)")
        msgLog.append(p1).append('\n')
        return wrappedSender.sendRawMessage(p0, p1)
    }

    // ----------------------------
    // others

    override fun isOp(): Boolean {
        logger.info("isOp()")
        return wrappedSender.isOp()
    }

    override fun setOp(p0: Boolean) {
        logger.info("setOp($p0)")
        return wrappedSender.setOp(p0)
    }

    override fun isPermissionSet(p0: String): Boolean {
        logger.info("isPermissionSet($p0)")
        return wrappedSender.isPermissionSet(p0)
    }

    override fun isPermissionSet(p0: Permission): Boolean {
        logger.info("isPermissionSet($p0)")
        return wrappedSender.isPermissionSet(p0)
    }

    override fun hasPermission(p0: String): Boolean {
        logger.info("hasPermission($p0)")
        return wrappedSender.hasPermission(p0)
    }

    override fun hasPermission(p0: Permission): Boolean {
        logger.info("hasPermission($p0)")
        return wrappedSender.hasPermission(p0)
    }

    override fun addAttachment(p0: Plugin, p1: String, p2: Boolean): PermissionAttachment {
        logger.info("addAttachment($p0, $p1, $p2)")
        return wrappedSender.addAttachment(p0, p1, p2)
    }

    override fun addAttachment(p0: Plugin): PermissionAttachment {
        logger.info("addAttachment($p0)")
        return wrappedSender.addAttachment(p0)
    }

    override fun addAttachment(p0: Plugin, p1: String, p2: Boolean, p3: Int): PermissionAttachment? {
        logger.info("addAttachment($p0, $p1, $p2, $p3)")
        return wrappedSender.addAttachment(p0, p1, p2, p3)
    }

    override fun addAttachment(p0: Plugin, p1: Int): PermissionAttachment? {
        logger.info("addAttachment($p0, $p1)")
        return wrappedSender.addAttachment(p0, p1)
    }

    override fun removeAttachment(p0: PermissionAttachment) {
        logger.info("removeAttachment($p0")
        return wrappedSender.removeAttachment(p0)
    }

    override fun recalculatePermissions() {
        logger.info("recalculatePermissions()")
        return wrappedSender.recalculatePermissions()
    }

    override fun getEffectivePermissions(): MutableSet<PermissionAttachmentInfo> {
        logger.info("getEffectivePermissions()")
        return wrappedSender.getEffectivePermissions()
    }

    override fun getName(): String {
        logger.info("getName()")
        return "OrderFulfiller"
    }

    override fun isConversing(): Boolean {
        logger.info("isConversing()")
        return wrappedSender.isConversing()
    }

    override fun acceptConversationInput(p0: String) {
        logger.info("acceptConversationInput($p0)")
        return wrappedSender.acceptConversationInput(p0)
    }

    override fun beginConversation(p0: Conversation): Boolean {
        logger.info("beginConversation($p0)")
        return wrappedSender.beginConversation(p0)
    }

    override fun abandonConversation(p0: Conversation) {
        logger.info("abandonConversation($p0)")
        return wrappedSender.abandonConversation(p0)
    }

    override fun abandonConversation(p0: Conversation, p1: ConversationAbandonedEvent) {
        logger.info("abandonConversation($p0, $p1)")
        return wrappedSender.abandonConversation(p0, p1)
    }
}