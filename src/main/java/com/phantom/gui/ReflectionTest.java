package com.phantom.gui;
import net.minecraft.client.gui.screens.Screen;
import java.lang.reflect.Method;
public class ReflectionTest {
    public static void main(String[] args) {
        for (Method m : Screen.class.getMethods()) {
            if (m.getName().contains("mouse") || m.getName().contains("key") || m.getName().contains("char")) {
                System.out.println(m.toString());
            }
        }
    }
}
