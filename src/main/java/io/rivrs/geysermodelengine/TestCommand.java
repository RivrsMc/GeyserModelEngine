package io.rivrs.geysermodelengine;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;

@RequiredArgsConstructor
public class TestCommand implements CommandExecutor {

    private final GeyserModelEngine plugin;
    private Entity baseEntity;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player player)) {
            return false;
        }

        if (this.baseEntity != null) {
            this.baseEntity.remove();
            this.baseEntity = null;
            player.sendMessage("Removed the entity");
            return true;
        }

        // Create an armor stand
        Bat bat = (Bat) player.getWorld().spawnEntity(player.getLocation(), EntityType.BAT);
        bat.setGravity(false);
        bat.setAI(false);
        this.baseEntity = bat;

        // Create a bedrock entity
        int entityId = bat.getEntityId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            EntityUtils.setCustomEntity(player, entityId, "modelengine:autel_plaine");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                EntityUtils.sendCustomHitBox(player, entityId, 0.01f, 0.01f);
                EntityUtils.sendCustomScale(player, entityId, 1f);

                Map<String, Boolean> properties = new HashMap<>();
                properties.put("npc_barmaid:hat", true);
                properties.put("npc_barmaid:leftleg", true);
                properties.put("npc_barmaid", true);
                properties.put("npc_barmaid:chignon", true);
                properties.put("npc_barmaid:leftarm", true);
                properties.put("npc_barmaid:plateau", true);
                properties.put("npc_barmaid:body", true);
                properties.put("npc_barmaid:rightleg", true);
                properties.put("npc_barmaid:cup", true);
                properties.put("npc_barmaid:diam", true);
                properties.put("npc_barmaid:rightarm", true);
                properties.put("npc_barmaid:fourche", true);
                properties.put("npc_barmaid:bot", true);
                properties.put("npc_barmaid:top", true);
                properties.put("npc_barmaid:cup3", true);
                properties.put("npc_barmaid:cup2", true);
                properties.put("npc_barmaid:tablier", true);
                properties.put("modelengine:exclamation", true);
                properties.put("modelengine:anim_idle", true);
                EntityUtils.sendBoolProperties(player, entityId, properties);

                player.sendMessage("Sent properties");
            }, 10);
        }, 10);
        bat.remove();
        bat = null;
        player.sendMessage("Created a java entity");

        return true;
    }
}
