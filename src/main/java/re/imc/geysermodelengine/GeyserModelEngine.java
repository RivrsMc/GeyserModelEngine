package re.imc.geysermodelengine;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.wrappers.Pair;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.type.Mount;

import lombok.Getter;
import re.imc.geysermodelengine.listener.ModelListener;
import re.imc.geysermodelengine.listener.MountPacketListener;
import re.imc.geysermodelengine.model.ModelEntity;

@Getter
public final class GeyserModelEngine extends JavaPlugin {

    @Getter
    private static GeyserModelEngine instance;
    @Getter
    private static boolean alwaysSendSkin;

    private int sendDelay;
    private int viewDistance;
    private EntityType modelEntityType;
    private Cache<Player, Boolean> joinedPlayer;
    private int joinSendDelay;
    private boolean debug;
    private final Map<Player, Pair<ActiveModel, Mount>> drivers = new ConcurrentHashMap<>();

    @Getter
    private boolean initialized = false;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        // Config
        sendDelay = getConfig().getInt("data-send-delay", 0);
        viewDistance = getConfig().getInt("entity-view-distance", 60);
        debug = getConfig().getBoolean("debug", false);
        modelEntityType = EntityType.valueOf(getConfig().getString("model-entity-type", "BAT"));
        joinSendDelay = getConfig().getInt("join-send-delay", 20);
        if (joinSendDelay > 0) {
            joinedPlayer = CacheBuilder.newBuilder()
                    .expireAfterWrite(joinSendDelay * 50L, TimeUnit.MILLISECONDS).build();
        }
        instance = this;

        // Packets listener
        ProtocolLibrary.getProtocolManager().addPacketListener(new MountPacketListener());

        // Events
        Bukkit.getPluginManager().registerEvents(new ModelListener(this), this);

        // Task
        Bukkit.getScheduler()
                .runTaskLater(GeyserModelEngine.getInstance(), () -> {
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
    }

}
