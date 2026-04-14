/*
 * NotificationManager.java — Lightweight toast notification system.
 *
 * Displays short text banners in the top-left of the screen for 1.5 seconds.
 * Used to confirm module toggles and config saves.
 * Rendered on both the HUD layer and over GUI screens so messages are always visible.
 */
package com.phantom.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lightweight toast-notification system that displays short text banners
 * in the top-left of the HUD for a fixed duration then fades them out.
 *
 * <p>Notifications are pushed from any thread (module enable/disable happens on the
 * game thread via the tick event) but rendered exclusively on the render thread via
 * the HUD and GUI render hooks. Since both push and render run on the same game thread
 * in practice, no synchronisation is needed.</p>
 *
 * <p>The manager is statically accessible so any module or GUI screen can fire a
 * notification without holding a reference to a screen or manager instance.</p>
 */
public final class NotificationManager {
    private static final long LIFETIME_MS = 1500L;
    private static final List<Notification> NOTIFICATIONS = new ArrayList<>();

    private NotificationManager() {
    }

    /**
     * Queues a new notification that will be visible for {@link #LIFETIME_MS} ms.
     * Called from module toggle, config save, etc.
     *
     * @param message Short one-line string to display (keep under ~40 chars so it fits).
     */
    public static void push(String message) {
        NOTIFICATIONS.add(new Notification(message, System.currentTimeMillis() + LIFETIME_MS));
    }

    /**
     * Renders all currently active notifications and removes expired ones.
     *
     * <p>Called from both {@code HudRenderCallback} (in-world HUD) and directly
     * from each GUI screen's {@code render()} method so notifications appear even
     * when a menu is open.</p>
     *
     * <p>Layout: each notification is a dark semi-transparent box with white text,
     * stacked vertically from y=12 downward with 18px per entry.</p>
     */
    public static void render(GuiGraphics graphics) {
        render(graphics, 10, 12);
    }

    public static void render(GuiGraphics graphics, int startX, int startY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Notification> iterator = NOTIFICATIONS.iterator();
        int y = startY;

        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            if (now >= notification.expiresAt()) {
                iterator.remove();
                continue;
            }

            int textWidth = mc.font.width(notification.message());
            int x = startX;
            graphics.fill(x - 5, y - 5, x + textWidth + 8, y + 13, 0xD0101010);
            graphics.drawString(mc.font, notification.message(), x, y, 0xFFFFFFFF, true);
            y += 18;
        }
    }

    /**
     * Immutable data container for one notification entry.
     *
     * @param message   The string to render.
     * @param expiresAt Absolute epoch-ms timestamp when this toast should be removed.
     */
    private record Notification(String message, long expiresAt) {
    }
}
