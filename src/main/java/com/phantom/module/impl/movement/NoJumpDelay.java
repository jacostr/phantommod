/*
 * Ghost: clears vanilla jump cooldown each tick on the client player (snappier bunny hops).
 */
package com.phantom.module.impl.movement;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;

public class NoJumpDelay extends Module {
    private static NoJumpDelay instance;

    public NoJumpDelay() {
        super("No Jump Delay", "Removes the short client jump cooldown so repeated jumps feel instant.", ModuleCategory.GHOST, -1);
        instance = this;
    }

    public static boolean isModuleEnabled() {
        return instance != null && instance.isEnabled();
    }
}
