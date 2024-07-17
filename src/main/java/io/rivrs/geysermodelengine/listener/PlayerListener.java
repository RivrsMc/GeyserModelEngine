package io.rivrs.geysermodelengine.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import io.rivrs.geysermodelengine.GeyserModelEngine;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerListener implements Listener {

    private final GeyserModelEngine plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        this.plugin.getPlayers().addPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.plugin.getEntities().unload(e.getPlayer());
        this.plugin.getPlayers().removePlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        final Player player = e.getPlayer();

        if (!e.getFrom().getWorld().equals(e.getTo().getWorld()))
            this.plugin.getPlayers().addPlayer(player.getUniqueId());
    }
}
