package re.imc.geysermodelengine.model;

import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3f;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.entity.BaseEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.ModelBone;

import io.rivrs.bedrockcore.api.BedrockAPI;
import lombok.Getter;
import lombok.Setter;
import me.zimzaza4.geyserutils.common.animation.Animation;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import me.zimzaza4.geyserutils.spigot.api.PlayerUtils;
import re.imc.geysermodelengine.GeyserModelEngine;

@Getter
@Setter
public class EntityTask {

    private static final String ANIMATION_PROPERTY = "modelengine:anim";

    private final ModelEntity model;

    private int tick = 0;
    private int syncTick = 0;

    private final AtomicInteger animationCooldown = new AtomicInteger(0);
    private final AtomicInteger currentAnimationPriority = new AtomicInteger(0);

    private boolean spawnAnimationPlayed = false;
    private boolean removed = false;

    private float lastScale = -1.0f;
    private Color lastColor = null;
    private final Map<ModelBone, Boolean> lastModelBoneSet = new HashMap<>();


    private String lastAnimation = "";
    private int currentAnimProperty = 1;
    private int lastAnimProperty = -1;

    private boolean looping = true;

    private BukkitRunnable syncTask;
    private BukkitRunnable asyncTask;


    public EntityTask(ModelEntity model) {
        this.model = model;
    }

