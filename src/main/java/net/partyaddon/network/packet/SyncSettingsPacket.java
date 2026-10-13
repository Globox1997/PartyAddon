package net.partyaddon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.partyaddon.PartyAddonMain;

public record SyncSettingsPacket(int groupSize, float groupXpBonus, float fullGroupBonus) implements CustomPayload {

    public static final CustomPayload.Id<SyncSettingsPacket> PACKET_ID = new CustomPayload.Id<>(PartyAddonMain.identifierOf("sync_settings_packet"));

    public static final PacketCodec<RegistryByteBuf, SyncSettingsPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeInt(value.groupSize);
        buf.writeFloat(value.groupXpBonus);
        buf.writeFloat(value.fullGroupBonus);
    }, buf -> new SyncSettingsPacket(buf.readInt(), buf.readFloat(), buf.readFloat()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
