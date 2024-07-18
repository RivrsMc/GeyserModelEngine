package io.rivrs.geysermodelengine;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.retrooper.packetevents.PacketEvents;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.rivrs.geysermodelengine.configuration.Configuration;
import io.rivrs.geysermodelengine.entity.EntitiesManager;
import io.rivrs.geysermodelengine.listener.ModelEngineListener;
import io.rivrs.geysermodelengine.listener.PlayerListener;
import io.rivrs.geysermodelengine.listener.packet.ArmorStandListener;
import io.rivrs.geysermodelengine.listener.packet.EntityTeleportListener;
import io.rivrs.geysermodelengine.player.PlayersManager;
import lombok.Getter;

@Getter
public class GeyserModelEngine extends JavaPlugin {

    // Configuration
    private Configuration configuration;

    // Managers
    private PlayersManager players;
    private EntitiesManager entities;

    // State
    private transient boolean started;

    @Override
    public void onLoad() {
        // Packet events
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .kickOnPacketException(false)
                .reEncodeByDefault(false)
                .checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        // Configuration
        configuration = new Configuration(this.getDataFolder().toPath().resolve("config.yml"));
        configuration.load();

        // Managers
        this.players = new PlayersManager(this);
        this.entities = new EntitiesManager(this);

        // Packet events
        //PacketEvents.getAPI().getEventManager().registerListener(new EntityTeleportListener());
        PacketEvents.getAPI().init();

        // Events
        List.of(
                new ModelEngineListener(this),
                new PlayerListener(this)
        ).forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));

        this.started = true;
    }

    @Override
    public void onDisable() {
        this.started = false;

        // Bukkit
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        // Packet events
        PacketEvents.getAPI().terminate();
    }

}
