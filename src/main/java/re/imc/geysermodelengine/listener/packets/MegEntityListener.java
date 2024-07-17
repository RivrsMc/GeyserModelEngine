package re.imc.geysermodelengine.listener.packets;

import java.util.List;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.ticxo.modelengine.api.ModelEngineAPI;

import lombok.RequiredArgsConstructor;
import re.imc.geysermodelengine.GeyserModelEngine;

@RequiredArgsConstructor
public class MegEntityListener extends SimplePacketListenerAbstract {

    private final GeyserModelEngine plugin;
    private static final List<PacketType.Play.Server> WATCHED = List.of(
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_STATUS,
            PacketType.Play.Server.ENTITY_HEAD_LOOK,
            PacketType.Play.Server.DESTROY_ENTITIES,
            PacketType.Play.Server.ENTITY_EQUIPMENT,
            PacketType.Play.Server.ENTITY_ANIMATION,
            PacketType.Play.Server.ENTITY_EFFECT,
            PacketType.Play.Server.ENTITY_VELOCITY,
            PacketType.Play.Server.ENTITY_RELATIVE_MOVE,
            PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION,
            PacketType.Play.Server.ENTITY_MOVEMENT,
            PacketType.Play.Server.ENTITY_ROTATION
    );

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (!WATCHED.contains(event.getPacketType())
            || !(event.getPlayer() instanceof Player player)
            || !FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
            return;

        if (event.getPacketType().equals(PacketType.Play.Server.SPAWN_ENTITY)) {
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);

            this.cancelIfNeeded(event, packet.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_METADATA)) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_TELEPORT)) {
            WrapperPlayServerEntityTeleport wrapper = new WrapperPlayServerEntityTeleport(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_STATUS)) {
            WrapperPlayServerEntityStatus wrapper = new WrapperPlayServerEntityStatus(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_HEAD_LOOK)) {
            WrapperPlayServerEntityHeadLook wrapper = new WrapperPlayServerEntityHeadLook(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.DESTROY_ENTITIES)) {
            WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(event);

            for (int entityId : wrapper.getEntityIds()) {
                this.cancelIfNeeded(event, entityId);
            }
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_EQUIPMENT)) {
            WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_ANIMATION)) {
            WrapperPlayServerEntityAnimation wrapper = new WrapperPlayServerEntityAnimation(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_EFFECT)) {
            WrapperPlayServerEntityEffect wrapper = new WrapperPlayServerEntityEffect(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_VELOCITY)) {
            WrapperPlayServerEntityVelocity wrapper = new WrapperPlayServerEntityVelocity(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE)) {
            WrapperPlayServerEntityRelativeMove wrapper = new WrapperPlayServerEntityRelativeMove(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION)) {
            WrapperPlayServerEntityRelativeMoveAndRotation wrapper = new WrapperPlayServerEntityRelativeMoveAndRotation(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_MOVEMENT)) {
            WrapperPlayServerEntityMovement wrapper = new WrapperPlayServerEntityMovement(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_ROTATION)) {
            WrapperPlayServerEntityRotation wrapper = new WrapperPlayServerEntityRotation(event);

            this.cancelIfNeeded(event, wrapper.getEntityId());
        }
    }

    @Override
    public PacketListenerPriority getPriority() {
        return PacketListenerPriority.MONITOR;
    }

    private void cancelIfNeeded(PacketPlaySendEvent event, int entityId) {
        if (ModelEngineAPI.getModeledEntity(entityId) == null)
            return;

        System.out.println("[MEG-Listener] Cancelled Entity packet with ID: " + entityId);
        event.setCancelled(true);
    }
}
