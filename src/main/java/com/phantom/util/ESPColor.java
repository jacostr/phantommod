package com.phantom.util;

public enum ESPColor {
    RED("Red", 0xFFFF0000),
    GREEN("Green", 0xFF00FF00),
    BLUE("Blue", 0xFF0000FF),
    YELLOW("Yellow", 0xFFFFFF00),
    CYAN("Cyan", 0xFF00FFFF),
    MAGENTA("Magenta", 0xFFFF00FF),
    ORANGE("Orange", 0xFFFFA500),
    PINK("Pink", 0xFFFF69B4),
    PURPLE("Purple", 0xFF800080),
    WHITE("White", 0xFFFFFFFF);

    private final String label;
    private final int color;

    ESPColor(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }

    public ESPColor next() {
        ESPColor[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static ESPColor fromString(String name, ESPColor def) {
        if (name == null) return def;
        try {
            return ESPColor.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
