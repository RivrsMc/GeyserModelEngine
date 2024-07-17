package io.rivrs.geysermodelengine.task;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.geysermc.floodgate.api.FloodgateApi;

import io.rivrs.geysermodelengine.GeyserModelEngine;
import io.rivrs.geysermodelengine.configuration.Configuration;
import io.rivrs.geysermodelengine.model.BedrockEntity;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityViewersTask implements Runnable {

    private final GeyserModelEngine plugin;

    @Override
    public void run() {
        final Configuration configuration = plugin.getConfiguration();

        for (BedrockEntity entity : this.plugin.getEntities().entities()) {
            final Location location = entity.getLocation();

            // Add missing viewers
            Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(player -> location.getWorld().equals(player.getWorld()))
                    .filter(player -> !entity.hasViewer(player.getUniqueId()))
                    .filter(player -> FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
                    .filter(player -> player.getLocation().distanceSquared(location) <= NumberConversions.square(configuration.viewDistance()))
                    .filter(player -> this.plugin.getEntities().getViewedEntitiesCount(player.getUniqueId()) < configuration.maximumModels())
                    .forEach(entity::addViewer);

            // Remove old viewers
            for (UUID viewer : entity.getViewers()) {
                Player player = Bukkit.getPlayer(viewer);

                // Offline players or players in different worlds are removed
                if (player == null || !player.isOnline() || !location.getWorld().equals(player.getWorld())) {
                    entity.removeViewer(viewer);
                    continue;
                }

                // Players outside the view distance are removed
                if (player.getLocation().distanceSquared(location) > NumberConversions.square(configuration.viewDistance())) {
                    entity.removeViewer(viewer);
                }
            }
        }
    }
}
