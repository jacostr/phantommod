package com.phantom.util;

import net.minecraft.client.Minecraft;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String PREFIX = "[PhantomMod] ";
    private static boolean debugEnabled = false;
    private static boolean fileLoggingEnabled = false;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static void setFileLoggingEnabled(boolean enabled) {
        fileLoggingEnabled = enabled;
    }

    public static void warn(String msg) {
        log("WARN: " + msg, false);
    }

    public static void info(String msg) {
        log("INFO: " + msg, false);
    }

    public static void error(String msg) {
        log("ERROR: " + msg, true);
    }

    public static void error(String msg, Throwable t) {
        log("ERROR: " + msg, true);
        if (debugEnabled || fileLoggingEnabled) {
            if (debugEnabled) t.printStackTrace(System.err);
            if (fileLoggingEnabled) logToFile("ERROR: " + msg + "\n" + stackTraceToString(t));
        }
    }

    private static void log(String msg, boolean isError) {
        String formatted = PREFIX + msg;
        if (debugEnabled) {
            if (isError) System.err.println(formatted);
            else System.out.println(formatted);
        }
        if (fileLoggingEnabled) {
            logToFile(msg);
        }
    }

    private static void logToFile(String msg) {
        try {
            File logFile = new File(Minecraft.getInstance().gameDirectory, "phantom.log");
            try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
                out.println("[" + LocalDateTime.now().format(formatter) + "] " + msg);
            }
        } catch (Exception ignored) {}
    }

    private static String stackTraceToString(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}