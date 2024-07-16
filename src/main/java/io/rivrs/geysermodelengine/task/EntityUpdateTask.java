package io.rivrs.geysermodelengine.task;

import org.bukkit.Bukkit;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import io.rivrs.geysermodelengine.GeyserModelEngine;
import io.rivrs.geysermodelengine.model.BedrockEntity;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityUpdateTask implements Runnable {

    private final GeyserModelEngine plugin;

    @Override
    public void run() {
        for (BedrockEntity entity : this.plugin.getEntities().entities()) {
            final ActiveModel activeModel = entity.getActiveModel();
            final ModeledEntity modeledEntity = entity.getModeledEntity();

            // Check if the model is destroyed, removed, or the entity is dead
            if (activeModel.isDestroyed()
                || activeModel.isRemoved()
                || !modeledEntity.getBase().isAlive()) {
                this.handleRemoval(entity);
                return;
            }
        }
    }

    private void handleRemoval(BedrockEntity entity) {
        if (!entity.getActiveModel().isRemoved() && entity.hasAnimation("death")) {
            Bukkit.getScheduler().runTaskLater(this.plugin, entity::remove, Math.min(Math.max(playAnimation("death", 999, 5f, true) - 3, 0), 200));
        } else {
            Bukkit.getScheduler().runTask(this.plugin, entity::remove);
        }

        this.plugin.getEntities().removeEntity(entity.getUniqueId());
    }
}
