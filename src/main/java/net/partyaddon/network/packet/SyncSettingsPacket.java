package net.partyaddon.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.partyaddon.PartyAddonMain;

public record SyncSettingsPacket(int groupSize, float groupXpBonus, float fullGroupBonus) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSettingsPacket> PACKET_ID = new CustomPacketPayload.Type<>(PartyAddonMain.identifierOf("sync_settings_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSettingsPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeInt(value.groupSize);
        buf.writeFloat(value.groupXpBonus);
        buf.writeFloat(value.fullGroupBonus);
    }, buf -> new SyncSettingsPacket(buf.readInt(), buf.readFloat(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}
