package com.phantom.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {
    private static final long LIFETIME_MS = 1500L;
    private static final List<Notification> NOTIFICATIONS = new ArrayList<>();

    private NotificationManager() {
    }

    public static void push(String message) {
        NOTIFICATIONS.add(new Notification(message, System.currentTimeMillis() + LIFETIME_MS));
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Notification> iterator = NOTIFICATIONS.iterator();
        int y = 12;

        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            if (now >= notification.expiresAt()) {
                iterator.remove();
                continue;
            }

            int textWidth = mc.font.width(notification.message());
            int x = 10;
            graphics.fill(x - 5, y - 5, x + textWidth + 8, y + 13, 0xD0101010);
            graphics.drawString(mc.font, notification.message(), x, y, 0xFFFFFFFF, true);
            y += 18;
        }
    }

    private record Notification(String message, long expiresAt) {
    }
}
