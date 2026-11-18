package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

public record LeaveGroupPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LeaveGroupPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("leave_group_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeaveGroupPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
    }, buf -> new LeaveGroupPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}

