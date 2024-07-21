package re.imc.geysermodelengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.type.Mount;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import re.imc.geysermodelengine.listener.ModelListener;
import re.imc.geysermodelengine.listener.MountPacketListener;
import re.imc.geysermodelengine.model.ModelEntity;
import re.imc.geysermodelengine.utils.Pair;

@Getter
public final class GeyserModelEngine extends JavaPlugin {

    @Getter
    private static GeyserModelEngine instance;
    private int sendDelay;
    private int viewDistance;
    private EntityType modelEntityType;
    private Cache<Player, Boolean> joinedPlayer;
    private int joinSendDelay;
    private boolean debug;
    private final Map<Player, Pair<ActiveModel, Mount>> drivers = new ConcurrentHashMap<>();
    private boolean initialized = false;
    private final List<String> enablePartVisibilityModels = new ArrayList<>();

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
        // Plugin startup logic
        saveDefaultConfig();
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
        instance = this;

        PacketEvents.getAPI().getEventManager().registerListener(new MountPacketListener(this));
        PacketEvents.getAPI().init();

        Bukkit.getPluginManager().registerEvents(new ModelListener(this), this);
        Bukkit.getScheduler()
                .runTaskLater(this, () -> {
                    for (World world : Bukkit.getWorlds()) {
                        for (Entity entity : world.getEntities()) {
                            if (!ModelEntity.ENTITIES.containsKey(entity.getEntityId())) {
                                ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
                                if (modeledEntity != null) {
                                    Optional<ActiveModel> model = modeledEntity.getModels().values().stream().findFirst();
                                    model.ifPresent(m -> ModelEntity.create(modeledEntity, m));
                                }
                            }
                        }
                    }
                    initialized = true;
                }, 100);
    }

    @Override
    public void onDisable() {
        for (Map<ActiveModel, ModelEntity> entities : ModelEntity.ENTITIES.values()) {
            entities.forEach((model, modelEntity) -> modelEntity.getEntity().remove());
        }

        // Bukkit
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        // Packet events
        PacketEvents.getAPI().terminate();
    }

}