package com.github.xaeroblock.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload sent on Xaero's Minimap channel (xaerominimap:main).
 *
 * Packet format expected by the XaerosMinimap client mod:
 *   byte  : packet type   (0 = server restrictions)
 *   bool  : hide minimap  (true = hidden/disabled)
 *   bool  : radar disabled
 *   bool  : cave mode disabled
 *   bool  : waypoints disabled
 *
 * If Xaero updates their protocol this encoding may need to be adjusted.
 */
public record XaeroMinimapPayload(boolean blocked) implements CustomPacketPayload {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("xaerominimap", "main");

    public static final Type<XaeroMinimapPayload> TYPE = new Type<>(CHANNEL_ID);

    public static final StreamCodec<FriendlyByteBuf, XaeroMinimapPayload> STREAM_CODEC =
            StreamCodec.of(XaeroMinimapPayload::encode, XaeroMinimapPayload::decode);

    private static void encode(FriendlyByteBuf buf, XaeroMinimapPayload payload) {
        buf.writeByte(0);                        // packet type: server restrictions
        buf.writeBoolean(payload.blocked());     // hide minimap
        buf.writeBoolean(payload.blocked());     // disable radar
        buf.writeBoolean(false);                 // disable cave mode (keep available)
        buf.writeBoolean(payload.blocked());     // disable waypoints
    }

    private static XaeroMinimapPayload decode(FriendlyByteBuf buf) {
        // Server never receives this payload; decoder is a no-op stub
        return new XaeroMinimapPayload(false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
