package io.rivrs.geysermodelengine.utils;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.PacketEvents;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PacketUtils {

    public static void sendPacket(UUID uniqueId, Object packet) {
        Player player = Bukkit.getPlayer(uniqueId);
        if (player == null)
            return;

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    public static void sendPacket(Set<UUID> uniqueIds, Object packet) {
        for (UUID uniqueId : uniqueIds) {
            sendPacket(uniqueId, packet);
        }
    }

}
