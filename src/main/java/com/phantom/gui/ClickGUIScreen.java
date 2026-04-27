package com.phantom.gui;

import com.phantom.PhantomMod;
import com.phantom.gui.framework.*;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Completely remade ClickGUI with a premium liquid glassy aesthetic.
 * Features a sidebar-based category navigation and custom custom-built widgets.
 */
public class ClickGUIScreen extends Screen {
    private int guiWidth;
    private int guiHeight;
    private static final int SIDEBAR_WIDTH = 100;
    private static final int PADDING = 10;
    
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private String searchText = "";
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private final List<BaseComponent> components = new ArrayList<>();
    private final List<BaseComponent> sidebarComponents = new ArrayList<>();
    private ModernTextField searchField;

    public ClickGUIScreen() {
        super(Component.literal("PhantomMod"));
    }

    @Override
    protected void init() {
        // Ensure at least 20px padding from screen edges
        this.guiWidth = Math.min(this.width - 40, (int)(this.width * 0.8));
        this.guiHeight = Math.min(this.height - 40, (int)(this.height * 0.8));
        rebuildUI();
    }

    private void rebuildUI() {
        components.clear();
        sidebarComponents.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int guiLeft = centerX - guiWidth / 2;
        int guiTop = centerY - guiHeight / 2;

        // --- Sidebar ---
        int sidebarY = guiTop + 45;
        for (ModuleCategory category : ModuleCategory.values()) {
            sidebarComponents.add(new ModernButton(guiLeft + 5, sidebarY, SIDEBAR_WIDTH - 10, 20, 
                Component.literal(category.getLabel()), 
                btn -> {
                    selectedCategory = category;
                    scrollOffset = 0;
                    rebuildUI();
                }));
            sidebarY += 24;
        }

        // Sidebar Bottom Profiles Button
        int bottomY = guiTop + guiHeight - 30;
        sidebarComponents.add(new ModernButton(guiLeft + 5, bottomY, SIDEBAR_WIDTH - 10, 20, 
            Component.literal("Profiles"), 
            btn -> this.minecraft.setScreen(new ProfileScreen(this, PhantomMod.getModuleManager(), false))));

        // --- Main Area ---
        int mainX = guiLeft + SIDEBAR_WIDTH + PADDING;
        int mainWidth = guiWidth - SIDEBAR_WIDTH - PADDING * 2;

        boolean wasFocused = searchField != null && searchField.isFocused();
        searchField = new ModernTextField(mainX, guiTop + 10, mainWidth, 20, "Search modules...", text -> {
            this.searchText = text;
            this.scrollOffset = 0;
            rebuildUI();
        });
        searchField.setText(searchText);
        searchField.setFocused(wasFocused);
        components.add(searchField);

        // Module List
        String q = searchText.trim().toLowerCase(Locale.ROOT);
        List<Module> modules = PhantomMod.getModuleManager().getModules().stream()
                .filter(m -> q.isEmpty() ? m.getCategory() == selectedCategory : true)
                .filter(m -> q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());

        int moduleY = guiTop + 40 - scrollOffset;
        for (Module module : modules) {
            if (moduleY + 24 < guiTop + 40 || moduleY > guiTop + guiHeight - 10) {
                moduleY += 28;
                continue;
            }

            // Module Toggle
            components.add(new ModernToggle(mainX, moduleY, 40, 20, "", module.isEnabled(), val -> {
                module.toggle();
            }));

            // Settings Button (if applicable)
            if (module.hasConfigurableSettings()) {
                ModernButton settingsBtn = new ModernButton(mainX + mainWidth - 40, moduleY, 40, 20, 
                    Component.literal("≡"), 
                    btn -> this.minecraft.setScreen(module.createSettingsScreen(this)));
                settingsBtn.setTextScale(1.4f);
                components.add(settingsBtn);
            }

            moduleY += 28;
        }
        
        maxScroll = Math.max(0, (modules.size() * 28) - (guiHeight - 50));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int guiLeft = centerX - guiWidth / 2;
        int guiTop = centerY - guiHeight / 2;

        // Draw Liquid Glassy Backgrounds
        // Sidebar
        RenderUtil.drawGlassPanel(graphics, guiLeft, guiTop, SIDEBAR_WIDTH, guiHeight, 0x80101010, 0x30FFFFFF);
        
        // Main Panel
        RenderUtil.drawGlassPanel(graphics, guiLeft + SIDEBAR_WIDTH, guiTop, guiWidth - SIDEBAR_WIDTH, guiHeight, 0x60050505, 0x20FFFFFF);

        // Draw Logo / Title
        FontDescription cleanFont = new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "uniform"));
        graphics.pose().pushMatrix();
        graphics.pose().translate(guiLeft + 50, guiTop + 12);
        graphics.pose().scale(1.2f, 1.2f);
        graphics.drawCenteredString(this.font, Component.literal("PHANTOM MOD").withStyle(s -> s.withFont(cleanFont)), 0, 0, 0xFFA8E6A3);
        graphics.pose().popMatrix();
        
        graphics.pose().pushMatrix();
        graphics.pose().translate(guiLeft + 50, guiTop + 24);
        graphics.pose().scale(0.6f, 0.6f);
        graphics.drawCenteredString(this.font, Component.literal("v0.8.0 Premium").withStyle(s -> s.withFont(cleanFont)), 0, 0, 0xFF888888);
        graphics.pose().popMatrix();

        // Render Sidebar Components
        for (BaseComponent component : sidebarComponents) {
            component.render(graphics, mouseX, mouseY, delta);
        }

        // Render Main Components
        for (BaseComponent component : components) {
            component.render(graphics, mouseX, mouseY, delta);
        }

        // Draw Scroll Bar
        if (maxScroll > 0) {
            int scrollBarX = guiLeft + guiWidth - 8;
            int scrollBarY = guiTop + 40;
            int scrollBarHeight = guiHeight - 50;
            
            // Track
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, 0x40000000);
            
            // Thumb
            int thumbHeight = Math.max(20, (int) ((double) (guiHeight - 50) * (guiHeight - 50) / (maxScroll + guiHeight - 50)));
            int thumbY = scrollBarY + (int) ((double) scrollOffset / maxScroll * (scrollBarHeight - thumbHeight));
            graphics.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0xFFA8E6A3);
        }
        
        // Render Module Names
        String q = searchText.trim().toLowerCase(Locale.ROOT);
        List<Module> modules = PhantomMod.getModuleManager().getModules().stream()
                .filter(m -> q.isEmpty() ? m.getCategory() == selectedCategory : true)
                .filter(m -> q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());

        int textY = guiTop + 46 - scrollOffset;
        for (Module module : modules) {
            if (textY + 10 >= guiTop + 40 && textY <= guiTop + guiHeight - 10) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(guiLeft + SIDEBAR_WIDTH + PADDING + 55, textY);
                graphics.pose().scale(1.1f, 1.1f);
                graphics.drawString(this.font, Component.literal(module.getName()).withStyle(s -> s.withFont(cleanFont)), 0, 0, 0xFFFFFFFF, false);
                graphics.pose().popMatrix();
            }
            textY += 28;
        }

        NotificationManager.render(graphics);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isFocused) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (BaseComponent component : sidebarComponents) {
            if (component.mouseClicked(mouseX, mouseY, button)) return true;
        }
        for (BaseComponent component : components) {
            if (component.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(event, isFocused);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (BaseComponent component : components) {
            component.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (searchField.keyPressed(event.key(), event.scancode(), event.modifiers())) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (searchField.charTyped((char) event.codepoint(), 0)) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int nextOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(scrollY * 20)));
        if (nextOffset != scrollOffset) {
            scrollOffset = nextOffset;
            rebuildUI();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
