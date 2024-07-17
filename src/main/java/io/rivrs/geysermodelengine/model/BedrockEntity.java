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

import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;
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

        this.viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> this.sendEntityData(JavaPlugin.getProvidingPlugin(GeyserModelEngine.class), player, 0));

        this.removed = false;
    }

    public void spawn(JavaPlugin plugin, Player player, int delay) {
        System.out.println("Spawning entity " + this.activeModel.getBlueprint().getName().toLowerCase() + " for " + player.getName());
        this.sendEntityData(plugin, player, delay);
    }

    public void teleport(@NotNull Location location) {
        this.location = location;

        if (!this.removed)
            PacketUtils.sendPacket(this.viewers, this.getTeleportPacket(this.location));
    }

    public void teleport(Player player) {
        if (!this.removed)
            PacketUtils.sendPacket(player.getUniqueId(), this.getTeleportPacket(this.location));
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
    public void updateEntityProperties(boolean ignore) {
        this.viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> updateEntityProperties(player, ignore));
    }

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

        System.out.println("Updating entity properties for " + player.getName() + " : " + this.activeModel.getBlueprint().getName().toLowerCase());
        updates.forEach((s, aBoolean) -> System.out.println(" - " + s + " : " + aBoolean));
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
        Vector3f vector3f = this.activeModel.getHitboxScale();
        System.out.println("Sending hitbox for " + player.getName() + " : " + this.activeModel.getBlueprint().getName().toLowerCase() + " : " + vector3f.y + " : " + vector3f.x);

        PlayerUtils.sendCustomHitBox(player, this.asEntity(), vector3f.y, vector3f.x);
    }

    private void sendScale(Player player, boolean ignore) {
        Vector3f scale = this.activeModel.getScale();
        float average = (scale.x + scale.y + scale.z) / 3;

        if (average == this.lastScale)
            return;

        System.out.println("Sending scale for " + player.getName() + " : " + this.activeModel.getBlueprint().getName().toLowerCase() + " : " + average);
        PlayerUtils.sendCustomScale(player, this.asEntity(), average);
        if (!ignore)
            this.lastScale = average;
    }

    private void sendEntityData(JavaPlugin plugin, Player player, int delay) {
        PlayerUtils.setCustomEntity(player, this.asEntity(), "modelengine:%s".formatted(this.activeModel.getBlueprint().getName().toLowerCase()));
        System.out.println("Sending entity data for " + player.getName() + " : " + this.activeModel.getBlueprint().getName().toLowerCase());

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            PacketUtils.sendPacket(player.getUniqueId(), getSpawnPacket());
            System.out.println("Sending spawn packet for " + player.getName() + " : " + this.activeModel.getBlueprint().getName().toLowerCase());

            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (looping) {
                    this.playBedrockAnimation(player, this.lastAnimation, true, 0);
                    System.out.println("Playing animation for " + player.getName() + " : " + this.activeModel.getBlueprint().getName().toLowerCase());
                }

                this.sendHitBox(player);
                this.sendScale(player, true);
                System.out.println("Updating entity properties for " + player.getName() + " : " + this.activeModel.getBlueprint().getName().toLowerCase());
                this.updateEntityProperties(player, true);
            }, 10);
        }, delay);
    }

    // Packets methods
    private WrapperPlayServerSpawnEntity getSpawnPacket() {
        return new WrapperPlayServerSpawnEntity(
                this.id,
                this.uniqueId,
                SpigotConversionUtil.fromBukkitEntityType(this.entityType),
                SpigotConversionUtil.fromBukkitLocation(this.location),
                0,
                0,
                Vector3d.zero()
        );
    }

    private WrapperPlayServerEntityTeleport getTeleportPacket(Location destination) {
        return new WrapperPlayServerEntityTeleport(
                this.id,
                SpigotConversionUtil.fromBukkitLocation(destination),
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
