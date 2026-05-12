package com.github.xaeroblock;

import com.github.xaeroblock.command.XaeroBlockCommand;
import com.github.xaeroblock.payload.XaeroMinimapPayload;
import com.github.xaeroblock.payload.XaeroWorldMapPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mod(XaeroBlockMod.MOD_ID)
public class XaeroBlockMod {

    public static final String MOD_ID = "xaeroblock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Re-send restrictions every N server ticks (20 ticks = 1 second)
    private static final int RESEND_INTERVAL_TICKS = 20 * 60; // 60 seconds

    private int tickCounter = 0;

    public XaeroBlockMod(IEventBus modEventBus) {
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    // ---------------------------------------------------------------
    // Mod-bus events
    // ---------------------------------------------------------------

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[XaeroBlock] Common setup complete.");
    }

    /**
     * Registers our S2C payloads on Xaero's channels.
     * Marked optional so clients without Xaero's mods are not disconnected.
     */
    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID).optional();

        registrar.playToClient(
            XaeroMinimapPayload.TYPE,
            XaeroMinimapPayload.STREAM_CODEC,
            (payload, ctx) -> {} // server never receives this
        );

        registrar.playToClient(
            XaeroWorldMapPayload.TYPE,
            XaeroWorldMapPayload.STREAM_CODEC,
            (payload, ctx) -> {} // server never receives this
        );

        LOGGER.info("[XaeroBlock] Registered S2C payloads for xaerominimap:main and xaeroworldmap:main.");
    }

    // ---------------------------------------------------------------
    // Game-bus events
    // ---------------------------------------------------------------

    private void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        Path configDir = Paths.get("config");
        MapAccessManager.init(configDir);
        LOGGER.info("[XaeroBlock] Server starting – map access manager initialized.");
    }

    private void onServerStopping(ServerStoppingEvent event) {
        MapAccessManager.save();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Delay by 1 tick to ensure the connection is fully ready before sending packets
        player.getServer().execute(() -> {
            if (player.getServer().getPlayerList().getPlayer(player.getUUID()) != null) {
                XaeroPacketSender.updateRestrictions(player);
                LOGGER.debug("[XaeroBlock] Sent initial restriction to {} (blocked={})",
                        player.getName().getString(), !MapAccessManager.canUseMap(player));
            }
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        XaeroBlockCommand.register(event.getDispatcher());
    }

    /**
     * Periodically re-send restriction packets to all players.
     * This prevents client-side bypass by re-applying restrictions every minute.
     */
    private void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < RESEND_INTERVAL_TICKS) return;
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            XaeroPacketSender.updateRestrictions(player);
        }
    }
}
