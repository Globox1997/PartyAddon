package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

public record PartyScreenPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PartyScreenPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("party_screen_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartyScreenPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
    }, buf -> new PartyScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

