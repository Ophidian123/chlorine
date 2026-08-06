package com.chlorine.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registered under the "modmenu" entrypoint key in fabric.mod.json. This
 * class is only ever classloaded if Mod Menu itself is installed and asks
 * Fabric Loader for entrypoints under that key — Fabric Loader's core
 * doesn't eagerly load classes for entrypoint keys it doesn't recognize
 * itself, so declaring this is safe even for players without Mod Menu.
 * Without Mod Menu, Chlorine still works fine — you'd just edit
 * config/chlorine.json directly instead of using a "Config" button.
 */
public class ChlorineModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ChlorineConfigScreenBuilder::build;
    }
}
