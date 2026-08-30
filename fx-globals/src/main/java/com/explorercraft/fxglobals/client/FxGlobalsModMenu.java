package com.explorercraft.fxglobals.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Puts the config button next to FX Globals in Mod Menu's list. */
public class FxGlobalsModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return FxGlobalsConfigScreen::new;
	}
}
