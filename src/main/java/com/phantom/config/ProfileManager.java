/*
 * ProfileManager.java — Manages 4 custom profile slots.
 *
 * Each slot has a user-editable name and stores a full config snapshot.
 * Profile names are persisted in phantom-profile-names.properties.
 * Profile data is stored in phantom-profiles/slot_0.properties through slot_3.properties.
 */
package com.phantom.config;

import com.phantom.PhantomMod;
import com.phantom.module.Module;
import com.phantom.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ProfileManager {
    public static final int SLOT_COUNT = 4;

    private static final String[] DEFAULT_NAMES = {"Profile 1", "Profile 2", "Profile 3", "Profile 4"};
    private static final String[] profileNames = new String[SLOT_COUNT];
    private static boolean namesLoaded = false;

    private ProfileManager() {
    }

    private static void ensureNamesLoaded() {
        if (namesLoaded) return;
        namesLoaded = true;
        Properties nameProps = new Properties();
        Path path = getNameFilePath();
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                nameProps.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            profileNames[i] = nameProps.getProperty("slot." + i + ".name", DEFAULT_NAMES[i]);
        }
    }

    private static void saveNames() {
        Properties nameProps = new Properties();
        for (int i = 0; i < SLOT_COUNT; i++) {
            nameProps.setProperty("slot." + i + ".name", profileNames[i]);
        }
        Path path = getNameFilePath();
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream os = Files.newOutputStream(path)) {
                nameProps.store(os, "PhantomMod profile names");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProfileName(int slot) {
        ensureNamesLoaded();
        if (slot < 0 || slot >= SLOT_COUNT) return "Unknown";
        return profileNames[slot];
    }

    public static void setProfileName(int slot, String name) {
        ensureNamesLoaded();
        if (slot < 0 || slot >= SLOT_COUNT) return;
        profileNames[slot] = (name == null || name.isBlank()) ? DEFAULT_NAMES[slot] : name;
        saveNames();
    }

    public static void saveSlot(int slot, ModuleManager moduleManager) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        Properties properties = ConfigManager.capture(moduleManager);
        ConfigManager.writeProfile("slot_" + slot, properties);
        PhantomMod.saveConfig();
    }

    public static boolean loadSlot(int slot, ModuleManager moduleManager) {
        if (slot < 0 || slot >= SLOT_COUNT) return false;
        Properties properties = ConfigManager.readProfile("slot_" + slot);
        if (properties.isEmpty()) return false;
        ConfigManager.apply(moduleManager, properties, true);
        return true;
    }

    // Legacy compatibility: migrate old "custom" profile to slot 0 on first access
    static {
        Path legacyPath = FabricLoader.getInstance().getConfigDir()
                .resolve("phantom-profiles").resolve("custom.properties");
        Path slot0Path = FabricLoader.getInstance().getConfigDir()
                .resolve("phantom-profiles").resolve("slot_0.properties");
        if (Files.exists(legacyPath) && !Files.exists(slot0Path)) {
            try {
                Files.copy(legacyPath, slot0Path);
            } catch (IOException ignored) {
            }
        }
    }

    private static Path getNameFilePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("phantom-profile-names.properties");
    }
}
