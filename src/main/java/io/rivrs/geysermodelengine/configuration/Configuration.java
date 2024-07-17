package io.rivrs.geysermodelengine.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import lombok.Data;

@Data
public class Configuration {

    private final Path path;
    private YamlConfiguration configuration;

    public int dataSendDelay() {
        return this.configuration.getInt("data-send-delay", 0);
    }

    public int viewDistance() {
        return this.configuration.getInt("entity-view-distance", 60);
    }

    public int joinSendDelay() {
        return this.configuration.getInt("join-send-delay", 20);
    }

    public int maximumModels() {
        return this.configuration.getInt("maximum-models", 10);
    }

    public EntityType modelEntityType() {
        try {
            return EntityType.valueOf(this.configuration.getString("model-entity-type", "BAT"));
        } catch (IllegalArgumentException e) {
            return EntityType.BAT;
        }
    }

    public void load() {
        if (!Files.exists(path))
            loadDefault();

        this.configuration = YamlConfiguration.loadConfiguration(path.toFile());
    }

    public void loadDefault() {
        if (!Files.isDirectory(path.getParent()))
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new RuntimeException("Failed to create configuration directory", e);
            }

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            if (inputStream == null)
                throw new RuntimeException("Default configuration not found");
            Files.copy(inputStream, path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load default configuration", e);
        }
    }
}
