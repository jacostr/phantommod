package com.phantom.config;

import com.phantom.module.Module;
import com.phantom.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigManager {
    private static final String FILE_NAME = "phantom-memory.properties";

    private ConfigManager() {
    }

    public static void load(ModuleManager moduleManager) {
        Properties properties = new Properties();
        Path path = getConfigPath();

        if (Files.exists(path)) {
            try (InputStream inputStream = Files.newInputStream(path)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        for (Module module : moduleManager.getModules()) {
            module.loadConfig(properties);
        }
    }

    public static void save(ModuleManager moduleManager) {
        Properties properties = new Properties();

        for (Module module : moduleManager.getModules()) {
            module.saveConfig(properties);
        }

        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                properties.store(outputStream, "PhantomMod saved settings");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
