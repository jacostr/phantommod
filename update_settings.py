import re

with open("src/main/java/com/phantom/gui/ModuleSettingsScreen.java", "r") as f:
    lines = f.readlines()

new_lines = []
in_render = False
for line in lines:
    if "private void addToggle" in line:
        new_lines.append('    private void addToggle(int x, int y, String label, boolean enabled, java.util.function.Consumer<Boolean> consumer) {\n')
        new_lines.append('        components.add(new ModernToggle(x, y, PANEL_WIDTH - 40, 20, label, enabled, consumer));\n')
        new_lines.append('    }\n')
        continue
    if "components.add(new ModernToggle" in line and "x + PANEL_WIDTH - 85" in line:
        continue
    if "// We'll draw the label manually" in line:
        continue
    
    if "// Draw Toggle Labels" in line:
        in_render = True
        continue
    
    if in_render and "}" in line and "public void render" not in line: # wait, this is too fragile
        pass

