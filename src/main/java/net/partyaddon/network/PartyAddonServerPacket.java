package net.partyaddon.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.group.GroupManager;

public class PartyAddonServerPacket {

    // screen
    public static final Identifier OPEN_PARTY_SCREEN_CS_PACKET = new Identifier("partyaddon", "open_party_screen_cs");
    public static final Identifier OPEN_PARTY_SCREEN_SC_PACKET = new Identifier("partyaddon", "open_party_screen_sc");
    // star
    public static final Identifier CHANGE_STAR_PLAYER_LIST_CS_PACKET = new Identifier("partyaddon", "change_star_player_list_cs");
    public static final Identifier SYNC_STAR_PLAYER_LIST_SC_PACKET = new Identifier("partyaddon", "sync_star_player_list_sc");
    // group
    public static final Identifier INVITE_PLAYER_TO_GROUP_CS_PACKET = new Identifier("partyaddon", "invite_player_to_group_cs");
    public static final Identifier SYNC_INVITATION_SC_PACKET = new Identifier("partyaddon", "sync_invitation_sc");

    public static final Identifier DECLINE_INVITATION_CS_PACKET = new Identifier("partyaddon", "decline_invitation_cs");
    public static final Identifier SYNC_DECLINE_INVITATION_SC_PACKET = new Identifier("partyaddon", "sync_decline_invitation_sc");

    public static final Identifier ACCEPT_INVITATION_CS_PACKET = new Identifier("partyaddon", "accept_invitation_cs");
    public static final Identifier SYNC_ACCEPT_INVITATION_CS_PACKET = new Identifier("partyaddon", "sync_accept_invitation_cs");

    // public static final Identifier LEAVE_GROUP_SC_PACKET = new Identifier("partyaddon", "leave_group_sc");
    public static final Identifier LEAVE_GROUP_CS_PACKET = new Identifier("partyaddon", "leave_group_cs");

    public static final Identifier KICK_PLAYER_CS_PACKET = new Identifier("partyaddon", "kick_player_cs");

    // manager
    public static final Identifier SYNC_GROUP_MANAGER_SC_PACKET = new Identifier("partyaddon", "sync_group_manager_sc");

