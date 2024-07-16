package io.rivrs.geysermodelengine.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.ticxo.modelengine.api.events.AddModelEvent;

import io.rivrs.geysermodelengine.GeyserModelEngine;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ModelEngineListener implements Listener {

    private final GeyserModelEngine plugin;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onModelAdd(AddModelEvent e) {
        if (e.isCancelled() || !this.plugin.isStarted())
            return;

        Bukkit.getScheduler().runTask(plugin, () -> plugin.getEntities().add(e.getTarget(), e.getModel()));
    }
}
