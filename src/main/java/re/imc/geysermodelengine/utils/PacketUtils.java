package re.imc.geysermodelengine.utils;

import java.util.Collection;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PacketUtils {

    public static Location wrap(org.bukkit.Location location) {
        return new Location(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static EntityType wrap(org.bukkit.entity.EntityType entityType) {
        return EntityTypes.getByName(entityType.name());
    }

    public static void broadcast(Collection<Player> players, Object packet) {
        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }
}
