package io.rivrs.geysermodelengine.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;

import io.rivrs.geysermodelengine.GeyserModelEngine;
import io.rivrs.geysermodelengine.utils.ModelUtils;
import io.rivrs.geysermodelengine.utils.PacketUtils;
import lombok.Data;
import me.zimzaza4.geyserutils.common.animation.Animation;
import me.zimzaza4.geyserutils.spigot.api.PlayerUtils;

@Data
public class BedrockEntity {

    private final int id;
    private final UUID uniqueId;
    private final EntityType entityType;
    private final Set<UUID> viewers = new CopyOnWriteArraySet<>();
    private Location location;
    private boolean removed;

    // Model engine
    private final ModeledEntity modeledEntity;
    private final ActiveModel activeModel;

    // State
    private final Map<ModelBone, Boolean> lastModelBoneSet = new HashMap<>();
    private final AtomicInteger animationCooldown = new AtomicInteger(0);
    private final AtomicInteger currentAnimationPriority = new AtomicInteger(0);
    private String lastAnimation = "";
    private String currentAnimProperty = "anim_spawn";
    private String lastAnimProperty = "";
    private boolean looping = true;
    private float lastScale = -1.0f;

    public BedrockEntity(EntityType entityType, Location location, ModeledEntity modeledEntity, ActiveModel activeModel) {
        this.id = ThreadLocalRandom.current().nextInt(20000, 100000000);
        this.uniqueId = UUID.randomUUID();
        this.entityType = entityType;
        this.location = location;
        this.modeledEntity = modeledEntity;
        this.activeModel = activeModel;
        this.removed = true;
    }

    public boolean isAlive() {
        return !removed;
    }

    public boolean isDead() {
        return removed;
    }

    // Actions
    public void spawn() {
        if (!this.removed)
            return;

        PacketUtils.sendPacket(this.viewers, getSpawnPacket());
        this.removed = false;
    }

    public void spawn(JavaPlugin plugin, Player player, int delay) {
        this.sendEntityData(plugin, player, delay);
    }

    public void teleport(@NotNull Location location) {
        this.location = location;

        if (!this.removed)
            PacketUtils.sendPacket(this.viewers, this.getTeleportPacket(this.location));
    }

    public void remove() {
        if (this.removed)
            return;

        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(this.id);
        PacketUtils.sendPacket(this.viewers, destroyPacket);

        removed = true;
    }

    public void playHurtAnimation() {
        if (this.isDead())
            return;

        WrapperPlayServerHurtAnimation hurtPacket = new WrapperPlayServerHurtAnimation(this.id, 1f);
        PacketUtils.sendPacket(this.viewers, hurtPacket);
    }

    // Viewer methods
    public void addViewer(Player player) {
        viewers.add(player.getUniqueId());

        if (this.isAlive())
            this.spawn(JavaPlugin.getProvidingPlugin(GeyserModelEngine.class), player, 0);
    }

    public void removeViewer(UUID viewer) {
        viewers.remove(viewer);

        if (this.isAlive())
            PacketUtils.sendPacket(viewer, getDestroyPacket());
    }

    public boolean hasViewer(UUID viewer) {
        return viewers.contains(viewer);
    }

    // Model engine
    public @Nullable BlueprintAnimation animation(String animationName) {
        return this.activeModel.getBlueprint().getAnimations().get(animationName);
    }

    public boolean hasAnimation(String animationName) {
        return this.animation(animationName) != null;
    }

    public int playAnimation(String animationName, int priority) {
        return this.playAnimation(animationName, priority, 0, false);
    }

    public int playAnimation(String animationName, int priority, float blendTime, boolean forceLoop) {
        BlueprintAnimation animation = this.animation(animationName);
        if (animation == null)
            return 0;

        boolean play = false;
        if (this.currentAnimationPriority.get() < priority) {
            this.currentAnimationPriority.set(priority);
            play = true;
        } else if (this.animationCooldown.get() == 0)
            play = true;

        this.looping = forceLoop || animation.getLoopMode().equals(BlueprintAnimation.LoopMode.LOOP);

        if (this.lastAnimation.equals(animationName) && looping)
            play = false;

        if (!play)
            return this.animationCooldown.get();

        this.setAnimationProperty("modelengine:anim_stop");
        viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> updateEntityProperties(player, false));
        this.currentAnimationPriority.set(priority);

