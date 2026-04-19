package com.phantom.util;

import net.minecraft.client.Minecraft;

public class Logger {
    private static final String PREFIX = "[PhantomMod] ";

    public static void warn(String msg) {
        System.out.println(PREFIX + "WARN: " + msg);
    }

    public static void info(String msg) {
        System.out.println(PREFIX + "INFO: " + msg);
    }

    public static void error(String msg) {
        System.err.println(PREFIX + "ERROR: " + msg);
    }

    public static void error(String msg, Throwable t) {
        System.err.println(PREFIX + "ERROR: " + msg);
        t.printStackTrace(System.err);
    }
}