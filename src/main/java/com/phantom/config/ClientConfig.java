package com.phantom.config;

import org.lwjgl.glfw.GLFW;

import java.util.Properties;

/**
 * Global client options stored alongside module keys in the main properties file.
 */
public final class ClientConfig {
    public static final String KEY_PANIC = "phantom.panic_key";
    public static final String KEY_MODULE_GUIDE = "phantom.module_guide_key";

    private static int panicKeyGlfw = GLFW.GLFW_KEY_END;
    private static int moduleGuideKeyGlfw = GLFW.GLFW_KEY_INSERT;

    private ClientConfig() {
    }

    public static int getPanicKeyGlfw() {
        return panicKeyGlfw;
    }

    public static void setPanicKeyGlfw(int glfwKey) {
        panicKeyGlfw = glfwKey;
    }

    public static int getModuleGuideKeyGlfw() {
        return moduleGuideKeyGlfw;
    }

    public static void setModuleGuideKeyGlfw(int glfwKey) {
        moduleGuideKeyGlfw = glfwKey;
    }

    public static void loadFrom(Properties properties) {
        String v = properties.getProperty(KEY_PANIC);
        if (v != null) {
            try {
                panicKeyGlfw = Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        String g = properties.getProperty(KEY_MODULE_GUIDE);
        if (g != null) {
            try {
                moduleGuideKeyGlfw = Integer.parseInt(g.trim());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static void saveTo(Properties properties) {
        properties.setProperty(KEY_PANIC, Integer.toString(panicKeyGlfw));
        properties.setProperty(KEY_MODULE_GUIDE, Integer.toString(moduleGuideKeyGlfw));
    }
}
