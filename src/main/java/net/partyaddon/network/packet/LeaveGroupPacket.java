package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

public record LeaveGroupPacket() implements CustomPayload {

    public static final CustomPayload.Id<LeaveGroupPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("leave_group_packet"));

    public static final PacketCodec<RegistryByteBuf, LeaveGroupPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
    }, buf -> new LeaveGroupPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}

