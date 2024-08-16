package org.mcpq.main

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import protocol.MinecraftOuterClass
import protocol.MinecraftOuterClass.Event
import protocol.MinecraftOuterClass.EventStreamRequest
import java.util.concurrent.LinkedBlockingQueue


open class EventListener : Listener {
    val outQueue = LinkedBlockingQueue<Event>(100)

    companion object {
        fun createFrom(request: EventStreamRequest, plugin: MCPQPlugin): EventListener? {
            return when (request.eventType) {
                MinecraftOuterClass.EventType.EVENT_PLAYER_JOIN -> object : EventListener() {
                    @EventHandler(ignoreCancelled=true)
                    fun onPlayerJoin(event: PlayerJoinEvent) {
                        outQueue.offer(Event.newBuilder()
                            .setType(MinecraftOuterClass.EventType.EVENT_PLAYER_JOIN)
                            .setPlayerMsg(Event.PlayerAndMessage.newBuilder()
                                .setTrigger(MinecraftOuterClass.Player.newBuilder()
                                    .setName(event.player.name)
                                    .build())
                                .build())
                            .build()
                        ).not().then {
                            plugin.logger.warning { "onPlayerJoin: event dropped" }
                        }
                    }
                }
                MinecraftOuterClass.EventType.EVENT_PLAYER_LEAVE -> object : EventListener() {
                    @EventHandler(ignoreCancelled=true)
                    fun onPlayerLeave(event: PlayerQuitEvent) {
                        outQueue.offer(Event.newBuilder()
                            .setType(MinecraftOuterClass.EventType.EVENT_PLAYER_LEAVE)
                            .setPlayerMsg(Event.PlayerAndMessage.newBuilder()
                                .setTrigger(MinecraftOuterClass.Player.newBuilder()
                                    .setName(event.player.name)
                                    .build())
                                .build())
                            .build()
                        ).not().then {
                            plugin.logger.warning { "onPlayerLeave: event dropped" }
                        }
                    }
                }
                MinecraftOuterClass.EventType.EVENT_PLAYER_DEATH -> object : EventListener() {
                    @EventHandler(ignoreCancelled=true)
                    fun onPlayerDeath(event: PlayerDeathEvent) {
                        outQueue.offer(Event.newBuilder()
                            .setType(MinecraftOuterClass.EventType.EVENT_PLAYER_DEATH)
                            .setPlayerMsg(Event.PlayerAndMessage.newBuilder()
                                .setTrigger(MinecraftOuterClass.Player.newBuilder()
                                    .setName(event.entity.name)
                                    .build())
                                .setMessage(event.deathMessage)
                                .build())
                            .build()
                        ).not().then {
                            plugin.logger.warning { "onPlayerDeath: event dropped" }
                        }
                    }
                }
                MinecraftOuterClass.EventType.EVENT_CHAT_MESSAGE -> object : EventListener() {
                    @EventHandler(ignoreCancelled=true)
                    fun onChatMessage(event: AsyncPlayerChatEvent) {
                        outQueue.offer(Event.newBuilder()
                            .setType(MinecraftOuterClass.EventType.EVENT_CHAT_MESSAGE)
                            .setPlayerMsg(Event.PlayerAndMessage.newBuilder()
                                .setTrigger(MinecraftOuterClass.Player.newBuilder()
                                    .setName(event.player.name)
                                    .build())
                                .setMessage(event.message)
                                .build())
                            .build()
                        ).not().then {
                            plugin.logger.warning { "onChatMessage: event dropped" }
                        }
                    }
                }
                MinecraftOuterClass.EventType.EVENT_BLOCK_HIT -> object : EventListener() {
                    @EventHandler(ignoreCancelled=true)
                    fun onBlockHit(event: PlayerInteractEvent) {
                        // plugin.logger.info { "onBlockHit: fired: name ${event.player.name} id ${event.player.entityId}" }
                        // ignore clicks on air blocks and physical actions
                        if (event.action != Action.LEFT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_BLOCK) return
                        // ignore OFF_HAND events and physical events
                        if (event.hand == null || event.hand != EquipmentSlot.HAND) return
                        // ignore events where no block was clicked
                        val block = event.clickedBlock ?: return
                        outQueue.offer(Event.newBuilder()
                            .setType(MinecraftOuterClass.EventType.EVENT_BLOCK_HIT)
                            .setBlockHit(Event.BlockHit.newBuilder()
                                .setTrigger(MinecraftOuterClass.Player.newBuilder()
                                    .setName(event.player.name)
                                    .build())
                                .setRightHand(event.action == Action.RIGHT_CLICK_BLOCK)
                                .setItemType(if (event.hasItem()) event.material.name.lowercase() else "")
                                .setPos(MinecraftOuterClass.Vec3.newBuilder()
                                        .setX(block.x).setY(block.y).setZ(block.z)
                                        .build())
                                .setFace(event.blockFace.name.lowercase())
                                .build())
                            .build()
                        ).not().then {
                            plugin.logger.warning { "onBlockHit: event dropped" }
                        }
                    }
                }
                MinecraftOuterClass.EventType.EVENT_PROJECTILE_HIT -> object : EventListener() {
                    @EventHandler(ignoreCancelled=true)
                    fun onProjectileHit(event: ProjectileHitEvent) {
                        val shooter = event.entity.shooter
                        if (shooter == null || shooter !is Player) {
                            // plugin.logger.info { "onProjectileHit: not shot by player, dropping}" }
                            return
                        }
                        // plugin.logger.info { "onProjectileHit: fired by ${shooter.name}" }
                        val projBuilder = Event.ProjectileHit.newBuilder()
                            .setTrigger(MinecraftOuterClass.Player.newBuilder()
                                .setName(shooter.name)
                                .build())
                            .setProjectile(event.entity.type.name.lowercase())

                        val targetEntity = event.hitEntity
                        val targetBlock = event.hitBlock
                        val targetBlockFace = event.hitBlockFace
                        if (targetEntity != null) {
                            if (targetBlock != null) {
                                plugin.logger.warning { "onProjectileHit: hit entity AND block at once" }
                            }
                            // TODO: use float values of precise hit?
                            projBuilder.pos = MinecraftOuterClass.Vec3.newBuilder()
                                .setX(targetEntity.location.blockX)
                                .setY(targetEntity.location.blockY)
                                .setZ(targetEntity.location.blockZ)
                                .build()
                            if (targetEntity is Player) {
                                projBuilder.player = MinecraftOuterClass.Player.newBuilder()
                                    .setName(targetEntity.name)
                                    .build()
                            } else {
                                projBuilder.entity = MinecraftOuterClass.Entity.newBuilder()
                                    .setId(targetEntity.uniqueId.toString())
                                    .setType(targetEntity.name.lowercase())
                                    .build()
                            }
                        } else if (targetBlock != null) {
                            projBuilder.setBlock(targetBlock.type.name.lowercase())
                                .setPos(MinecraftOuterClass.Vec3.newBuilder()
                                    .setX(targetBlock.x)
                                    .setY(targetBlock.y)
                                    .setZ(targetBlock.z)
                                    .build())
                            if (targetBlockFace != null) {
                                projBuilder.face = targetBlockFace.name.lowercase()
                            } else {
                                plugin.logger.warning { "onProjectileHit: block was hit but not face?" }
                            }
                        }

                        outQueue.offer(Event.newBuilder()
                            .setType(MinecraftOuterClass.EventType.EVENT_PROJECTILE_HIT)
                            .setProjectileHit(projBuilder.build())
                            .build()
                        ).not().then {
                            plugin.logger.warning { "onProjectileHit: event dropped" }
                        }
                    }
                }

                else -> { null }
            }
        }
    }
}
