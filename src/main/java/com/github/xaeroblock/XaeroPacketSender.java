package com.github.xaeroblock;

import com.github.xaeroblock.payload.XaeroMinimapPayload;
import com.github.xaeroblock.payload.XaeroWorldMapPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Utility class for sending Xaero map restriction packets to players.
 */
public final class XaeroPacketSender {

    private XaeroPacketSender() {}

    /**
     * Sends the correct restriction state for both maps based on player access.
     * Safe to call for any online player.
     */
    public static void updateRestrictions(ServerPlayer player) {
        boolean blocked = !MapAccessManager.canUseMap(player);
        sendMinimap(player, blocked);
        sendWorldMap(player, blocked);
    }

    /** Sends a minimap restriction packet (blocked=true hides the minimap). */
    public static void sendMinimap(ServerPlayer player, boolean blocked) {
        PacketDistributor.sendToPlayer(player, new XaeroMinimapPayload(blocked));
    }

    /** Sends a world-map restriction packet (blocked=true disables the world map). */
    public static void sendWorldMap(ServerPlayer player, boolean blocked) {
        PacketDistributor.sendToPlayer(player, new XaeroWorldMapPayload(blocked));
    }
}
