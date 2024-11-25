package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

public record PartyScreenPacket() implements CustomPayload {

    public static final CustomPayload.Id<PartyScreenPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("party_screen_packet"));

    public static final PacketCodec<RegistryByteBuf, PartyScreenPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
    }, buf -> new PartyScreenPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

