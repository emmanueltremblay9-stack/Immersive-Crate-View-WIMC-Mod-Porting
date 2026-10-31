package com.emmanueltremblay.wimc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Wimc.MOD_ID)
public final class Wimc {
    public static final String MOD_ID = "immersive_crate_view_wimc_mod_porting";

    public Wimc(IEventBus modBus, ModContainer modContainer, Dist dist) {
        if (dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, WimcClientConfig.SPEC);
        }
    }
}
