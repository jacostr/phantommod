/*
 * NotificationManager.java — Lightweight toast notification system.
 *
 * Displays short text banners in the top-left of the screen for 1.5 seconds.
 * Used to confirm module toggles, config saves, and panic-key activation.
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
    /** How long each toast stays visible, in milliseconds. */
    private static final long LIFETIME_MS = 1500L;

    /** Active notifications ordered oldest-first; iterated and expired every render frame. */
    private static final List<Notification> NOTIFICATIONS = new ArrayList<>();

    // Static utility — prevent instantiation.
    private NotificationManager() {
    }

    /**
     * Queues a new notification that will be visible for {@link #LIFETIME_MS} ms.
     * Called from module toggle, config save, panic, etc.
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null) {
            return; // F1 mode or font not loaded yet.
        }

        long now = System.currentTimeMillis();
        Iterator<Notification> iterator = NOTIFICATIONS.iterator();
        int y = 12; // Start distance from the top edge of the screen.

        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            if (now >= notification.expiresAt()) {
                // Toast expired — remove and skip rendering. Iterator-safe removal.
                iterator.remove();
                continue;
            }

            // Draw a dark pill behind the text so it's readable over any background.
            int textWidth = mc.font.width(notification.message());
            int x = 10;
            graphics.fill(x - 5, y - 5, x + textWidth + 8, y + 13, 0xD0101010);
            // White text with drop-shadow for readability.
            graphics.drawString(mc.font, notification.message(), x, y, 0xFFFFFFFF, true);
            y += 18; // Move down for the next notification.
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
