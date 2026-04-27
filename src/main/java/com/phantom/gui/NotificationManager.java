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
import com.phantom.util.RenderUtil;
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
    public enum Position {
        TOP_LEFT("Top Left"),
        TOP_RIGHT("Top Right"),
        TOP_CENTER("Top Center"),
        BOTTOM_LEFT("Bottom Left"),
        BOTTOM_RIGHT("Bottom Right"),
        BOTTOM_CENTER("Bottom Center");

        private final String label;
        Position(String label) { this.label = label; }
        public String getLabel() { return label; }
        public Position next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final long LIFETIME_MS = 1500L;
    private static final List<Notification> NOTIFICATIONS = new ArrayList<>();
    private static Position currentPosition = Position.TOP_LEFT;

    private NotificationManager() {
    }

    public static Position getPosition() { return currentPosition; }
    public static void setPosition(Position pos) { currentPosition = pos; }
    public static void cyclePosition() { currentPosition = currentPosition.next(); }

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null || NOTIFICATIONS.isEmpty()) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int startY;
        boolean isBottom = currentPosition.name().startsWith("BOTTOM");
        
        if (isBottom) {
            startY = sh - 25 - (currentPosition == Position.BOTTOM_CENTER ? 30 : 0);
        } else {
            startY = 12;
        }

        render(graphics, sw, sh, startY, isBottom);
    }

    private static void render(GuiGraphics graphics, int sw, int sh, int startY, boolean isBottom) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        Iterator<Notification> iterator = NOTIFICATIONS.iterator();
        int y = startY;

        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            if (now >= notification.expiresAt()) {
                iterator.remove();
                continue;
            }

            net.minecraft.network.chat.FontDescription cleanFont = new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "uniform"));
            net.minecraft.network.chat.Component styledMsg = net.minecraft.network.chat.Component.literal(notification.message()).withStyle(s -> s.withFont(cleanFont));
            int textWidth = mc.font.width(styledMsg);
            
            int x;
            if (currentPosition.name().contains("RIGHT")) {
                x = sw - textWidth - 15;
            } else if (currentPosition.name().contains("CENTER")) {
                x = (sw - textWidth) / 2;
            } else {
                x = 10;
            }
            
            // Glassy Notification Panel
            RenderUtil.drawGlassPanel(graphics, x - 5, y - 5, textWidth + 12, 18, 0xA0101010, 0x40A8E6A3);
            graphics.drawString(mc.font, styledMsg, x, y, 0xFFFFFFFF, false);
            
            if (isBottom) {
                y -= 22;
            } else {
                y += 22;
            }
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
