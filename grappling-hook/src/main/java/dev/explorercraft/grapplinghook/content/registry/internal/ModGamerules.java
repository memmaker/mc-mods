package dev.explorercraft.grapplinghook.content.registry.internal;

import dev.explorercraft.grapplinghook.GrappleMod;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * 26.2 turned gamerules into registered objects and moved the values onto the server, so the rule
 * is built through Fabric's builder and read through MinecraftServer rather than off the level.
 */
public class ModGamerules {

    public static final GameRule<Boolean> USE_LIMITED_HOOK = GameRuleBuilder
            .forBoolean(false)
            .category(GameRuleCategory.PLAYER)
            .buildAndRegister(GrappleMod.id("use_limited_hook"));

    /** False on the client, where the rule's value is not available; hooks are server-authoritative. */
    public static boolean useLimitedHook(Level level) {
        return level.getServer() != null
                && Boolean.TRUE.equals(level.getServer().getGameRules().get(USE_LIMITED_HOOK));
    }

    // Run to ensure these are loaded.
    public static void bump() {}

}
