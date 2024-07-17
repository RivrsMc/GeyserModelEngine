package io.rivrs.geysermodelengine.task;

import org.bukkit.Bukkit;

import com.ticxo.modelengine.api.entity.BaseEntity;
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
                System.out.println("Entity is dead");
                this.handleRemoval(entity);
                return;
            }

            // Teleport the entity to the meg model
            entity.teleport(modeledEntity.getBase().getLocation());

            // Animation
            BaseEntity<?> base = modeledEntity.getBase();
            if (base.isStrafing() && entity.hasAnimation("strafe"))
                entity.playAnimation("strafe", 50);
            else if (base.isFlying() && entity.hasAnimation("fly"))
                entity.playAnimation("fly", 40);
            else if (base.isJumping() && entity.hasAnimation("jump"))
                entity.playAnimation("jump", 30);
            else if (base.isWalking() && entity.hasAnimation("walk"))
                entity.setAnimationProperty("modelengine:anim_walk");
            else if (entity.hasAnimation("idle"))
                entity.setAnimationProperty("modelengine:anim_idle");

            if (entity.getAnimationCooldown().get() > 0 || entity.getViewers().isEmpty())
                return;

            // Update the model
            entity.updateEntityProperties(false);
        }
    }

    private void handleRemoval(BedrockEntity entity) {
        if (!entity.getActiveModel().isRemoved() && entity.hasAnimation("death")) {
            Bukkit.getScheduler().runTaskLater(this.plugin, entity::remove, Math.min(Math.max(entity.playAnimation("death", 999, 5f, true) - 3, 0), 200));
        } else {
            Bukkit.getScheduler().runTask(this.plugin, entity::remove);
        }

        this.plugin.getEntities().removeEntity(entity.getUniqueId());
    }
}
