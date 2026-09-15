package net.partyaddon.group;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.network.PartyAddonClientPacket;
import net.partyaddon.network.PartyAddonServerPacket;

public class GroupManager {

    private final PlayerEntity playerEntity;

    private List<UUID> availablePlayerIdList = new ArrayList<UUID>();
    private List<UUID> starPlayerIdList = new ArrayList<UUID>();
    private List<UUID> groupPlayerIdList = new ArrayList<UUID>();
    @Nullable
    private UUID groupLeaderId = null;
    @Nullable
    private UUID invitationPlayerId = null;
    private int invitationTick = 0;

    public GroupManager(PlayerEntity playerEntity) {
        this.playerEntity = playerEntity;
    }

    public void readNbt(NbtCompound nbt) {
    }

    public void writeNbt(NbtCompound nbt) {
    }

    public void tick() {
        // Can be server instead of client?
        if (this.playerEntity.getWorld().isClient() && this.invitationPlayerId != null) {
            this.invitationTick++;
            if (this.invitationTick >= ConfigInit.CONFIG.invitationTime) {
                PartyAddonClientPacket.writeC2SDeclineInvitationPacket(this.invitationPlayerId);
            }
        }
    }

    // available player
    public List<UUID> getAvailablePlayerIdList() {
        return this.availablePlayerIdList;
    }

    // available player
    public void setAvailablePlayerIdList(List<UUID> availablePlayerIdList) {
        this.availablePlayerIdList = availablePlayerIdList;
    }

    // Star stuff
    public List<UUID> getStarPlayerIdList() {
        return this.starPlayerIdList;
    }

    public void removePlayerStar(UUID playerId) {
        if (this.starPlayerIdList.contains(playerId)) {
            this.starPlayerIdList.remove((Object) playerId);
        }
    }

    public void addPlayerStar(UUID playerId) {
        if (this.playerEntity.getWorld().getPlayerByUuid(playerId) != null && !this.starPlayerIdList.contains(playerId)) {
            this.starPlayerIdList.add(playerId);
        }
    }

    public void updateStarPlayerIdList(List<UUID> newStarPlayerIdList) {
        this.starPlayerIdList = newStarPlayerIdList;
    }

    public void clearStarPlayerIdList() {
        this.starPlayerIdList.clear();
    }

    // Group stuff
    public List<UUID> getGroupPlayerIdList() {
        return this.groupPlayerIdList;
    }

    public void addPlayerToGroup(UUID playerId) {
        if (this.playerEntity.getWorld().getPlayerByUuid(playerId) != null && !this.groupPlayerIdList.contains(playerId)) {
            this.groupPlayerIdList.add(playerId);
        }
    }

    public void removePlayerFromGroup(UUID playerId) {
        if (this.groupPlayerIdList.contains(playerId)) {
            this.groupPlayerIdList.remove((Object) playerId);
        }
    }

    public void clearPlayerGroupIdList() {
        this.groupPlayerIdList.clear();
    }

    public void updatePlayerGroupIdList(List<UUID> newGroupPlayerIdList, UUID groupLeaderId) {
        this.groupLeaderId = groupLeaderId;
        this.groupPlayerIdList = newGroupPlayerIdList;
    }

    public void leaveGroup() {
        if (!this.groupPlayerIdList.isEmpty()) {
            // Do this before writeS2CLeaveGroupPacket cause it syncs to client
            this.groupPlayerIdList.clear();
        }
        // setGroupLeaderId(0);
        setGroupLeaderId(null);
    }

    public void invitePlayerToGroup(UUID playerId) {
        this.invitationPlayerId = playerId;
    }

    public void declineInvitation() {
        this.invitationTick = 0;
        this.invitationPlayerId = null;
    }

    @Nullable
    public UUID getInvitationPlayerId() {
        return this.invitationPlayerId;
    }

    public void setGroupLeaderId(UUID groupLeaderId) {
        this.groupLeaderId = groupLeaderId;
    }

    @Nullable
    public UUID getGroupLeaderId() {
        return this.groupLeaderId;
    }

    @Nullable
    public boolean isGroupLeader() {
        return this.groupLeaderId != null && this.groupLeaderId.equals(this.playerEntity.getUuid());
    }

    public void kickPlayer(UUID playerId) {
        if (isGroupLeader() && getGroupPlayerIdList().contains(playerId)) {
            removePlayerFromGroup(playerId);
        }
    }

