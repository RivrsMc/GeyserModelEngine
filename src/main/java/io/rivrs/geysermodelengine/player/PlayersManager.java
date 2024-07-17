package io.rivrs.geysermodelengine.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.rivrs.geysermodelengine.GeyserModelEngine;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayersManager {

    private final GeyserModelEngine plugin;
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public void addPlayer(UUID uuid) {
        joinTimes.put(uuid, System.currentTimeMillis());
    }

    public void removePlayer(UUID uuid) {
        joinTimes.remove(uuid);
    }

    public boolean isInGracePeriod(UUID uuid) {
        long delay = plugin.getConfiguration().joinSendDelay() * 1000L;
        return joinTimes.containsKey(uuid) && System.currentTimeMillis() < joinTimes.get(uuid) + delay;
    }

    public long getRemainingGracePeriod(UUID uuid) {
        return joinTimes.get(uuid) + (plugin.getConfiguration().joinSendDelay() * 1000L) - System.currentTimeMillis();
    }
}
