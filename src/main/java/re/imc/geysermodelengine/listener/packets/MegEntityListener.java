package re.imc.geysermodelengine.listener.packets;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.ticxo.modelengine.api.ModelEngineAPI;

import lombok.RequiredArgsConstructor;
import re.imc.geysermodelengine.GeyserModelEngine;

@RequiredArgsConstructor
public class MegEntityListener extends SimplePacketListenerAbstract {

    private final GeyserModelEngine plugin;

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        final Player player = (Player) event.getPlayer();
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
            return;

        if (event.getPacketType().equals(PacketType.Play.Server.SPAWN_ENTITY)) {
            WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
            if (ModelEngineAPI.getModeledEntity(wrapper.getEntityId()) == null)
                return;

            System.out.println("[MEG-Listener] Cancelled Entity spawn with ID: " + wrapper.getEntityId());
            event.setCancelled(true);
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_METADATA)) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            if (ModelEngineAPI.getModeledEntity(wrapper.getEntityId()) == null)
                return;

            System.out.println("[MEG-Listener] Cancelled Entity metadata with ID: " + wrapper.getEntityId());
            event.setCancelled(true);
        } else if (event.getPacketType().equals(PacketType.Play.Server.ENTITY_TELEPORT)) {
            WrapperPlayServerEntityTeleport wrapper = new WrapperPlayServerEntityTeleport(event);
            if (ModelEngineAPI.getModeledEntity(wrapper.getEntityId()) == null)
                return;

            System.out.println("[MEG-Listener] Cancelled Entity teleport with ID: " + wrapper.getEntityId());
            event.setCancelled(true);
        }
    }

    @Override
    public PacketListenerPriority getPriority() {
        return PacketListenerPriority.MONITOR;
    }
}
