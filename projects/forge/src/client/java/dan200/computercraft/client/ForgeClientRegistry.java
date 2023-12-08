// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.model.turtle.TurtleModelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * Registers textures and models for items.
 */
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeClientRegistry {
    private ForgeClientRegistry() {
    }

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(new ResourceLocation(ComputerCraftAPI.MOD_ID, "turtle"), TurtleModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        ClientRegistry.registerExtraModels(event::register);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ClientRegistry.registerShaders(event.getResourceProvider(), event::registerShader);
    }

    @SubscribeEvent
    public static void onItemColours(RegisterColorHandlersEvent.Item event) {
        ClientRegistry.registerItemColours(event::register);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        ClientRegistry.registerReloadListeners(event::registerReloadListener, Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent event) {
        ClientRegistry.register();
        event.enqueueWork(ClientRegistry::registerMainThread);
    }
}
