// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft;

import com.electronwill.nightconfig.core.file.FileConfig;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.details.FluidData;
import dan200.computercraft.shared.peripheral.generic.methods.EnergyMethods;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.platform.ForgeConfigFile;
import dan200.computercraft.shared.platform.NetworkHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@Mod(ComputerCraftAPI.MOD_ID)
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ComputerCraft {
    public ComputerCraft() {
        ModRegistry.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ((ForgeConfigFile) ConfigSpec.serverSpec).spec());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ((ForgeConfigFile) ConfigSpec.clientSpec).spec());

        NetworkHandler.setup();
    }

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.create(new RegistryBuilder<>(ITurtleUpgrade.serialiserRegistryKey()));
        event.create(new RegistryBuilder<>(IPocketUpgrade.serialiserRegistryKey()));
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(ModRegistry::registerMainThread);

        ComputerCraftAPI.registerGenericSource(new InventoryMethods());
        ComputerCraftAPI.registerGenericSource(new FluidMethods());
        ComputerCraftAPI.registerGenericSource(new EnergyMethods());

        ForgeComputerCraftAPI.registerGenericCapability(Capabilities.ItemHandler.BLOCK);
        ForgeComputerCraftAPI.registerGenericCapability(Capabilities.FluidHandler.BLOCK);
        ForgeComputerCraftAPI.registerGenericCapability(Capabilities.EnergyStorage.BLOCK);

        ForgeDetailRegistries.FLUID_STACK.addProvider(FluidData::fill);

        // if (ModList.get().isLoaded(MoreRedIntegration.MOD_ID)) MoreRedIntegration.setup();
    }

    @SubscribeEvent
    public static void sync(ModConfigEvent.Loading event) {
        syncConfig(event.getConfig());
    }

    @SubscribeEvent
    public static void sync(ModConfigEvent.Reloading event) {
        syncConfig(event.getConfig());
    }

    private static void syncConfig(ModConfig config) {
        if (!config.getModId().equals(ComputerCraftAPI.MOD_ID)) return;

        var path = config.getConfigData() instanceof FileConfig fileConfig ? fileConfig.getNioPath() : null;

        if (config.getType() == ModConfig.Type.SERVER && ((ForgeConfigFile) ConfigSpec.serverSpec).spec().isLoaded()) {
            ConfigSpec.syncServer(path);
        } else if (config.getType() == ModConfig.Type.CLIENT) {
            ConfigSpec.syncClient(path);
        }
    }
}
