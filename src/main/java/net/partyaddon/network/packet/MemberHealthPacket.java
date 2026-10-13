package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

import java.util.List;
import java.util.UUID;

public record MemberHealthPacket(List<MemberHealth> members) implements CustomPayload {

    public static final CustomPayload.Id<MemberHealthPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("member_health_packet"));

    public static final PacketCodec<RegistryByteBuf, MemberHealthPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeCollection(value.members, (bufx, member) -> {
            bufx.writeUuid(member.uuid());
            bufx.writeFloat(member.health());
            bufx.writeFloat(member.maxHealth());
        });
    }, buf -> new MemberHealthPacket(buf.readList((bufx) -> new MemberHealth(bufx.readUuid(), bufx.readFloat(), bufx.readFloat()))));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

    public record MemberHealth(UUID uuid, float health, float maxHealth) {
    }

}
