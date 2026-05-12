package com.github.xaeroblock.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload sent on Xaero's World Map channel (xaeroworldmap:main).
 *
 * Packet format expected by the XaerosWorldMap client mod:
 *   byte  : packet type      (0 = server restrictions)
 *   bool  : disable world map (true = disabled)
 *   bool  : disable auto-mapping
 *
 * If Xaero updates their protocol this encoding may need to be adjusted.
 */
public record XaeroWorldMapPayload(boolean blocked) implements CustomPacketPayload {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("xaeroworldmap", "main");

    public static final Type<XaeroWorldMapPayload> TYPE = new Type<>(CHANNEL_ID);

    public static final StreamCodec<FriendlyByteBuf, XaeroWorldMapPayload> STREAM_CODEC =
            StreamCodec.of(XaeroWorldMapPayload::encode, XaeroWorldMapPayload::decode);

    private static void encode(FriendlyByteBuf buf, XaeroWorldMapPayload payload) {
        buf.writeByte(0);                        // packet type: server restrictions
        buf.writeBoolean(payload.blocked());     // disable world map
        buf.writeBoolean(payload.blocked());     // disable auto-mapping
    }

    private static XaeroWorldMapPayload decode(FriendlyByteBuf buf) {
        return new XaeroWorldMapPayload(false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
