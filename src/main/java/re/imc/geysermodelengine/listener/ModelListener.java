package re.imc.geysermodelengine.listener;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.wrappers.Pair;
import com.ticxo.modelengine.api.events.*;
import com.ticxo.modelengine.api.model.ActiveModel;

import lombok.RequiredArgsConstructor;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.model.EntityTask;
import re.imc.geysermodelengine.model.ModelEntity;

@RequiredArgsConstructor
public class ModelListener implements Listener {

    private final GeyserModelEngine plugin;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAddModel(AddModelEvent event) {
        if (event.isCancelled() || !this.plugin.isInitialized())
            return;

        Bukkit.getScheduler().runTask(this.plugin, () -> ModelEntity.create(event.getTarget(), event.getModel()));
    }


    @EventHandler
    public void onRemoveModel(RemoveModelEvent event) {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelMount(ModelMountEvent event) {
        Map<ActiveModel, ModelEntity> map = ModelEntity.ENTITIES.get(event.getVehicle().getModeledEntity().getBase().getEntityId());
        if (map == null || !event.isDriver())
            return;
        ModelEntity model = map.get(event.getVehicle());

        if (model != null && event.getPassenger() instanceof Player player) {
            this.plugin.getDrivers().put(player, new Pair<>(event.getVehicle(), event.getSeat()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelDismount(ModelDismountEvent event) {
        if (event.getPassenger() instanceof Player player) {
            this.plugin.getDrivers().remove(player);
        }
    }


    @EventHandler
    public void onAnimationPlay(AnimationPlayEvent event) {
        if (event.getModel().getModeledEntity() == null) {
            return;
        }
        Map<ActiveModel, ModelEntity> map = ModelEntity.ENTITIES.get(event.getModel().getModeledEntity().getBase().getEntityId());
        if (map == null) {
            return;
        }
        ModelEntity model = map.get(event.getModel());

        if (model != null) {
            EntityTask task = model.getTask();
            int p = (event.getProperty().isForceOverride() ? 80 : (event.getProperty().isOverride() ? 70 : 60));
            task.playAnimation(event.getProperty().getName(), p);
        }
    }


    @EventHandler
    public void onModelEntityHurt(EntityDamageEvent event) {
        Map<ActiveModel, ModelEntity> model = ModelEntity.ENTITIES.get(event.getEntity().getEntityId());
        if (model != null) {
            for (Map.Entry<ActiveModel, ModelEntity> entry : model.entrySet()) {
                if (!entry.getValue().getEntity().isDead()) {
                    entry.getValue().getEntity().sendHurtPacket(entry.getValue().getViewers());
                }
            }

        }
    }

    /*

    @EventHandler
    public void onModelAttack(EntityDamageByEntityEvent event) {
        ModelEntity model = ModelEntity.ENTITIES.get(event.getDamager().getEntityId());
        if (model != null) {
            EntityTask task = model.getTask();

            task.playAnimation("attack", 55);
        }
    }

     */


    @EventHandler
    public void onAnimationEnd(AnimationEndEvent event) {

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GeyserModelEngine.getInstance().getJoinedPlayer().put(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GeyserModelEngine.getInstance().getDrivers().remove(event.getPlayer());
    }
}