    public void runAsync() {
        PacketEntity entity = model.getEntity();
        if (entity.isDead()) {
            return;
        }

        model.teleportToModel();
        Set<Player> viewers = model.getViewers();
        ActiveModel activeModel = model.getActiveModel();
        ModeledEntity modeledEntity = model.getModeledEntity();
        if (activeModel.isDestroyed() || activeModel.isRemoved() || !modeledEntity.getBase().isAlive()) {
            if (!activeModel.isRemoved() && hasAnimation("death")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        removed = true;
                        entity.remove();
                    }
                }.runTaskLater(GeyserModelEngine.getInstance(), Math.min(Math.max(playAnimation("death", 999, 5f, true) - 3, 0), 200));
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        removed = true;
                        entity.remove();
                    }
                }.runTask(GeyserModelEngine.getInstance());
            }

            ModelEntity.ENTITIES.remove(modeledEntity.getBase().getEntityId());
            ModelEntity.MODEL_ENTITIES.remove(entity.getEntityId());
            cancel();
            return;
        }

        if (!spawnAnimationPlayed) {
            spawnAnimationPlayed = true;
        }

        if (tick % 5 == 0) {

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (BedrockAPI.isBedrockPlayer(onlinePlayer)) {

                    if (canSee(onlinePlayer, model.getEntity())) {

                        if (!viewers.contains(onlinePlayer)) {
                            sendSpawnPacket(onlinePlayer);
                            viewers.add(onlinePlayer);
                        }
                    } else {
                        if (viewers.contains(onlinePlayer)) {
                            entity.sendEntityDestroyPacket(Collections.singletonList(onlinePlayer));
                            viewers.remove(onlinePlayer);
                        }
                    }
                }
            }

            if (tick % 40 == 0) {

                for (Player viewer : Set.copyOf(viewers)) {

                    if (!canSee(viewer, model.getEntity())) {
                        viewers.remove(viewer);
                    }
                }
            }
        }

        tick++;
        if (tick > 400) {
            tick = 0;
            sendHitBoxToAll();
        }

        BaseEntity<?> base = modeledEntity.getBase();

        if (base.isStrafing() && hasAnimation("strafe")) {
            playAnimation("strafe", 50);
        } else if (base.isFlying() && hasAnimation("fly")) {
            playAnimation("fly", 40);
        } else if (base.isJumping() && hasAnimation("jump")) {
            playAnimation("jump", 30);
        } else if (base.isWalking() && hasAnimation("walk")) {
            setAnimationProperty(3);
            // playAnimation("walk", 20);
        } else if (hasAnimation("idle")) {
            // playAnimation("idle", 0);
            setAnimationProperty(2);
        }

        if (animationCooldown.get() > 0) {
            animationCooldown.decrementAndGet();
        }

        Optional<Player> player = viewers.stream().findAny();
        if (player.isEmpty()) return;

        updateEntityProperties(player.get(), false);

        // do not actually use this, atleast bundle these up ;(
        sendScale(player.get(), true);
        sendColor(player.get(), true);
    }

    private void sendSpawnPacket(Player onlinePlayer) {
        EntityTask task = model.getTask();
        int delay = 1;
        boolean firstJoined = GeyserModelEngine.getInstance().getJoinedPlayer().getIfPresent(onlinePlayer) != null;
        if (firstJoined) {
            delay = GeyserModelEngine.getInstance().getJoinSendDelay();
        }
        if (task == null || firstJoined) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> model.getTask().sendEntityData(onlinePlayer, GeyserModelEngine.getInstance().getSendDelay()), delay);
        } else {
            task.sendEntityData(onlinePlayer, GeyserModelEngine.getInstance().getSendDelay());
        }
    }

    public void sendEntityData(Player player, int delay) {
        EntityUtils.setCustomEntity(player, model.getEntity().getEntityId(), "modelengine:" + model.getActiveModel().getBlueprint().getName().toLowerCase());
        Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
            model.getEntity().sendSpawnPacket(Collections.singletonList(player));
            Bukkit.getScheduler().runTaskLaterAsynchronously(GeyserModelEngine.getInstance(), () -> {
                if (looping) {
                    playBedrockAnimation(lastAnimation, Set.of(player), looping, 0f);
                }
                sendHitBox(player);
                sendScale(player, true);
                updateEntityProperties(player, true);
            }, 8);
        }, delay);
    }

    public void sendScale(Player player, boolean ignore) {
        if (player == null) return;

        Vector3f scale = model.getActiveModel().getScale();
        float average = (scale.x + scale.y + scale.z) / 3;
        if (average == lastScale) return;

        EntityUtils.sendCustomScale(player, model.getEntity().getEntityId(), average);

        lastScale = average;
    }

    public void sendColor(Player player, boolean ignore) {
        if (player == null) return;

        Color color = new Color(model.getActiveModel().getDefaultTint().asARGB());
        if (color.equals(lastColor)) return;

        EntityUtils.sendCustomColor(player, model.getEntity().getEntityId(), color);

        lastColor = color;
    }

    public void setAnimationProperty(int currentAnimProperty) {
        this.lastAnimProperty = currentAnimProperty;
        this.currentAnimProperty = currentAnimProperty;
    }

    public void updateEntityProperties(Player player, boolean ignore) {
        int entity = model.getEntity().getEntityId();

        Map<String, Boolean> updates = new HashMap<>();

        if (GeyserModelEngine.getInstance().getEnablePartVisibilityModels().contains(model.getActiveModel().getBlueprint().getName())) {
            model.getActiveModel().getBones().forEach((s, bone) -> {
                if (!lastModelBoneSet.containsKey(bone))
                    lastModelBoneSet.put(bone, !bone.isVisible());

                Boolean lastBone = lastModelBoneSet.get(bone);
                if (lastBone == null)
                    return;

                if (!lastBone.equals(bone.isVisible()) || ignore) {
                    String name = unstripName(bone).toLowerCase();
                    updates.put(model.getActiveModel().getBlueprint().getName() + ":" + name, bone.isVisible());
                    lastModelBoneSet.replace(bone, bone.isVisible());
                }

            });
        }

        int animationUpdate = -1;

        if (ignore || !(lastAnimProperty == currentAnimProperty)) {
            if (animationCooldown.get() == 0) {
                animationUpdate = currentAnimProperty;
            } else {
                animationUpdate = 0;
            }
        }
        if (animationUpdate != -1) {
            EntityUtils.sendIntProperty(player, entity, ANIMATION_PROPERTY, animationUpdate);
        }
        if (updates.isEmpty()) return;

        EntityUtils.sendBoolProperties(player, entity, updates);
    }

    private String unstripName(ModelBone bone) {
        String name = bone.getBoneId();
        if (bone.getBlueprintBone().getBehaviors().get("head") != null) {
            if (!bone.getBlueprintBone().getBehaviors().get("head").isEmpty()) return "hi_" + name;
            return "h_" + name;
        }

        return name;
    }

    public void sendHitBoxToAll() {
        for (Player viewer : model.getViewers()) {
            EntityUtils.sendCustomHitBox(viewer, model.getEntity().getEntityId(), 0.01f, 0.01f);
        }

    }

    public void sendHitBox(Player viewer) {
        EntityUtils.sendCustomHitBox(viewer, model.getEntity().getEntityId(), 0.01f, 0.01f);

    }

    public boolean hasAnimation(String animation) {
        ActiveModel activeModel = model.getActiveModel();
        BlueprintAnimation animationProperty = activeModel.getBlueprint().getAnimations().get(animation);
        return !(animationProperty == null);
    }

    public int playAnimation(String animation, int p) {
        return playAnimation(animation, p, 0, false);
    }

    public int playAnimation(String animation, int p, float blendTime, boolean forceLoop) {

        ActiveModel activeModel = model.getActiveModel();

        BlueprintAnimation animationProperty = activeModel.getBlueprint().getAnimations().get(animation);


        if (animationProperty == null) {
            return 0;
        }


        boolean play = false;
        if (currentAnimationPriority.get() < p) {
            currentAnimationPriority.set(p);
            play = true;
        } else if (animationCooldown.get() == 0) {
            play = true;
        }
        looping = forceLoop || animationProperty.getLoopMode() == BlueprintAnimation.LoopMode.LOOP;
        ;

        if (lastAnimation.equals(animation)) {
            if (looping) {
                play = false;
            }
        }


        if (play) {
            setAnimationProperty(0);

            currentAnimationPriority.set(p);

            String id = "animation." + activeModel.getBlueprint().getName().toLowerCase() + "." + animationProperty.getName().toLowerCase();
            lastAnimation = id;
            animationCooldown.set((int) (animationProperty.getLength() * 20));
            playBedrockAnimation(id, model.getViewers(), looping, blendTime);
        }
        return animationCooldown.get();
    }

    public void playBedrockAnimation(String animationId, Set<Player> viewers, boolean loop, float blendTime) {
        int entity = model.getEntity().getEntityId();
        Animation.AnimationBuilder animation = Animation.builder()
                .animation(animationId)
                .blendOutTime(blendTime);

        if (loop) {
            animation.nextState(animationId);
        }
        for (Player viewer : viewers) {
            PlayerUtils.playEntityAnimation(viewer, animation.build(), Collections.singletonList(entity));
        }

    }

    private boolean canSee(Player player, PacketEntity entity) {
        if (!player.isOnline() || player.isDead()) {
            return false;
        }
        if (GeyserModelEngine.getInstance().getJoinedPlayer() != null && GeyserModelEngine.getInstance().getJoinedPlayer().getIfPresent(player) != null) {
            return false;
        }

        if (entity.getLocation().getChunk() == player.getChunk()) {
            return true;
        }

        if (!Objects.equals(entity.getLocation().getWorld(), player.getWorld())) {
            return false;
        }

        if (player.getLocation().distanceSquared(entity.getLocation()) > player.getSimulationDistance() * player.getSimulationDistance() * 256) {
            return false;
        }
        return !(player.getLocation().distance(entity.getLocation()) > GeyserModelEngine.getInstance().getViewDistance());

    }

    public void cancel() {
        // syncTask.cancel();
        asyncTask.cancel();
    }

    public void run(GeyserModelEngine instance, int i) {
        String id;
        ActiveModel activeModel = model.getActiveModel();
        if (hasAnimation("spawn")) {
            id = "animation." + activeModel.getBlueprint().getName().toLowerCase() + ".spawn";
        } else {
            id = "animation." + activeModel.getBlueprint().getName().toLowerCase() + ".idle";
        }

        lastAnimation = id;
        sendHitBoxToAll();

        asyncTask = new BukkitRunnable() {
            @Override
            public void run() {
                runAsync();
            }
        };
        asyncTask.runTaskTimerAsynchronously(instance, i + 2, 0);
    }
}