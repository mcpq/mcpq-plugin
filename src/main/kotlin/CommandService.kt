package org.mcpq.main

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.HandlerList
import org.mcpq.main.util.MessageInterceptor
import protocol.MinecraftGrpcKt
import protocol.MinecraftOuterClass
import protocol.MinecraftOuterClass.*
import java.lang.Thread.sleep
import java.time.Instant
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


inline fun Boolean.then(block: () -> Unit) {
    if (this) {
        block()
    }
}


class CommandService(val plugin: MCPQPlugin) : MinecraftGrpcKt.MinecraftCoroutineImplBase() {
    val OK: Status = Status.newBuilder().setCode(StatusCode.OK).build()

    inline fun mcrun(crossinline block: () -> Unit) = Bukkit.getScheduler()
        .runTask(plugin, kotlinx.coroutines.Runnable {block()})

    fun <T : Any> mcrun_blocking(block: () -> T): T {
        val future = Bukkit.getScheduler().callSyncMethod(plugin, block)
        return future.get()
    }

    override suspend fun getServerInfo(request: ServerInfoRequest): ServerInfoResponse {
        return ServerInfoResponse.newBuilder()
            .setMcVersion(Bukkit.getServer().version)
            .setMcpqVersion(plugin.description.version)
            .build()
    }

    override suspend fun getMaterials(request: MaterialRequest): MaterialResponse {
        val response = MaterialResponse.newBuilder()
        val materials = Material.values()
        if (request.onlyKeys) {
            materials.forEach {
                response.addMaterials(MaterialResponse.Material.newBuilder().setKey(it.key.toString()).build())
            }
        } else {
            materials.forEach {
                response.addMaterials(MaterialResponse.Material.newBuilder()
                    .setKey(it.key.toString())
                    .setIsAir(it.isAir)
                    .setIsBlock(it.isBlock)
                    .setIsBurnable(it.isBurnable)
                    .setIsEdible(it.isEdible)
                    .setIsFlammable(it.isFlammable)
                    .setIsFuel(it.isFuel)
                    .setIsInteractable(it.isInteractable)
                    .setIsItem(it.isItem)
                    .setIsOccluding(it.isOccluding)
                    .setIsSolid(it.isSolid)
                    .setHasGravity(it.hasGravity())
                    .build()
                )
            }
        }
        return response.setStatus(OK).build()
    }

    override suspend fun getEntityTypes(request: EntityTypeRequest): EntityTypeResponse {
        val response = EntityTypeResponse.newBuilder()
        val entityTypes = EntityType.values().filter { it != EntityType.UNKNOWN }
        if (request.onlyKeys) {
            entityTypes.forEach {
                response.addTypes(EntityTypeResponse.EntityType.newBuilder().setKey(it.key.toString()).build())
            }
        } else {
            entityTypes.forEach {
                response.addTypes(EntityTypeResponse.EntityType.newBuilder()
                    .setKey(it.key.toString())
                    .setIsSpawnable(it.isSpawnable)
                    .build()
                )
            }
        }
        return response.setStatus(OK).build()
    }

    override suspend fun runCommand(request: CommandRequest): Status {
        return runCommandWithOptions(request).status
    }

