package dev.explorercraft.grapplinghook;

import dev.explorercraft.grapplinghook.config.GrapplePropertyConfigLoader;
import dev.explorercraft.grapplinghook.content.command.GrappleModCommand;
import dev.explorercraft.grapplinghook.config.GrappleModCommonConfig;
import dev.explorercraft.grapplinghook.config.ServerFeatures;
import dev.explorercraft.grapplinghook.config.pack.DataPackProcessor;
import dev.explorercraft.grapplinghook.content.registry.CustomizationCategories;
import dev.explorercraft.grapplinghook.content.registry.CustomizationProperties;
import dev.explorercraft.grapplinghook.content.registry.internal.*;
import dev.explorercraft.grapplinghook.network.NetworkManager;
import dev.explorercraft.grapplinghook.physics.ServerPhysicsObserver;
import dev.explorercraft.grapplinghook.physics.persistence.HookPersistenceManager;
import dev.explorercraft.grapplinghook.util.GrappleModUtils;
import dev.explorercraft.grapplinghook.util.scheduling.Ticker;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Optional;

/*
 * This file is part of GrappleMod.

    GrappleMod is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GrappleMod is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GrappleMod.  If not, see <http://www.gnu.org/licenses/>.
 */
public class GrappleMod implements ModInitializer {

    public static final String MOD_ID = "grapplinghook";
    public static final Logger LOGGER = LogManager.getLogger();

    private static MinecraftServer currentServerInstance = null;
    private static GrappleMod instance = null;

    private ServerFeatures serverFeatures;
    private Ticker ticker;
    private ServerPhysicsObserver serverPhysicsObserver;

    @Override
    public void onInitialize() {
        instance = this;

        this.ticker = new Ticker();

        try {
            this.initConfig();
        } catch (Exception e) {
            LOGGER.info(e);
        }

        this.serverFeatures = new ServerFeatures();

        ModDataComponents.bump();


        ModItems.registerAllItems();
        ModEntities.registerAllEntities();
        ModAdvancementTriggers.registerAllTriggers();
        ModRecipeSerializers.registerAll();

        CustomizationProperties.registerAll();
        CustomizationCategories.registerAll();

        ModTags.bump();
        ModGamerules.bump();

        this.queueCommandRegistration();

        NetworkManager.registerAll();

        this.serverPhysicsObserver = new ServerPhysicsObserver();

        this.registerDataPacks();

        ServerTickEvents.START_SERVER_TICK.register(this.ticker::tick);
        ServerTickEvents.END_SERVER_TICK.register(HookPersistenceManager::tickServer);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServerInstance = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HookPersistenceManager.clearAll();
            currentServerInstance = null;
        });
    }

    private void initConfig() {
        GrapplePropertyConfigLoader.load();
        GrappleModCommonConfig.HANDLER.defaults().saveDefaults();
        GrappleModCommonConfig.HANDLER.load();

        GrappleModCommonConfig.resetConfigFromServer();
    }

    private void queueCommandRegistration() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if(FabricLoader.getInstance().isDevelopmentEnvironment())
                dispatcher.register(GrappleModCommand.build());
        });
    }

    public void registerDataPacks() {
        GrappleMod.LOGGER.info("Re-assigning datapack reload listener...");

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new DataPackProcessor());

        // ponytail: the four bundled variant packs (classic textures, simplified, classic
        // recipes, hook-only) went with the port; they were written for the pre-1.21.4 asset
        // layout. Re-add as real packs if anyone asks.
    }

    public ServerPhysicsObserver getServerPhysicsObserver() {
        return this.serverPhysicsObserver;
    }

    public Ticker getTicker() {
        return this.ticker;
    }

    public ServerFeatures getServerFeatures() {
        return this.serverFeatures;
    }

    public static GrappleMod get() {
        return instance;
    }

    public static MinecraftServer getServer() {
        return currentServerInstance;
    }

    public static Identifier id(String id) {
        return Identifier.fromNamespaceAndPath(MOD_ID, id);
    }

    /** @deprecated This just seems like a bad idea & a hack. */
    @Deprecated(since = "mc 1.21.1")
    public static Identifier vanillaId(String id) {
        return Identifier.fromNamespaceAndPath("minecraft", id);
    }

    public static Path getDefaultConfigPath() {
        return YACLPlatform.getConfigDir();
    }
}
