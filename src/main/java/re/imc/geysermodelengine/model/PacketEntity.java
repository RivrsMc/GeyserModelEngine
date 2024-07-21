package re.imc.geysermodelengine.model;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;


public class PacketEntity {

    public PacketEntity(EntityType type, Set<Player> viewers, Location location) {
        this.id = ThreadLocalRandom.current().nextInt(300000000, 400000000);
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.viewers = viewers;
        this.location = location;
    }

    private final int id;
    private final UUID uuid;
    private final EntityType type;
    private final Set<Player> viewers;
    private Location location;
    private boolean removed = false;

    public @NotNull Location getLocation() {
        return location;
    }

    public void teleport(@NotNull Location location) {
        this.location = location.clone();
        sendLocationPacket(viewers);
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
        WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(
                this.id,
                this.uuid,
                SpigotConversionUtil.fromBukkitEntityType(this.type),
                SpigotConversionUtil.fromBukkitLocation(location),
                0,
                0,
                Vector3d.zero()
        );

        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }

    public void sendLocationPacket(Collection<Player> players) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                this.id,
                SpigotConversionUtil.fromBukkitLocation(location),
                true
        );
        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }

    public void sendHurtPacket(Collection<Player> players) {
        WrapperPlayServerHurtAnimation packet = new WrapperPlayServerHurtAnimation(
                this.id,
                2
        );
        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }

    public void sendEntityDestroyPacket(Collection<Player> players) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(this.id);
        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }

    public int getEntityId() {
        return id;
    }

}