    override suspend fun runCommandWithOptions(request: CommandRequest): CommandResponse {
        val console = Bukkit.getConsoleSender()
        // checkout link, may require plugin?
        // https://www.spigotmc.org/threads/how-do-i-get-the-output-of-dispatchcommand-command-when-called-by-callsyncmethod.354521/
        if (request.output) { // also blocking
            // TODO: can only capture Bukkit command output, not from vanilla!
            val interceptor = MessageInterceptor(console, plugin.logger)
            try {
                val value = mcrun_blocking {
                    interceptor.server.dispatchCommand(interceptor, request.command)
                }
                // plugin.logger.info(interceptor.getMessageLog())
                return CommandResponse.newBuilder()
                    .setStatus(Status.newBuilder().setCode(StatusCode.OK).setExtra(value.toString()).build())
                    .setOutput(interceptor.getMessageLogStripColor())
                    .build()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        } else if (request.blocking) {
            mcrun_blocking {
                Bukkit.dispatchCommand(console, request.command)
            }
            return CommandResponse.newBuilder().setStatus(OK).build()
        } else {
            mcrun {
                Bukkit.dispatchCommand(console, request.command)
            }
            return CommandResponse.newBuilder().setStatus(OK).build()
        }
    }

    override suspend fun postToChat(request: ChatPostRequest): Status {
        if (request.hasPlayer()) {
            val player = Bukkit.getPlayer(request.player.name)
                ?: return Status.newBuilder().setCode(StatusCode.PLAYER_NOT_FOUND).setExtra(request.player.name).build()
            player.sendMessage(request.message)
            return OK
        } else {
            val numPlayers = Bukkit.broadcastMessage(request.message)
            return Status.newBuilder()
                .setCode(StatusCode.OK)
                .setExtra("$numPlayers") // message was sent to X players
                .build()
        }
    }

    override suspend fun accessWorlds(request: WorldRequest): WorldResponse {
        // world.name == folder name (e.g. world, world_nether, world_the_end)
        // world.key == namespace name (e.g. minecraft:overworld, minecraft:the_nether, minecraft:the_end)
        // world.uid == game object id (e.g. f4c3968c-99d4-46ff-9bfa-bb45ec6a17ce, ...)
        if (request.worldsCount == 0)
            return WorldResponse.newBuilder()
                .setStatus(OK)
                .addAllWorlds(Bukkit.getWorlds().map {
                    World.newBuilder()
                    .setName(it.name)
                        .setInfo(WorldInfo.newBuilder()
                            .setKey(it.key.toString())
                            .setPvp(it.pvp)
                            .build()
                        )
                    .build()
                })
                .build()

        val response = WorldResponse.newBuilder().setStatus(OK)
        request.worldsList.forEach { reqWorld ->
            val world = Bukkit.getWorld(reqWorld.name)
                ?: return WorldResponse.newBuilder()
                .setStatus(
                    Status.newBuilder()
                        .setCode(StatusCode.WORLD_NOT_FOUND)
                        .setExtra(reqWorld.name)
                        .build()
                ).build()
            reqWorld.hasInfo().then {
                // cannot set key so ignore
                world.pvp = reqWorld.info.pvp
            }
            response.addWorlds(World.newBuilder()
                .setName(world.name)
                .build()
            )
        }
        return response.build()
    }

    override suspend fun getHeight(request: HeightRequest): HeightResponse {
        val world = (if (request.hasWorld()) Bukkit.getWorld(request.world.name) else Bukkit.getWorlds().firstOrNull())
            ?: return HeightResponse.newBuilder().setStatus(Status.newBuilder()
                .setCode(StatusCode.WORLD_NOT_FOUND)
                .setExtra(if (request.hasWorld()) request.world.name else "Bukkit.getWorlds().first()")
                .build()).build()
        val block = world.getHighestBlockAt(request.x, request.z)
        return HeightResponse.newBuilder()
            .setStatus(OK)
            .setBlock(Block.newBuilder()
                .setInfo(BlockInfo.newBuilder().setBlockType(block.type.name.lowercase()).build())
                .setWorld(World.newBuilder().setName(world.name).build())
                .setPos(Vec3.newBuilder().setX(block.x).setY(block.y).setZ(block.z).build()))
            .build()
    }

    override suspend fun getBlock(request: BlockRequest): BlockResponse {
        val world = (if (request.hasWorld()) Bukkit.getWorld(request.world.name) else Bukkit.getWorlds().firstOrNull())
            ?: return BlockResponse.newBuilder().setStatus(Status.newBuilder()
                .setCode(StatusCode.WORLD_NOT_FOUND)
                .setExtra(if (request.hasWorld()) request.world.name else "Bukkit.getWorlds().first()")
                .build()).build()
        val block = world.getBlockAt(request.pos.x, request.pos.y, request.pos.z)
        request.withData.then {
            return BlockResponse.newBuilder()
                .setStatus(Status.newBuilder()
                    .setCode(StatusCode.NOT_IMPLEMENTED)
                    .setExtra("getBlock(withData=True)")
                    .build())
                .setInfo(BlockInfo.newBuilder()
                    .setBlockType(block.type.name.lowercase())
                    .build())
                .build()
        }
        return BlockResponse.newBuilder()
            .setStatus(OK)
            .setInfo(BlockInfo.newBuilder()
                .setBlockType(block.type.name.lowercase())
                .build())
            .build()
    }

    override suspend fun setBlock(request: Block): Status {
        request.hasInfo().not().then {
            return Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT).setExtra("Block.info").build()
        }
        val material = Material.matchMaterial(request.info.blockType)
            ?: return Status.newBuilder()
                .setCode(StatusCode.BLOCK_TYPE_NOT_FOUND)
                .setExtra(request.info.blockType)
                .build()
        request.info.hasNbt().then {
            return Status.newBuilder()
                .setCode(StatusCode.NOT_IMPLEMENTED)
                .setExtra("Block.info.nbt")
                .build()
        }
        val world = (if (request.hasWorld()) Bukkit.getWorld(request.world.name) else Bukkit.getWorlds().firstOrNull())
            ?: return Status.newBuilder()
                .setCode(StatusCode.WORLD_NOT_FOUND)
                .setExtra(if (request.hasWorld()) request.world.name else "Bukkit.getWorlds().first()")
                .build()
        val block = world.getBlockAt(request.pos.x, request.pos.y, request.pos.z)
        mcrun {
            block.type = material
        }
        return OK
    }

