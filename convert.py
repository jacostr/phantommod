import re

with open("old_ModuleSettingsScreen.java", "r") as f:
    code = f.read()

# 1. Replace PhantomSlider -> ModernSlider
code = code.replace("this.addRenderableWidget(new PhantomSlider", "components.add(new ModernSlider")
code = code.replace("import com.phantom.gui.widget.PhantomSlider;", "")

# 2. Replace net.minecraft.client.gui.components.* imports
code = re.sub(r'import net\.minecraft\.client\.gui\.components\..*;\n', '', code)

# 3. Import framework
code = code.replace("import com.phantom.module.Module;", "import com.phantom.module.Module;\nimport com.phantom.gui.framework.*;\nimport com.phantom.util.RenderUtil;")

# 4. Replace Button.builder
# Pattern: Button.builder( TITLE , ACTION ).bounds( X, Y, W, H ).build()
# This regex is tricky because ACTION can be a multi-line lambda block.
# It's easier to just use a custom helper method: ModernButton.create(TITLE, ACTION).bounds(X,Y,W,H).build()
# Let's replace `Button.builder(` with `ModernButtonBuilder.create(`
# And we'll provide a dummy ModernButtonBuilder class inside ModuleSettingsScreen!

code = code.replace("Button.builder(", "ModernButtonBuilder.create(")

# 5. CycleButton builder (used in ESP)
# this.addRenderableWidget(net.minecraft.client.gui.components.CycleButton.builder(...)
code = code.replace("net.minecraft.client.gui.components.CycleButton.builder", "ModernCycleBuilder.create")

# 6. We need to replace the class fields and methods to match the new glassy style
class_header = r"public class ModuleSettingsScreen extends Screen \{"
new_class_content = """public class ModuleSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 4;
    private static final int TEXT_SPACING = 10;

    private final Screen parent;
    private final Module module;
    private final java.util.List<BaseComponent> components = new java.util.ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    private void addRenderableWidget(BaseComponent component) {
        components.add(component);
    }
    
    // ModernButtonBuilder helper
    public static class ModernButtonBuilder {
        private Component title;
        private java.util.function.Consumer<ModernButton> action;
        private int x, y, w, h;
        public static ModernButtonBuilder create(Component title, java.util.function.Consumer<ModernButton> action) {
            ModernButtonBuilder b = new ModernButtonBuilder();
            b.title = title;
            b.action = action;
            return b;
        }
        public ModernButtonBuilder bounds(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h; return this;
        }
        public ModernButton build() {
            return new ModernButton(x, y, w, h, title, action);
        }
    }
    
    // ModernCycleBuilder helper (stub to avoid compilation errors)
    public static class ModernCycleBuilder {
        public static Object create(Object a, Object b) { return new Object(); }
    }

    @Override
    protected void init() {
        components.clear();
        int centerX = this.width / 2;
        int y = 54 - scrollOffset;

        // Back button
        components.add(new ModernButton(centerX - PANEL_WIDTH/2 + 10, 10, 50, 20, Component.literal("<- Back"), b -> this.minecraft.setScreen(parent)));

"""

# Regex to replace from `public class ModuleSettingsScreen extends Screen {` down to the start of `init()` body
code = re.sub(r'public class ModuleSettingsScreen extends Screen \{.*?\n    protected void init\(\) \{.*?\n        int y = 54 - scrollOffset;\n', new_class_content, code, flags=re.DOTALL)

with open("new_ModuleSettingsScreen.java", "w") as f:
    f.write(code)