    // compat
    public static final Identifier MAP_COMPAT_CS_PACKET = new Identifier("partyaddon", "map_compat_packet_cs");
    public static final Identifier MAP_COMPAT_SC_PACKET = new Identifier("partyaddon", "map_compat_packet_sc");

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(OPEN_PARTY_SCREEN_CS_PACKET, (server, player, handler, buffer, sender) -> {
            server.execute(() -> {
                writeS2CSyncGroupManagerPacket(player, ((GroupManagerAccess) player).getGroupManager());
                writeS2COpenPartyScreenPacket(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CHANGE_STAR_PLAYER_LIST_CS_PACKET, (server, player, handler, buffer, sender) -> {
            UUID playerId = buffer.readUuid();
            server.execute(() -> {
                GroupManager groupManager = ((GroupManagerAccess) player).getGroupManager();
                if (groupManager.getStarPlayerIdList().contains(playerId)) {
                    groupManager.removePlayerStar(playerId);
                } else {
                    groupManager.addPlayerStar(playerId);
                }
                // Sync star player list
                writeS2CSyncStarPlayerListPacket(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(INVITE_PLAYER_TO_GROUP_CS_PACKET, (server, player, handler, buffer, sender) -> {
            UUID invitedPlayerId = buffer.readUuid();
            server.execute(() -> {
                if (player.getServer().getPlayerManager().getPlayer(invitedPlayerId) != null && player.getServer().getPlayerManager().getPlayer(invitedPlayerId) instanceof ServerPlayerEntity) {
                    player.getServer().getPlayerManager().getPlayer(invitedPlayerId).sendMessage(Text.translatable("text.partyaddon.invitation", player.getName().getString()));
                    ((GroupManagerAccess) player.getServer().getPlayerManager().getPlayer(invitedPlayerId)).getGroupManager().invitePlayerToGroup(player.getUuid());
                    // Sync invitation
                    writeS2CSyncInvitationPacket((ServerPlayerEntity) player.getServer().getPlayerManager().getPlayer(invitedPlayerId), player.getUuid());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DECLINE_INVITATION_CS_PACKET, (server, player, handler, buffer, sender) -> {
            UUID invitationPlayerId = buffer.readUuid();
            server.execute(() -> {
                writeS2CSyncDeclinePacket(player, invitationPlayerId);
                ((GroupManagerAccess) player).getGroupManager().declineInvitation();

                if (player.getServer().getPlayerManager().getPlayer(invitationPlayerId) != null && player.getServer().getPlayerManager().getPlayer(invitationPlayerId) instanceof ServerPlayerEntity) {
                    player.getServer().getPlayerManager().getPlayer(invitationPlayerId).sendMessage(Text.translatable("text.partyaddon.declined_invitation", player.getName().getString()));
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ACCEPT_INVITATION_CS_PACKET, (server, player, handler, buffer, sender) -> {
            UUID invitationPlayerId = buffer.readUuid();
            server.execute(() -> {
                GroupManager.tryJoinGroup(player, invitationPlayerId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LEAVE_GROUP_CS_PACKET, (server, player, handler, buffer, sender) -> {
            server.execute(() -> {
                GroupManager.leaveGroup(player, false);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(KICK_PLAYER_CS_PACKET, (server, player, handler, buffer, sender) -> {
            UUID groupLeaderUUID = buffer.readUuid();
            UUID kickPlayerId = buffer.readUuid();
            server.execute(() -> {
                if (player.getServer().getPlayerManager().getPlayer(kickPlayerId) != null && player.getServer().getPlayerManager().getPlayer(kickPlayerId) instanceof ServerPlayerEntity
                        && player.getServer().getPlayerManager().getPlayer(groupLeaderUUID) != null && player.getServer().getPlayerManager().getPlayer(groupLeaderUUID) instanceof ServerPlayerEntity
                        && ((GroupManagerAccess) player.getServer().getPlayerManager().getPlayer(groupLeaderUUID)).getGroupManager().isGroupLeader()) {
                    GroupManager.leaveGroup((ServerPlayerEntity) player.getServer().getPlayerManager().getPlayer(kickPlayerId), true);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(MAP_COMPAT_CS_PACKET, (server, player, handler, buffer, sender) -> {
            server.execute(() -> {
                writeS2CMapCompatPacket(player);
            });
        });
    }

    public static void writeS2CSyncGroupManagerPacket(ServerPlayerEntity serverPlayerEntity, GroupManager groupManager) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        List<UUID> availablePlayerIdList = new ArrayList<UUID>();
        for (int i = 0; i < serverPlayerEntity.getServer().getPlayerManager().getPlayerList().size(); i++) {
            availablePlayerIdList.add(serverPlayerEntity.getServer().getPlayerManager().getPlayerList().get(i).getUuid());
        }

        availablePlayerIdList.remove((Object) serverPlayerEntity.getUuid());

        for (int i = 0; i < groupManager.getGroupPlayerIdList().size(); i++) {
            availablePlayerIdList.remove((Object) groupManager.getGroupPlayerIdList().get(i));
        }
        buf.writeInt(availablePlayerIdList.size());
        for (int i = 0; i < availablePlayerIdList.size(); i++) {
            buf.writeUuid(availablePlayerIdList.get(i));
        }
        int starPlayerCount = groupManager.getStarPlayerIdList().size();
        buf.writeInt(starPlayerCount);
        for (int i = 0; i < starPlayerCount; i++) {
            buf.writeUuid(groupManager.getStarPlayerIdList().get(i));
        }

        int groupCount = groupManager.getGroupPlayerIdList().size();
        buf.writeInt(groupCount);
        for (int i = 0; i < groupCount; i++) {
            buf.writeUuid(groupManager.getGroupPlayerIdList().get(i));
        }
        buf.writeBoolean(groupManager.getGroupLeaderId() != null);
        if (groupManager.getGroupLeaderId() != null) {
            buf.writeUuid(groupManager.getGroupLeaderId());
        }
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(SYNC_GROUP_MANAGER_SC_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    public static void writeS2CSyncStarPlayerListPacket(ServerPlayerEntity serverPlayerEntity) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        int count = ((GroupManagerAccess) serverPlayerEntity).getGroupManager().getStarPlayerIdList().size();
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeUuid(((GroupManagerAccess) serverPlayerEntity).getGroupManager().getStarPlayerIdList().get(i));
        }
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(SYNC_STAR_PLAYER_LIST_SC_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    public static void writeS2CSyncInvitationPacket(ServerPlayerEntity serverPlayerEntity, UUID invitationPlayerId) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(invitationPlayerId);
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(SYNC_INVITATION_SC_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    public static void writeS2CSyncDeclinePacket(ServerPlayerEntity serverPlayerEntity, UUID playerId) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        // buf.writeInt(playerId);
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(SYNC_DECLINE_INVITATION_SC_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);

        // if (playerId != 0 && serverPlayerEntity.world.getEntityById(playerId) != null && serverPlayerEntity.world.getEntityById(playerId) instanceof ServerPlayerEntity) {
        // serverPlayerEntity.world.getEntityById(playerId).sendMessage(Text.translatable("text.partyaddon.declined_invitation", serverPlayerEntity.getName().getString()));
        // }
    }

    public static void writeS2COpenPartyScreenPacket(ServerPlayerEntity serverPlayerEntity) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(OPEN_PARTY_SCREEN_SC_PACKET, buf);
        serverPlayerEntity.networkHandler.sendPacket(packet);
    }

    public static void writeS2CMapCompatPacket(ServerPlayerEntity serverPlayerEntity) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    GroupManager groupManager = ((GroupManagerAccess) serverPlayerEntity).getGroupManager();

    List<UUID> groupPlayerUUIDs = new ArrayList<>();
    List<BlockPos> groupPlayerBlockPoses = new ArrayList<>();
    List<Float> groupPlayerYaws = new ArrayList<>();

    for (UUID playerUUID : groupManager.getGroupPlayerIdList()) {
        // Get the player entity from the server player manager
        ServerPlayerEntity groupPlayer = serverPlayerEntity.getServer().getPlayerManager().getPlayer(playerUUID);
        if (groupPlayer == null || serverPlayerEntity.getUuid().equals(playerUUID)) {
            continue;
        }

        groupPlayerUUIDs.add(playerUUID);
        groupPlayerBlockPoses.add(groupPlayer.getBlockPos());
        groupPlayerYaws.add(groupPlayer.getYaw());
    }

    int uuidCount = groupPlayerUUIDs.size();
    buf.writeInt(uuidCount);
    for (int i = 0; i < uuidCount; i++) {
        buf.writeUuid(groupPlayerUUIDs.get(i));
        buf.writeBlockPos(groupPlayerBlockPoses.get(i));
        buf.writeFloat(groupPlayerYaws.get(i));
    }
    CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(MAP_COMPAT_SC_PACKET, buf);
    serverPlayerEntity.networkHandler.sendPacket(packet);
}


}
