package re.imc.geysermodelengine.packet.entity;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.packet.*;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PacketEntity {
    public PacketEntity(EntityType type, Set<Player> viewers, Location location) {
        this.id = ThreadLocalRandom.current().nextInt(300000000, 400000000);
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.viewers = viewers;
        this.location = location;
    }

    private int id;
    private UUID uuid;
    private EntityType type;
    private Set<Player> viewers;
    private Location location;
    private boolean removed = false;

    public @NotNull Location getLocation() {
        return location;
    }

    public boolean teleport(@NotNull Location location) {
        this.location = location.clone();
        sendLocationPacket(viewers);
        return true;
    }


    public void remove() {
        removed = true;
        sendEntityDestroyPacket(viewers);
    }

    public boolean isDead() {
        return removed;
    }

    public boolean isValid() {
        return !removed;
    }

    public void sendSpawnPacket(Collection<Player> players) {
        EntitySpawnPacket packet = new EntitySpawnPacket(id, uuid, type, location);
        // EntityMetadataPacket metadataPacket = new EntityMetadataPacket(id);

        players.forEach(player -> {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.encode());
        });
        // players.forEach(player -> ProtocolLibrary.getProtocolManager().sendServerPacket(player, metadataPacket.encode()));
    }

    public void sendLocationPacket(Collection<Player> players) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                id,
                SpigotConversionUtil.fromBukkitLocation(location),
                true
        );
        PacketUtils.broadcast(players, packet);
    }

    public void sendHurtPacket(Collection<Player> players) {
        EntityHurtPacket packet = new EntityHurtPacket(id);
        players.forEach(player -> ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.encode()));
    }

    public void sendEntityDestroyPacket(Collection<Player> players) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(id);
        PacketUtils.broadcast(players, packet);
    }

    public int getEntityId() {
        return id;
    }

}