    public static void tryJoinGroup(ServerPlayerEntity player, UUID invitationPlayerUUID) {
        if (invitationPlayerUUID != null && player.getWorld().getPlayerByUuid(invitationPlayerUUID) != null && player.getWorld().getPlayerByUuid(invitationPlayerUUID) instanceof ServerPlayerEntity) {

            UUID leaderId = ((GroupManagerAccess) player.getWorld().getPlayerByUuid(invitationPlayerUUID)).getGroupManager().getGroupLeaderId() == null ? invitationPlayerUUID
                    : ((GroupManagerAccess) player.getWorld().getPlayerByUuid(invitationPlayerUUID)).getGroupManager().getGroupLeaderId();

            // groupManager of leader
            GroupManager groupLeaderManager = ((GroupManagerAccess) player.getWorld().getPlayerByUuid(leaderId)).getGroupManager();
            if (groupLeaderManager.getGroupPlayerIdList().size() >= ConfigInit.CONFIG.groupSize) {
                player.sendMessage(Text.translatable("text.partyaddon.group_is_full", player.getWorld().getPlayerByUuid(leaderId).getName().getString()));
            } else {
                if (groupLeaderManager.getGroupPlayerIdList().isEmpty()) {
                    groupLeaderManager.addPlayerToGroup(leaderId); // equals invitationPlayerId
                    groupLeaderManager.setGroupLeaderId(leaderId);

                    groupLeaderManager.addPlayerToGroup(player.getUuid());
                } else {
                    groupLeaderManager.addPlayerToGroup(player.getUuid());
                }
                // Update leader
                PartyAddonServerPacket.writeS2CSyncGroupManagerPacket((ServerPlayerEntity) player.getWorld().getPlayerByUuid(leaderId), groupLeaderManager);

                for (int i = 1; i < groupLeaderManager.getGroupPlayerIdList().size(); i++) {
                    UUID playerId = groupLeaderManager.getGroupPlayerIdList().get(i);
                    if (player.getWorld().getPlayerByUuid(playerId) != null && player.getWorld().getPlayerByUuid(playerId) instanceof ServerPlayerEntity) {

                        ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player.getWorld().getPlayerByUuid(playerId);

                        List<UUID> groupPlayerList = new ArrayList<UUID>();
                        groupPlayerList.addAll(groupLeaderManager.getGroupPlayerIdList());
                        groupPlayerList.remove((Object) serverPlayerEntity.getUuid());
                        groupPlayerList.add(0, serverPlayerEntity.getUuid());
                        ((GroupManagerAccess) serverPlayerEntity).getGroupManager().updatePlayerGroupIdList(groupPlayerList, leaderId);

                        if (serverPlayerEntity.getUuid() != player.getUuid()) {
                            serverPlayerEntity.sendMessage(Text.translatable("text.partyaddon.accepted_invitation", player.getName().getString()));
                        }
                        // update manager
                        PartyAddonServerPacket.writeS2CSyncGroupManagerPacket(serverPlayerEntity, ((GroupManagerAccess) serverPlayerEntity).getGroupManager());
                    }
                }

            }
        } else {
            player.sendMessage(Text.translatable("text.partyaddon.no_invitation"));
        }
        // set invitation id server and client to 0
        ((GroupManagerAccess) player).getGroupManager().declineInvitation();
        PartyAddonServerPacket.writeS2CSyncDeclinePacket(player, null);
    }

    public static void leaveGroup(ServerPlayerEntity player, boolean kicked) {
        GroupManager groupManager = ((GroupManagerAccess) player).getGroupManager();
        if (groupManager.getGroupPlayerIdList().size() > 0) {
            for (int i = 1; i < groupManager.getGroupPlayerIdList().size(); i++) {
                UUID playerId = groupManager.getGroupPlayerIdList().get(i);
                if (player.getWorld().getPlayerByUuid(playerId) != null && player.getWorld().getPlayerByUuid(playerId) instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player.getWorld().getPlayerByUuid(playerId);

                    serverPlayerEntity.sendMessage(Text.translatable("text.partyaddon.left_group", player.getName().getString()));

                    GroupManager groupMemberManager = ((GroupManagerAccess) serverPlayerEntity).getGroupManager();
                    groupMemberManager.removePlayerFromGroup(player.getUuid());
                    if (groupMemberManager.getGroupPlayerIdList().size() <= 1) {
                        groupMemberManager.clearPlayerGroupIdList();
                        groupMemberManager.setGroupLeaderId(null);
                    }
                    PartyAddonServerPacket.writeS2CSyncGroupManagerPacket(serverPlayerEntity, groupMemberManager);
                }
            }

            groupManager.leaveGroup();
            if (kicked) {
                player.sendMessage(Text.translatable("text.partyaddon.kicked"));
            } else {
                player.sendMessage(Text.translatable("text.partyaddon.leave_group"));
            }
            PartyAddonServerPacket.writeS2CSyncGroupManagerPacket(player, groupManager);
        } else {
            player.sendMessage(Text.translatable("text.partyaddon.no_group"));
        }
    }

    public static void invitePlayer(ServerPlayerEntity inviter, ServerPlayerEntity invited) {
        if (inviter.getUuid().equals(invited.getUuid())) {
            inviter.sendMessage(Text.translatable("text.partyaddon.cannot_invite_self"));
            return;
        }
        GroupManager inviterManager = ((GroupManagerAccess) inviter).getGroupManager();
        if (!inviterManager.getGroupPlayerIdList().isEmpty() && !inviterManager.isGroupLeader()) {
            inviter.sendMessage(Text.translatable("text.partyaddon.not_leader"));
            return;
        }

        invited.sendMessage(Text.translatable("text.partyaddon.invitation", inviter.getName().getString()));
        ((GroupManagerAccess) invited).getGroupManager().invitePlayerToGroup(inviter.getUuid());
        PartyAddonServerPacket.writeS2CSyncInvitationPacket(invited, inviter.getUuid());
    }

}
