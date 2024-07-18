package io.rivrs.geysermodelengine.model;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class PacketEntity {

    protected final int id;
    protected final UUID uniqueId;
    protected final EntityType type;
    protected Location location;

    public void spawn(Player player) {
        WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(
                this.id,
                this.uniqueId,
                SpigotConversionUtil.fromBukkitEntityType(this.type),
                SpigotConversionUtil.fromBukkitLocation(this.location),
                0,
                0,
                Vector3d.zero()
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    public void teleport(Player player, Location destination) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                this.id,
                SpigotConversionUtil.fromBukkitLocation(destination),
                true
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    public void destroy(Player player) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(this.id);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
