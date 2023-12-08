// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.network.client.UpgradesLoadedMessage;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemFullBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlockEntity;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Forge-specific dispatch for {@link CommonHooks}.
 */
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID)
public class ForgeCommonHooks {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        switch (event.phase) {
            case START -> CommonHooks.onServerTickStart(event.getServer());
            case END -> CommonHooks.onServerTickEnd();
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        CommonHooks.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CommonHooks.onServerStopped();
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event) {
        CommandComputerCraft.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Sent event) {
        CommonHooks.onChunkWatch(event.getChunk(), event.getPlayer());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        CommonHooks.onDatapackReload((id, listener) -> event.addListener(listener));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var packet = new UpgradesLoadedMessage();
        if (event.getPlayer() == null) {
            PlatformHelper.get().sendToAllPlayers(packet, event.getPlayerList().getServer());
        } else {
            PlatformHelper.get().sendToPlayer(packet, event.getPlayer());
        }
    }

    /**
     * Attach capabilities to our block entities.
     *
     * @param event The event to register capabilities with.
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.COMPUTER_NORMAL.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.COMPUTER_ADVANCED.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.TURTLE_NORMAL.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.TURTLE_ADVANCED.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.SPEAKER.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.PRINTER.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.DISK_DRIVE.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.MONITOR_NORMAL.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.MONITOR_ADVANCED.get(), (b, d) -> b.peripheral());

        event.registerBlockEntity(
            PeripheralCapability.get(), BlockEntityType.COMMAND_BLOCK,
            (b, d) -> Config.enableCommandBlock ? new CommandBlockPeripheral(b) : null
        );

        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.WIRELESS_MODEM_NORMAL.get(), WirelessModemBlockEntity::getPeripheral);
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.WIRELESS_MODEM_ADVANCED.get(), WirelessModemBlockEntity::getPeripheral);
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.WIRED_MODEM_FULL.get(), WiredModemFullBlockEntity::getPeripheral);
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.CABLE.get(), CableBlockEntity::getPeripheral);

        event.registerBlockEntity(WiredElementCapability.get(), ModRegistry.BlockEntities.WIRED_MODEM_FULL.get(), (b, d) -> b.getElement());
        event.registerBlockEntity(WiredElementCapability.get(), ModRegistry.BlockEntities.CABLE.get(), CableBlockEntity::getWiredElement);
        // TODO: turtle.onMoved(peripheral::invalidate);
        // TODO: cable.onModemChanged(() -> { peripheralHandler.invalidate(); elementHandler.invalidate(); });
        // TODO: modem.onModemChanged(peripheral::invalidate);
    }

    @SubscribeEvent
    public static void lootLoad(LootTableLoadEvent event) {
        var pool = CommonHooks.getExtraLootPool(event.getName());
        if (pool != null) event.getTable().addPool(pool.build());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (CommonHooks.onEntitySpawn(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(itemEntity -> CommonHooks.onLivingDrop(event.getEntity(), itemEntity.getItem()));
    }
}