    override suspend fun setBlocks(request: Blocks): Status {
        request.hasInfo().not().then {
            return Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT).setExtra("Blocks.info").build()
        }
        val material = Material.matchMaterial(request.info.blockType)
            ?: return Status.newBuilder()
                .setCode(StatusCode.BLOCK_TYPE_NOT_FOUND)
                .setExtra(request.info.blockType)
                .build()
        request.info.hasNbt().then {
            return Status.newBuilder()
                .setCode(StatusCode.NOT_IMPLEMENTED)
                .setExtra("Blocks.info.nbt")
                .build()
        }
        val world = (if (request.hasWorld()) Bukkit.getWorld(request.world.name) else Bukkit.getWorlds().firstOrNull())
            ?: return Status.newBuilder()
                .setCode(StatusCode.WORLD_NOT_FOUND)
                .setExtra(if (request.hasWorld()) request.world.name else "Bukkit.getWorlds().first()")
                .build()
        request.posList.isEmpty().then {
            return Status.newBuilder()
                .setCode(StatusCode.MISSING_ARGUMENT)
                .setExtra("Blocks.pos")
                .build()
        }
        mcrun {
            for (pos in request.posList) {
                world.getBlockAt(pos.x, pos.y, pos.z).type = material
            }
        }
        return OK
    }

    override suspend fun setBlockCube(request: Blocks): Status {
        request.hasInfo().not().then {
            return Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT).setExtra("Blocks.info").build()
        }
        val material = Material.matchMaterial(request.info.blockType)
            ?: return Status.newBuilder()
                .setCode(StatusCode.BLOCK_TYPE_NOT_FOUND)
                .setExtra(request.info.blockType)
                .build()
        request.info.hasNbt().then {
            return Status.newBuilder()
                .setCode(StatusCode.NOT_IMPLEMENTED)
                .setExtra("Blocks.info.nbt")
                .build()
        }
        val world = (if (request.hasWorld()) Bukkit.getWorld(request.world.name) else Bukkit.getWorlds().firstOrNull())
            ?: return Status.newBuilder()
                .setCode(StatusCode.WORLD_NOT_FOUND)
                .setExtra(if (request.hasWorld()) request.world.name else "Bukkit.getWorlds().first()")
                .build()
        request.posList.isEmpty().then {
            return Status.newBuilder()
                .setCode(StatusCode.MISSING_ARGUMENT)
                .setExtra("Blocks.pos")
                .build()
        }
        if (request.posList.size != 2) {
            return Status.newBuilder()
                .setCode(StatusCode.INVALID_ARGUMENT)
                .setExtra("Blocks.pos")
                .build()
        }
        val pos1 = request.posList[0]
        val pos2 = request.posList[1]
        val minX = if (pos1.x < pos2.x) pos1.x else pos2.x
        val maxX = if (pos1.x >= pos2.x) pos1.x else pos2.x
        val minY = if (pos1.y < pos2.y) pos1.y else pos2.y
        val maxY = if (pos1.y >= pos2.y) pos1.y else pos2.y
        val minZ = if (pos1.z < pos2.z) pos1.z else pos2.z
        val maxZ = if (pos1.z >= pos2.z) pos1.z else pos2.z
        mcrun {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        world.getBlockAt(x, y, z).type = material
                    }
                }
            }
        }
        return OK
    }

    fun _transformPlayer(player: org.bukkit.entity.Player): Player {
        val loc = EntityLocation.newBuilder()
            .setPos(Vec3f.newBuilder()
                .setX(player.location.x.toFloat())
                .setY(player.location.y.toFloat())
                .setZ(player.location.z.toFloat())
                .build())
            .setOrientation(EntityOrientation.newBuilder()
                .setYaw(player.location.yaw)
                .setPitch(player.location.pitch)
                .build())
            .setWorld(World.newBuilder()
                .setName(player.world.name)
                .build())
            .build()
        return Player.newBuilder()
            .setName(player.name)
            .setLocation(loc)
            .build()
    }


    override suspend fun getPlayers(request: PlayerRequest): PlayerResponse {
        // if list is empty return all online players
        request.namesList.isEmpty().then {
            request.withLocations.then {
                return PlayerResponse.newBuilder()
                    .setStatus(OK)
                    .addAllPlayers(
                        Bukkit.getOnlinePlayers().map { _transformPlayer(it) }
                    ).build()
            }
            return PlayerResponse.newBuilder()
                .setStatus(OK)
                .addAllPlayers(
                    Bukkit.getOnlinePlayers().map { Player.newBuilder().setName(it.name).build() }
                ).build()
        }

        // else we search for (only) specific players
        val players: MutableList<org.bukkit.entity.Player> = mutableListOf()
        request.namesList.forEach {
            players.add(Bukkit.getPlayer(it)
                ?: return PlayerResponse.newBuilder()
                    .setStatus(Status.newBuilder()
                        .setCode(StatusCode.PLAYER_NOT_FOUND)
                        .setExtra(it)
                        .build())
                    .build())
        }
        request.withLocations.then {
            return PlayerResponse.newBuilder()
                .setStatus(OK)
                .addAllPlayers(players.map { _transformPlayer(it) })
                .build()
        }
        return PlayerResponse.newBuilder()
            .setStatus(OK)
            .addAllPlayers(players.map { Player.newBuilder().setName(it.name).build() })
            .build()
    }

    override suspend fun setPlayer(request: Player): Status {
        val player = Bukkit.getPlayer(request.name)
            ?: return Status.newBuilder()
                .setCode(StatusCode.PLAYER_NOT_FOUND)
                .setExtra(request.name)
                .build()
        val new_location = if (request.hasLocation()) {
            val loc = request.location
            val world = (if (loc.hasWorld()) Bukkit.getWorld(loc.world.name) else player.location.world)
                ?: return Status.newBuilder()
                    .setCode(StatusCode.WORLD_NOT_FOUND)
                    .setExtra(if (loc.hasWorld()) loc.world.name else "Bukkit.getPlayer(${player.name}).location.world")
                    .build()
            val x = if (loc.hasPos()) loc.pos.x.toDouble() else player.location.x
            val y = if (loc.hasPos()) loc.pos.y.toDouble() else player.location.y
            val z = if (loc.hasPos()) loc.pos.z.toDouble() else player.location.z
            val yaw = if (loc.hasOrientation()) loc.orientation.yaw else player.location.yaw
            val pitch = if (loc.hasOrientation()) loc.orientation.pitch else player.location.pitch
            Location(world, x, y, z, yaw, pitch)
        } else null

        // throw error if nothing is changed by call, for now that means if location is not changed
        if (new_location == null) {
            return Status.newBuilder()
                .setCode(StatusCode.MISSING_ARGUMENT)
                .setExtra("Player.location")
                .build()
        }

        mcrun {
            new_location?.let { player.teleport(it) }
        }
        return OK
    }

    override suspend fun spawnEntity(request: Entity): SpawnedEntityResponse {
        if (request.id.isNotEmpty()) {
            return SpawnedEntityResponse.newBuilder()
                .setStatus(Status.newBuilder().setCode(StatusCode.INVALID_ARGUMENT).setExtra("Entity.id").build())
                .build()
        }
        if (request.type.isEmpty()) {
            return SpawnedEntityResponse.newBuilder()
                .setStatus(Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT).setExtra("Entity.type").build())
                .build()
        }
        if (request.hasLocation().not()) {
            return SpawnedEntityResponse.newBuilder()
                .setStatus(Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT).setExtra("Entity.location").build())
                .build()
        }
        if (request.location.hasPos().not()) {
            return SpawnedEntityResponse.newBuilder()
                .setStatus(Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT).setExtra("Entity.location.pos").build())
                .build()
        }
        val world = (if (request.location.hasWorld()) Bukkit.getWorld(request.location.world.name) else Bukkit.getWorlds().firstOrNull())
            ?: return SpawnedEntityResponse.newBuilder().setStatus(Status.newBuilder()
                .setCode(StatusCode.WORLD_NOT_FOUND)
                .setExtra(if (request.location.hasWorld()) request.location.world.name else "Bukkit.getWorlds().first()")
                .build()).build()
        val type = EntityType.fromName(request.type)
            ?: return SpawnedEntityResponse.newBuilder().setStatus(Status.newBuilder()
                .setCode(StatusCode.ENTITY_TYPE_NOT_FOUND)
                .setExtra(request.type)
                .build()).build()
        if (type.isSpawnable.not()) {
            return SpawnedEntityResponse.newBuilder().setStatus(Status.newBuilder()
                .setCode(StatusCode.ENTITY_NOT_SPAWNABLE)
                .setExtra(request.type)
                .build()).build()
        }
        val loc = Location(world, request.location.pos.x.toDouble(), request.location.pos.y.toDouble(), request.location.pos.z.toDouble())

        val entity = mcrun_blocking {
            world.spawnEntity(loc, type)
        }

        return SpawnedEntityResponse.newBuilder().setStatus(OK)
            .setEntity(Entity.newBuilder()
                .setId(entity.uniqueId.toString())
                .setType(entity.type.name.lowercase())
                // TODO: add loc and more
                .build())
            .build()
    }

    fun _get_id_or_null(id: String): UUID? {
        return try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override suspend fun setEntity(request: Entity): Status {
        if (request.id.isEmpty()) {
            return Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT).setExtra("Entity.id").build()
        }
        if (request.type.isEmpty().not()) {
            // TODO: changing entity type?
            return Status.newBuilder().setCode(StatusCode.NOT_IMPLEMENTED).setExtra("Entity.type").build()
        }
        // throw error if nothing is changed by call, for now that means if location is not changed
        if (request.hasLocation().not()) {
            return Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT).setExtra("Entity.location").build()
        }
        if (request.location.hasWorld().not() && request.location.hasPos().not() && request.location.hasOrientation().not()) {
            return Status.newBuilder().setCode(StatusCode.MISSING_ARGUMENT)
                .setExtra("Entity.location.[world/pos/orientation]").build()
        }

        val uniqueId: UUID = _get_id_or_null(request.id)
            ?: return Status.newBuilder()
                .setCode(StatusCode.INVALID_ARGUMENT)
                .setExtra("Entity.id '${request.id}'")
                .build()

        // TODO: might use non-blocking sometimes
