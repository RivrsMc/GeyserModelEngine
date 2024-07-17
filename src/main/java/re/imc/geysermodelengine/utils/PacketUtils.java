package re.imc.geysermodelengine.utils;

import java.util.Collection;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PacketUtils {


    public static void broadcast(Collection<Player> players, PacketWrapper<?> packet) {
        players.forEach(player -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet));
    }
}
