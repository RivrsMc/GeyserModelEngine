package re.imc.geysermodelengine;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import re.imc.geysermodelengine.configuration.Configuration;
import re.imc.geysermodelengine.listener.ModelListener;
import re.imc.geysermodelengine.listener.packets.MountPacketListener;
import re.imc.geysermodelengine.model.ModelEntity;
import re.imc.geysermodelengine.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class GeyserModelEngine extends JavaPlugin {

    @Getter
    private static GeyserModelEngine instance;

    private Configuration configuration;
    private Cache<Player, Boolean> joinedPlayer;
    private final Map<Player, Pair<ActiveModel, Mount>> drivers = new ConcurrentHashMap<>();
    private boolean initialized = false;

    @Override
    public void onLoad() {
        super.onLoad();

        // Packet events
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI()
                .getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false)
                .bStats(false);
        PacketEvents.getAPI().load();
    }


    @Getter
    private boolean initialized = false;

    @Getter
    private List<String> enablePartVisibilityModels = new ArrayList<>();
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        // alwaysSendSkin = getConfig().getBoolean("always-send-skin");
        sendDelay = getConfig().getInt("data-send-delay", 0);
        viewDistance = getConfig().getInt("entity-view-distance", 60);
        debug = getConfig().getBoolean("debug", false);
        modelEntityType = EntityType.valueOf(getConfig().getString("model-entity-type", "BAT"));
        joinSendDelay = getConfig().getInt("join-send-delay", 20);
        enablePartVisibilityModels.addAll(getConfig().getStringList("enable-part-visibility-models"));
        if (joinSendDelay > 0) {
            joinedPlayer = CacheBuilder.newBuilder()
                    .expireAfterWrite(joinSendDelay * 50L, TimeUnit.MILLISECONDS).build();
        }

        // Instance
        instance = this;

        // Packets listener
        Arrays.asList(
                //  new MegEntityListener(this),
                new MountPacketListener(this)
        ).forEach(listener -> PacketEvents.getAPI().getEventManager().registerListener(listener));
        PacketEvents.getAPI().init();

        // Events
        Bukkit.getPluginManager().registerEvents(new ModelListener(this), this);

        // Task
        Bukkit.getScheduler()
                .runTaskLater(this, () -> {
                    Bukkit.getWorlds()
                            .stream()
                            .flatMap(world -> world.getEntities().stream())
                            .map(ModelEngineAPI::getModeledEntity)
                            .filter(Objects::nonNull)
                            .forEach(modeledEntity -> modeledEntity.getModels()
                                    .values()
                                    .stream()
                                    .findFirst()
                                    .ifPresent(m -> ModelEntity.create(modeledEntity, m)));
                    initialized = true;
                }, 100);
    }

    @Override
    public void onDisable() {
        for (Map<ActiveModel, ModelEntity> entities : ModelEntity.ENTITIES.values()) {
            entities.forEach((model, modelEntity) -> modelEntity.getEntity().remove());
        }

        ModelEntity.ENTITIES.clear();
        ModelEntity.MODEL_ENTITIES.clear();

        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);


        PacketEvents.getAPI().terminate();
    }

}