        this.lastAnimation = "animation.%s.%s".formatted(this.activeModel.getBlueprint().getName(), animation.getName()).toLowerCase();

        this.animationCooldown.set((int) (animation.getLength() * 20));
        this.playBedrockAnimation(this.lastAnimation, looping, blendTime);

        return this.animationCooldown.get();
    }

    // Bedrock methods
    public void updateEntityProperties(Player player, boolean ignore) {
        Map<String, Boolean> updates = new HashMap<>();

        for (ModelBone bone : this.activeModel.getBones().values()) {
            if (!this.lastModelBoneSet.containsKey(bone))
                this.lastModelBoneSet.put(bone, !bone.isVisible());

            boolean lastBone = this.lastModelBoneSet.getOrDefault(bone, false);
            if (lastBone != bone.isVisible() || ignore) {
                updates.put("%s:%s".formatted(this.activeModel.getBlueprint().getName(), ModelUtils.unstripName(bone)).toLowerCase(), bone.isVisible());
                this.lastModelBoneSet.replace(bone, bone.isVisible());
            }
        }

        if (ignore || !lastAnimProperty.equals(currentAnimProperty)) {
            updates.put(lastAnimProperty, false);
            updates.put(currentAnimProperty, true);
        }
        if (updates.isEmpty()) return;

        PlayerUtils.sendBoolProperties(player, this.asEntity(), updates);
    }

    public void setAnimationProperty(String currentAnimProperty) {
        this.lastAnimProperty = currentAnimProperty;
        this.currentAnimProperty = currentAnimProperty;
    }

    private void playBedrockAnimation(String id, boolean loop, float blendTime) {
        this.viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> playBedrockAnimation(player, id, loop, blendTime));
    }

    private void playBedrockAnimation(Player player, String id, boolean loop, float blendTime) {
        Animation.AnimationBuilder animation = Animation.builder()
                .animation(id)
                .blendOutTime(blendTime);
        if (loop)
            animation.nextState(id);

        PlayerUtils.playEntityAnimation(player, animation.build(), Collections.singletonList(this.id));
    }

    public void sendHitBox() {
        this.viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(this::sendHitBox);
    }

    private void sendHitBox(Player player) {
        PlayerUtils.sendCustomHitBox(player, this.asEntity(), 0.01f, 0.01f);
    }

    private void sendScale(Player player, boolean ignore) {
        Vector3f scale = this.activeModel.getScale();
        float average = (scale.x + scale.y + scale.z) / 3;

        if (average == this.lastScale)
            return;

        PlayerUtils.sendCustomScale(player, this.asEntity(), average);
        if (!ignore)
            this.lastScale = average;
    }

    private void sendEntityData(JavaPlugin plugin, Player player, int delay) {
        PlayerUtils.setCustomEntity(player, this.id, "modelengine:%s".formatted(this.activeModel.getBlueprint().getName().toLowerCase()));
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            PacketUtils.sendPacket(player.getUniqueId(), getSpawnPacket());

            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (looping)
                    this.playBedrockAnimation(player, this.lastAnimation, true, 0);

                this.sendHitBox(player);
                this.sendScale(player, true);
                this.updateEntityProperties(player, true);
            }, 10);
        }, delay);
    }

    // Packets methods
    private WrapperPlayServerSpawnEntity getSpawnPacket() {
        return new WrapperPlayServerSpawnEntity(
                this.id,
                this.uniqueId,
                EntityTypes.getByName(this.entityType.name()),
                PacketUtils.wrap(this.location),
                0,
                0,
                Vector3d.zero()
        );
    }

    private WrapperPlayServerEntityTeleport getTeleportPacket(Location destination) {
        return new WrapperPlayServerEntityTeleport(
                this.id,
                PacketUtils.wrap(destination),
                true
        );
    }

    private WrapperPlayServerDestroyEntities getDestroyPacket() {
        return new WrapperPlayServerDestroyEntities(this.id);
    }

    // Getters
    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    // Utils
    public Entity asEntity() {
        return new DumbEntity(this.id, this.uniqueId);
    }
}
