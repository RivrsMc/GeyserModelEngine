package io.rivrs.geysermodelengine.entity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import io.rivrs.geysermodelengine.GeyserModelEngine;
import io.rivrs.geysermodelengine.model.BedrockEntity;
import io.rivrs.geysermodelengine.task.EntityUpdateTask;
import io.rivrs.geysermodelengine.task.EntityViewersTask;

public class EntitiesManager {

    private final GeyserModelEngine plugin;
    private final List<BedrockEntity> entities = new CopyOnWriteArrayList<>();

    public EntitiesManager(GeyserModelEngine plugin) {
        this.plugin = plugin;

        this.startTasks();
    }

    private void startTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new EntityViewersTask(this.plugin), 100, 5);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new EntityUpdateTask(this.plugin), 100, 5);
    }

    public void add(ModeledEntity target, ActiveModel model) {
        BedrockEntity entity = new BedrockEntity(
                plugin.getConfiguration().modelEntityType(),
                target.getBase().getLocation(),
                target,
                model
        );

        this.entities.add(entity);

        // Spawn
        entity.spawn();
    }

    public void removeEntity(BedrockEntity entity) {
        this.entities.remove(entity);
    }

    public void removeEntity(UUID uniqueId) {
        this.entities.removeIf(entity -> entity.getUniqueId().equals(uniqueId));
    }

    public void removeEntity(int entityId) {
        this.entities.removeIf(entity -> entity.getId() == entityId);
    }

    public Optional<BedrockEntity> findById(int id) {
        return this.entities.stream()
                .filter(entity -> entity.getId() == id)
                .findFirst();
    }

    public Optional<BedrockEntity> findByUniqueId(UUID uniqueId) {
        return this.entities.stream()
                .filter(entity -> entity.getUniqueId().equals(uniqueId))
                .findFirst();
    }

    public List<BedrockEntity> getViewedEntities(UUID uniqueId) {
        return this.entities.stream()
                .filter(bedrockEntity -> bedrockEntity.hasViewer(uniqueId))
                .toList();
    }

    public int getViewedEntitiesCount(UUID uniqueId) {
        return (int) this.entities.stream()
                .filter(bedrockEntity -> bedrockEntity.hasViewer(uniqueId))
                .count();
    }

    public List<BedrockEntity> entities() {
        return Collections.unmodifiableList(this.entities);
    }
}