//        val blocking = true
//        blocking.not().then {
//            mcrun {
//                val entity = Bukkit.getEntity(uniqueId)
//                    ?: return@mcrun
//                val new_location = if (request.hasLocation()) {
//                    val loc = request.location
//                    val world = (if (loc.hasWorld()) Bukkit.getWorld(loc.world.name) else entity.location.world)
//                        ?: return@mcrun
//                    val x = if (loc.hasPos()) loc.pos.x.toDouble() else entity.location.x
//                    val y = if (loc.hasPos()) loc.pos.y.toDouble() else entity.location.y
//                    val z = if (loc.hasPos()) loc.pos.z.toDouble() else entity.location.z
//                    val yaw = if (loc.hasOrientation()) loc.orientation.yaw else entity.location.yaw
//                    val pitch = if (loc.hasOrientation()) loc.orientation.pitch else entity.location.pitch
//                    Location(world, x, y, z, yaw, pitch)
//                } else null
//
//                new_location?.let { entity.teleport(it) }
//            }
//            return OK
//        }

        val response = mcrun_blocking {
            val entity = Bukkit.getEntity(uniqueId)
                ?: return@mcrun_blocking Status.newBuilder()
                    .setCode(StatusCode.ENTITY_NOT_FOUND)
                    .setExtra(uniqueId.toString())
                    .build()
            val new_location = if (request.hasLocation()) {
                val loc = request.location
                val world = (if (loc.hasWorld()) Bukkit.getWorld(loc.world.name) else entity.location.world)
                    ?: return@mcrun_blocking Status.newBuilder()
                        .setCode(StatusCode.WORLD_NOT_FOUND)
                        .setExtra(if (loc.hasWorld()) loc.world.name else "Bukkit.getEntity(${uniqueId}).location.world")
                        .build()
                val x = if (loc.hasPos()) loc.pos.x.toDouble() else entity.location.x
                val y = if (loc.hasPos()) loc.pos.y.toDouble() else entity.location.y
                val z = if (loc.hasPos()) loc.pos.z.toDouble() else entity.location.z
                val yaw = if (loc.hasOrientation()) loc.orientation.yaw else entity.location.yaw
                val pitch = if (loc.hasOrientation()) loc.orientation.pitch else entity.location.pitch
                Location(world, x, y, z, yaw, pitch)
            } else null

            new_location?.let { entity.teleport(it) }
            OK
        }
        return response
    }

    override suspend fun getEntities(request: EntityRequest): EntityResponse {
        var entities: Collection<org.bukkit.entity.Entity>
        if (request.hasSpecific()) {
            val part = request.specific
            if (part.entitiesList.isEmpty()) {
                return EntityResponse.newBuilder().setStatus(
                    Status.newBuilder()
                        .setCode(StatusCode.MISSING_ARGUMENT)
                        .setExtra("Request.specific.entitiesList")
                        .build()
                ).build()
            }
            val entityIds = part.entitiesList.map { entity ->
                _get_id_or_null(entity.id)
                    ?: return EntityResponse.newBuilder().setStatus(
                        Status.newBuilder()
                            .setCode(StatusCode.INVALID_ARGUMENT)
                            .setExtra("Request.specific.entitiesList[].id '${entity.id}'")
                            .build()
                    ).build()
            }

            entities = mcrun_blocking {
                entityIds.map { id -> Bukkit.getEntity(id) }.filterNotNull()
            }

        } else if (request.hasWorldwide()) {
            val part = request.worldwide
            val world = (if (part.hasWorld()) Bukkit.getWorld(part.world.name) else Bukkit.getWorlds().firstOrNull())
                ?: return EntityResponse.newBuilder().setStatus(
                    Status.newBuilder()
                        .setCode(StatusCode.WORLD_NOT_FOUND)
                        .setExtra(if (part.hasWorld()) part.world.name else "Bukkit.getWorlds().first()")
                        .build()).build()

            entities = mcrun_blocking {
                world.entities
            }

            if (part.includeNotSpawnable.not()) {
                // do NOT include Not-Spawnable == include only spawnable
                entities = entities.filter { entity -> entity.type.isSpawnable }
            }
            if (part.type.isNotEmpty()) {
                val type = EntityType.fromName(part.type)
                    ?: return EntityResponse.newBuilder().setStatus(Status.newBuilder()
                        .setCode(StatusCode.ENTITY_TYPE_NOT_FOUND)
                        .setExtra(part.type)
                        .build()).build()
                entities = entities.filter { entity -> entity.type == type }
            }
        } else {
            return EntityResponse.newBuilder().setStatus(Status.newBuilder()
                .setCode(StatusCode.MISSING_ARGUMENT)
                .setExtra("Request.[specific/worldwide]")
                .build()).build()
        }

        return EntityResponse.newBuilder().setStatus(OK)
            .addAllEntities(entities
                .map { entity ->
                    val builder = Entity.newBuilder()
                        .setId(entity.uniqueId.toString())
                        .setType(entity.type.name.lowercase())
                    if (request.withLocations) {
                        builder.setLocation(EntityLocation.newBuilder()
                            .setWorld(World.newBuilder().setName(entity.world.name).build()) // TODO: worldInfo?
                            .setPos(Vec3f.newBuilder()
                                .setX(entity.location.x.toFloat())
                                .setY(entity.location.y.toFloat())
                                .setZ(entity.location.z.toFloat())
                                .build())
                            .setOrientation(EntityOrientation.newBuilder()
                                .setYaw(entity.location.yaw)
                                .setPitch(entity.location.pitch)
                                .build()))
                    }
                    builder.build()
                })
            .build()
    }

    override fun getEventStream(request: EventStreamRequest): Flow<Event> {
        return flow {
            val creationTime = Instant.now().epochSecond
            val eventType = request.eventType.name
            val listener = EventListener.createFrom(request, plugin)
            if (listener == null) {
                // TODO: could also return some "ExceptionEvent" or similar
                plugin.logger.warning { "getEventStream(${eventType})[${creationTime}]: Event type not supported" }
            } else {
                plugin.logger.info { "getEventStream(${eventType})[${creationTime}]: Started..." }
                Bukkit.getPluginManager().registerEvents(listener, plugin)
                try {
                    while (true) {
                        val event = listener.outQueue.take()
                        emit(event)
                    }
                } catch (e: CancellationException) {
                    plugin.logger.info { "getEventStream(${eventType})[${creationTime}]: Cancellation: ${e.message}" }
                } catch (e: Exception) {
                    plugin.logger.warning { "getEventStream(${eventType})[${creationTime}]: Exception: ${e.javaClass}: ${e.message}" }
                } finally {
                    HandlerList.unregisterAll(listener)
                    plugin.logger.info { "getEventStream(${eventType})[${creationTime}]: stopped listener" }
                }
            }
        }.flowOn(Dispatchers.IO)
    }
}
