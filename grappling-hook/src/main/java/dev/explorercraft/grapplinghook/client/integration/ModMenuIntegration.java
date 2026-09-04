package dev.explorercraft.grapplinghook.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.explorercraft.grapplinghook.config.ConfigUI;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigUI::buildModMenuConfig;
    }

}
