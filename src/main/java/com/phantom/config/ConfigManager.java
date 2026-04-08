/*
 * ConfigManager.java — Reads and writes the phantom-memory.properties config file.
 *
 * This is the single source of truth for persisting all module states, hotkeys,
 * slider values, and global settings across game restarts. It uses Java's built-in
 * Properties format (key=value) intentionally — zero external dependencies, human-
 * readable, and crash-safe since we write to a fresh OutputStream each time.
 *
 * Located at: .minecraft/config/phantom-memory.properties
 */
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

/**
 * Manages reading and writing of {@code phantom-memory.properties} in the Minecraft config folder.
 *
 * <p>The file uses Java's built-in {@link Properties} format — plain {@code key=value} pairs,
 * one per line. This was chosen because it requires zero third-party libraries, is human-readable,
 * and survives game crashes since writes are done to a fresh {@link OutputStream} each time
 * (no in-place mutation that can leave a corrupt file).</p>
 *
 * <p>Layout convention: each module gets a namespace prefix derived from its name
 * (lowercased, non-alphanumeric chars replaced with {@code _}).
 * For example: {@code aimassist.enabled=true}, {@code aimassist.key=-1}.</p>
 */
public final class ConfigManager {
    /** File name inside the Fabric config directory (.minecraft/config/). */
    private static final String FILE_NAME = "phantom-memory.properties";

    // Static utility class — no instances needed.
    private ConfigManager() {
    }

    /**
     * Loads all settings from disk into the module registry.
     * Called once at startup by {@link ModuleManager} constructor after all modules are created.
     *
     * <p>The load order is: global settings (ClientConfig) first, then each module in
     * registration order. Modules that find no matching keys keep their default values.</p>
     */
    public static void load(ModuleManager moduleManager) {
        Properties properties = readMainFile();
        // Each module reads its own slice of the properties file.
        for (Module module : moduleManager.getModules()) {
            module.loadConfig(properties);
        }
    }

    /**
     * Writes all current settings to disk. Called after any setting change via
     * {@link com.phantom.module.Module#saveConfig()}, which delegates up to
     * {@link com.phantom.PhantomMod#saveConfig()}.
     *
     * <p>We rebuild the entire Properties object from scratch on every save — this
     * automatically drops any stale keys for deleted modules.</p>
     */
    public static void save(ModuleManager moduleManager) {
        Properties properties = new Properties();
        for (Module module : moduleManager.getModules()) {
            module.saveConfig(properties);
        }
        writeMainFile(properties);
    }

    /**
     * Reads the config file into a {@link Properties} object.
     * Returns an empty Properties instance (not null) if the file doesn't exist yet —
     * this is normal on first launch and all modules will use their defaults.
     */
    private static Properties readMainFile() {
        Properties properties = new Properties();
        Path path = getMainConfigPath();
        if (Files.exists(path)) {
            try (InputStream inputStream = Files.newInputStream(path)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return properties;
    }

    /**
     * Writes the given Properties to disk, creating parent directories as needed.
     * The {@code store()} call adds a timestamp comment at the top of the file automatically.
     */
    private static void writeMainFile(Properties properties) {
        Path path = getMainConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                properties.store(outputStream, "PhantomMod saved settings");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /** Resolves the config file path using Fabric's config directory (platform-agnostic). */
    private static Path getMainConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
