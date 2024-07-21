package io.rivrs.geysermodelengine.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.rivrs.geysermodelengine.GeyserModelEngine;
import lombok.Getter;
import me.zimzaza4.geyserutils.common.animation.Animation;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import me.zimzaza4.geyserutils.spigot.api.PlayerUtils;
import net.kyori.adventure.key.Key;

@Getter
public class BedrockEntity extends PacketEntity {

    private static final Logger log = LoggerFactory.getLogger(BedrockEntity.class);
    private final Key modelKey;
    private final ModeledEntity modeledEntity;
    private final ActiveModel activeModel;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    // Cache
    //private final Map<ModelBone, Boolean> lastModelBoneSet = new HashMap<>();

    // Animations
    private final AtomicInteger animationCooldown = new AtomicInteger(0);
    private final AtomicInteger currentAnimationPriority = new AtomicInteger(0);
    private boolean looping = true;
    private String lastAnimationName = "";
    private String lastAnimationProperty = "";
    private String currentAnimationProperty = "";

    public BedrockEntity(EntityType type, ModeledEntity modeledEntity, ActiveModel activeModel) {
        super(SpigotReflectionUtil.generateEntityId(), UUID.randomUUID(), type, modeledEntity.getBase().getLocation());
        this.modeledEntity = modeledEntity;
        this.activeModel = activeModel;
        this.modelKey = Key.key("modelengine", this.activeModel.getBlueprint().getName().toLowerCase());
    }

    // Viewers
    public boolean hasViewer(Player player) {
        return this.viewers.contains(player.getUniqueId());
    }

    public void addViewer(Player player, int delay) {
        this.viewers.add(player.getUniqueId());

        this.spawnBedrockEntity(player);
        Bukkit.getScheduler().runTaskLaterAsynchronously(JavaPlugin.getProvidingPlugin(GeyserModelEngine.class), () -> {
            //this.spawnBedrockEntity(player);
            this.spawn(player);
            Bukkit.getScheduler().runTaskLaterAsynchronously(JavaPlugin.getProvidingPlugin(GeyserModelEngine.class), () -> {
                if (looping)
                    playBedrockAnimation(player, lastAnimationName, true, 0f);
                this.updateEntityProperties(player, true);
            }, 8);
        }, delay);
    }

    public void removeViewer(Player player) {
        this.removeViewer(player, false);
    }

    public void removeViewer(Player player, boolean disconnected) {
        this.viewers.remove(player.getUniqueId());
        if (!disconnected)
            this.destroy(player);
    }

    public List<Player> viewersAsPlayers() {
        return this.viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    public Set<UUID> viewers() {
        return Collections.unmodifiableSet(this.viewers);
    }

    // Packets
    public void teleport(Location location) {
        if (this.location.equals(location))
            return;

        this.location = location;
        this.viewersAsPlayers().forEach(player -> this.teleport(player, location));
    }

    public void destroy() {
        this.viewersAsPlayers().forEach(this::destroy);
    }

    // Bedrock
    public void updateEntityProperties(boolean force) {
        this.viewersAsPlayers().forEach(player -> this.updateEntityProperties(player, force));
    }

    public void updateEntityProperties(Player player, boolean force) {
        this.sendEntityProperties(player, force);
        this.sendHitBox(player);
        this.sendScale(player);
    }

    private void sendEntityProperties(Player player, boolean force) {
        Map<String, Boolean> properties = new HashMap<>();
//        for (ModelBone bone : this.activeModel.getBones().values()) {
//            if (!force && (lastModelBoneSet.containsKey(bone) && lastModelBoneSet.get(bone) == bone.isVisible()))
//                continue;
//
//            properties.put("%s:%s".formatted(this.modelKey.value(), ModelUtils.unstripName(bone)), bone.isVisible());
//            this.lastModelBoneSet.put(bone, bone.isVisible());
//        }

        if (force || !this.lastAnimationProperty.equals(currentAnimationProperty)) {
            properties.put(this.lastAnimationProperty, false);
            properties.put(this.currentAnimationProperty, true);
        }

        // Avoid sending empty properties
        if (properties.isEmpty())
            return;

        EntityUtils.sendBoolProperties(player, this.id, properties);
    }

    private void spawnBedrockEntity(Player player) {
        EntityUtils.setCustomEntity(player, this.id, this.modelKey.asString());
    }

    private void sendHitBox(Player player) {
        EntityUtils.sendCustomHitBox(player, this.id, 0.01f, 0.01f);
    }

    private void sendScale(Player player) {
        Vector3f scale = this.activeModel.getScale();
        float average = (scale.x + scale.y + scale.z) / 3;

        EntityUtils.sendCustomScale(player, this.id, average);
    }

    private void playBedrockAnimation(String name, boolean loop, float blendTime) {
        for (Player viewer : viewersAsPlayers()) {
            playBedrockAnimation(viewer, name, loop, blendTime);
        }
    }

    private void playBedrockAnimation(Player player, String name, boolean loop, float blendTime) {
        Animation.AnimationBuilder builder = Animation.builder()
                .animation(name)
                .blendOutTime(blendTime);

        if (loop)
            builder.nextState(name);

        Animation animation = builder.build();
        PlayerUtils.playEntityAnimation(player, animation, Collections.singletonList(this.id));
    }

    // Animations
    public int playAnimation(String animationName, int priority) {
        return this.playAnimation(animationName, priority, 0, false);
    }

    public int playAnimation(String animationName, int priority, int blendTime, boolean forceLoop) {
        BlueprintAnimation animation = this.animation(animationName).orElse(null);
        if (animation == null)
            return 0;

        boolean play = false;
        if (this.currentAnimationPriority.get() < priority) {
            this.currentAnimationPriority.set(priority);
            play = true;
        } else if (this.animationCooldown.get() == 0)
            play = true;

        this.looping = forceLoop || animation.getLoopMode().equals(BlueprintAnimation.LoopMode.LOOP);

        if (this.lastAnimationName.equals(animationName) && looping)
            play = false;

        if (!play)
            return this.animationCooldown.get();

        this.animationProperty("modelengine:anim_stop");
        this.updateEntityProperties(false);
        this.currentAnimationPriority.set(priority);

        String id = "animation.%s.%s".formatted(this.modelKey.value(), animation.getName().toLowerCase());
        this.lastAnimationName = id;

        this.animationCooldown.set((int) (animation.getLength() * 20));

        playBedrockAnimation(id, this.looping, blendTime);

        return this.animationCooldown.get();
    }

    public boolean hasAnimation(String animationName) {
        return this.activeModel.getBlueprint()
                .getAnimations()
                .containsKey(animationName);
    }

    public Optional<BlueprintAnimation> animation(String name) {
        return Optional.ofNullable(this.activeModel.getBlueprint().getAnimations().get(name));
    }

    public void animationProperty(String currentAnimationProperty) {
        this.lastAnimationProperty = currentAnimationProperty;
        this.currentAnimationProperty = currentAnimationProperty;
    }
}
