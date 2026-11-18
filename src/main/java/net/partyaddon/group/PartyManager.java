package net.partyaddon.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.levelz.access.ServerPlayerSyncAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.network.PartyAddonServerPacket;
import net.partyaddon.network.packet.MemberHealthPacket.MemberHealth;

public final class PartyManager {

    private static final int HEALTH_SYNC_INTERVAL = 10;

    private static final Map<UUID, Party> PARTIES = new HashMap<UUID, Party>();
    private static final Map<UUID, Invitation> INVITATIONS = new HashMap<UUID, Invitation>();
    private static final Map<UUID, Set<UUID>> STARS = new HashMap<UUID, Set<UUID>>();

    private record Invitation(UUID inviterId, int expirationTick) {
    }

    private enum LeaveReason {
        LEFT, KICKED, DISCONNECTED
    }

    private PartyManager() {
    }

    @Nullable
    public static Party getParty(UUID playerId) {
        return PARTIES.get(playerId);
    }

    public static void clear() {
        PARTIES.clear();
        INVITATIONS.clear();
        STARS.clear();
    }

    // Invitation stuff
    public static void invite(ServerPlayer inviter, ServerPlayer invited) {
        sendInvitation(inviter, invited);
        syncOutgoingInvitations(inviter);
    }

    private static void sendInvitation(ServerPlayer inviter, ServerPlayer invited) {
        if (inviter.getUUID().equals(invited.getUUID())) {
            inviter.sendSystemMessage(Component.translatable("text.partyaddon.cannot_invite_self"));
            return;
        }
        Party party = getParty(inviter.getUUID());
        if (party != null) {
            if (!party.isLeader(inviter.getUUID())) {
                inviter.sendSystemMessage(Component.translatable("text.partyaddon.not_leader"));
                return;
            }
            if (party.contains(invited.getUUID())) {
                inviter.sendSystemMessage(Component.translatable("text.partyaddon.already_in_your_group", invited.getName().getString()));
                return;
            }
            if (party.size() >= ConfigInit.CONFIG.groupSize) {
                inviter.sendSystemMessage(Component.translatable("text.partyaddon.group_is_full", inviter.getName().getString()));
                return;
            }
        }
        Invitation pending = INVITATIONS.get(invited.getUUID());
        if (pending != null && pending.inviterId().equals(inviter.getUUID())) {
            inviter.sendSystemMessage(Component.translatable("text.partyaddon.already_invited", invited.getName().getString()));
            return;
        }
        MinecraftServer server = inviter.getServer();
        INVITATIONS.put(invited.getUUID(), new Invitation(inviter.getUUID(), server.getTickCount() + Math.max(1, ConfigInit.CONFIG.invitationTime)));

        if (pending != null) {
            ServerPlayer previousInviter = server.getPlayerList().getPlayer(pending.inviterId());
            if (previousInviter != null) {
                previousInviter.sendSystemMessage(Component.translatable("text.partyaddon.invitation_replaced", invited.getName().getString()));
                syncOutgoingInvitations(previousInviter);
            }
        }

        invited.sendSystemMessage(Component.translatable("text.partyaddon.invitation", inviter.getName().getString()));
        inviter.sendSystemMessage(Component.translatable("text.partyaddon.invitation_sent", invited.getName().getString()));
        PartyAddonServerPacket.writeS2CSyncInvitationPacket(invited, inviter.getUUID());
    }

    public static void acceptInvitation(ServerPlayer player, @Nullable UUID expectedInviterId) {
        Invitation invitation = INVITATIONS.get(player.getUUID());
        if (invitation == null || (expectedInviterId != null && !expectedInviterId.equals(invitation.inviterId()))) {
            player.sendSystemMessage(Component.translatable("text.partyaddon.no_invitation"));
            return;
        }
        INVITATIONS.remove(player.getUUID());
        PartyAddonServerPacket.writeS2CSyncDeclinePacket(player, null);

        MinecraftServer server = player.getServer();
        ServerPlayer inviter = server.getPlayerList().getPlayer(invitation.inviterId());
        if (inviter == null) {
            player.sendSystemMessage(Component.translatable("text.partyaddon.no_invitation"));
            return;
        }
        syncOutgoingInvitations(inviter);
        Party party = getParty(inviter.getUUID());
        if (party != null) {
            if (!party.isLeader(inviter.getUUID())) {
                player.sendSystemMessage(Component.translatable("text.partyaddon.invitation_invalid"));
                return;
            }
            if (party.contains(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("text.partyaddon.already_member"));
                return;
            }
            if (party.size() >= ConfigInit.CONFIG.groupSize) {
                player.sendSystemMessage(Component.translatable("text.partyaddon.group_is_full", getName(server, party.getLeaderId())));
                return;
            }
        }
        if (getParty(player.getUUID()) != null) {
            removeMember(player, LeaveReason.LEFT);
        }
        if (party == null) {
            party = new Party(inviter.getUUID());
            PARTIES.put(inviter.getUUID(), party);
        }
        for (ServerPlayer member : party.getOnlineMembers(server)) {
            member.sendSystemMessage(Component.translatable("text.partyaddon.accepted_invitation", player.getName().getString()));
        }
        party.addMember(player.getUUID());
        PARTIES.put(player.getUUID(), party);

        player.sendSystemMessage(Component.translatable("text.partyaddon.joined_group", getName(server, party.getLeaderId())));
        syncParty(server, party);
    }

    public static void declineInvitation(ServerPlayer player, @Nullable UUID expectedInviterId) {
        Invitation invitation = INVITATIONS.get(player.getUUID());
        PartyAddonServerPacket.writeS2CSyncDeclinePacket(player, null);
        if (invitation == null || (expectedInviterId != null && !expectedInviterId.equals(invitation.inviterId()))) {
            player.sendSystemMessage(Component.translatable("text.partyaddon.no_invitation"));
            return;
        }
        INVITATIONS.remove(player.getUUID());

        MinecraftServer server = player.getServer();
        ServerPlayer inviter = server.getPlayerList().getPlayer(invitation.inviterId());
        if (inviter != null) {
            inviter.sendSystemMessage(Component.translatable("text.partyaddon.declined_invitation", player.getName().getString()));
            syncOutgoingInvitations(inviter);
        }
        player.sendSystemMessage(Component.translatable("text.partyaddon.invitation_declined", getName(server, invitation.inviterId())));
    }

    // Group stuff
    public static void leave(ServerPlayer player) {
        if (getParty(player.getUUID()) == null) {
            player.sendSystemMessage(Component.translatable("text.partyaddon.no_group"));
            return;
        }
        removeMember(player, LeaveReason.LEFT);
    }

    public static void kick(ServerPlayer leader, UUID targetId) {
        Party party = getLeaderParty(leader);
        if (party == null) {
            return;
        }
        if (leader.getUUID().equals(targetId)) {
            leader.sendSystemMessage(Component.translatable("text.partyaddon.cannot_kick_self"));
            return;
        }
        ServerPlayer target = leader.getServer().getPlayerList().getPlayer(targetId);
        if (target == null || !party.contains(targetId)) {
            leader.sendSystemMessage(Component.translatable("text.partyaddon.not_in_group"));
            return;
        }
        removeMember(target, LeaveReason.KICKED);
    }

    public static void promote(ServerPlayer leader, UUID targetId) {
        Party party = getLeaderParty(leader);
        if (party == null) {
            return;
        }
        if (leader.getUUID().equals(targetId)) {
            leader.sendSystemMessage(Component.translatable("text.partyaddon.already_leader"));
            return;
        }
        if (!party.contains(targetId)) {
            leader.sendSystemMessage(Component.translatable("text.partyaddon.not_in_group"));
            return;
        }
        MinecraftServer server = leader.getServer();
        party.setLeaderId(targetId);
        for (ServerPlayer member : party.getOnlineMembers(server)) {
            member.sendSystemMessage(Component.translatable("text.partyaddon.new_leader", getName(server, targetId)));
        }
        syncParty(server, party);
    }

    public static void list(ServerPlayer player) {
        Party party = getParty(player.getUUID());
        if (party == null) {
            player.sendSystemMessage(Component.translatable("text.partyaddon.no_group"));
            return;
        }
        MinecraftServer server = player.getServer();
        MutableComponent members = Component.empty();
        for (int i = 0; i < party.size(); i++) {
            UUID memberId = party.getMemberIds().get(i);
            if (i > 0) {
                members.append(", ");
            }
            if (party.isLeader(memberId)) {
                members.append(Component.translatable("text.partyaddon.leader_suffix", getName(server, memberId)));
            } else {
                members.append(getName(server, memberId));
            }
        }
        player.sendSystemMessage(Component.translatable("text.partyaddon.group_list", party.size(), ConfigInit.CONFIG.groupSize, members));
    }

    // Star stuff
    public static void toggleStar(ServerPlayer player, UUID targetId) {
        Set<UUID> stars = STARS.computeIfAbsent(player.getUUID(), id -> new LinkedHashSet<UUID>());
        if (!stars.remove(targetId) && !targetId.equals(player.getUUID()) && player.getServer().getPlayerList().getPlayer(targetId) != null) {
            stars.add(targetId);
        }
        PartyAddonServerPacket.writeS2CSyncStarPlayerListPacket(player, getStarIds(player.getUUID()));
    }

    public static List<UUID> getStarIds(UUID playerId) {
        Set<UUID> stars = STARS.get(playerId);
        return stars == null ? List.of() : List.copyOf(stars);
    }

    // Experience stuff
    public static boolean shareVanillaExperience(ServerPlayer collector, int experience) {
        Party party = getParty(collector.getUUID());
        if (party == null || experience <= 0) {
            return false;
        }
        List<ServerPlayer> members = party.getOnlineMembers(collector.getServer());
        if (members.isEmpty()) {
            return false;
        }
        int share = party.takeVanillaExperienceShare(experience * getExperienceMultiplier(party), members.size());
        if (share > 0) {
            for (ServerPlayer member : members) {
                member.giveExperiencePoints(share);
            }
        }
        return true;
    }

    public static boolean shareLevelZExperience(ServerPlayer collector, int experience) {
        Party party = getParty(collector.getUUID());
        if (party == null || experience <= 0) {
            return false;
        }
        List<ServerPlayer> members = party.getOnlineMembers(collector.getServer());
        if (members.isEmpty()) {
            return false;
        }
        int share = party.takeLevelZExperienceShare(experience * getExperienceMultiplier(party), members.size());
        if (share > 0) {
            for (ServerPlayer member : members) {
                ((ServerPlayerSyncAccess) member).addLevelExperience(share);
            }
        }
        return true;
    }

    private static double getExperienceMultiplier(Party party) {
        return Party.getExperienceMultiplier(party.size(), ConfigInit.CONFIG.groupSize, ConfigInit.CONFIG.groupXpBonus, ConfigInit.CONFIG.fullGroupBonus);
    }

    // Events
    public static void onJoin(ServerPlayer player) {
        PartyAddonServerPacket.writeS2CSyncSettingsPacket(player);
        syncPlayer(player);
    }

    public static void onDisconnect(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        Invitation ownInvitation = INVITATIONS.remove(player.getUUID());
        if (ownInvitation != null) {
            syncOutgoingInvitations(server, ownInvitation.inviterId());
        }

        Iterator<Map.Entry<UUID, Invitation>> iterator = INVITATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Invitation> entry = iterator.next();
            if (!entry.getValue().inviterId().equals(player.getUUID())) {
                continue;
            }
            iterator.remove();
            ServerPlayer invited = server.getPlayerList().getPlayer(entry.getKey());
            if (invited != null) {
                invited.sendSystemMessage(Component.translatable("text.partyaddon.invitation_expired", player.getName().getString()));
                PartyAddonServerPacket.writeS2CSyncDeclinePacket(invited, null);
            }
        }
        removeMember(player, LeaveReason.DISCONNECTED);
    }

    public static void tick(MinecraftServer server) {
        if (!INVITATIONS.isEmpty()) {
            int ticks = server.getTickCount();
            Set<UUID> invitersToSync = null;
            Iterator<Map.Entry<UUID, Invitation>> iterator = INVITATIONS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Invitation> entry = iterator.next();
                if (entry.getValue().expirationTick() > ticks) {
                    continue;
                }
                iterator.remove();
                ServerPlayer invited = server.getPlayerList().getPlayer(entry.getKey());
                ServerPlayer inviter = server.getPlayerList().getPlayer(entry.getValue().inviterId());
                if (invited != null) {
                    invited.sendSystemMessage(Component.translatable("text.partyaddon.invitation_expired", getName(server, entry.getValue().inviterId())));
                    PartyAddonServerPacket.writeS2CSyncDeclinePacket(invited, null);
                    if (inviter != null) {
                        inviter.sendSystemMessage(Component.translatable("text.partyaddon.invitation_not_answered", invited.getName().getString()));
                    }
                }
                if (inviter != null) {
                    if (invitersToSync == null) {
                        invitersToSync = new HashSet<UUID>();
                    }
                    invitersToSync.add(inviter.getUUID());
                }
            }
            if (invitersToSync != null) {
                for (UUID inviterId : invitersToSync) {
                    syncOutgoingInvitations(server, inviterId);
                }
            }
        }
        if (!PARTIES.isEmpty() && server.getTickCount() % HEALTH_SYNC_INTERVAL == 0) {
            Set<Party> parties = Collections.newSetFromMap(new IdentityHashMap<Party, Boolean>());
            parties.addAll(PARTIES.values());
            for (Party party : parties) {
                List<ServerPlayer> members = party.getOnlineMembers(server);
                List<MemberHealth> memberHealths = new ArrayList<MemberHealth>(members.size());
                for (ServerPlayer member : members) {
                    memberHealths.add(new MemberHealth(member.getUUID(), member.getHealth(), member.getMaxHealth()));
                }
                if (!party.updateSentMemberHealths(memberHealths)) {
                    continue;
                }
                for (ServerPlayer member : members) {
                    PartyAddonServerPacket.writeS2CMemberHealthPacket(member, memberHealths);
                }
            }
        }
    }

    // Sync stuff
    public static void syncPlayer(ServerPlayer player) {
        Party party = getParty(player.getUUID());
        List<UUID> availablePlayerIds = new ArrayList<UUID>();
        for (ServerPlayer serverPlayerEntity : player.getServer().getPlayerList().getPlayers()) {
            if (!serverPlayerEntity.getUUID().equals(player.getUUID())) {
                availablePlayerIds.add(serverPlayerEntity.getUUID());
            }
        }
        List<UUID> groupPlayerIds = party == null ? List.of() : party.getMemberIdsFor(player.getUUID());
        PartyAddonServerPacket.writeS2CSyncGroupPacket(player, availablePlayerIds, getStarIds(player.getUUID()), groupPlayerIds, party == null ? null : party.getLeaderId());
    }

    public static void syncOutgoingInvitations(ServerPlayer inviter) {
        List<UUID> invitedIds = new ArrayList<UUID>();
        for (Map.Entry<UUID, Invitation> entry : INVITATIONS.entrySet()) {
            if (entry.getValue().inviterId().equals(inviter.getUUID())) {
                invitedIds.add(entry.getKey());
            }
        }
        PartyAddonServerPacket.writeS2CSyncPendingInvitationsPacket(inviter, invitedIds);
    }

    private static void syncOutgoingInvitations(MinecraftServer server, UUID inviterId) {
        ServerPlayer inviter = server.getPlayerList().getPlayer(inviterId);
        if (inviter != null) {
            syncOutgoingInvitations(inviter);
        }
    }

    private static void syncParty(MinecraftServer server, Party party) {
        party.resetSentMemberHealths();
        for (ServerPlayer member : party.getOnlineMembers(server)) {
            syncPlayer(member);
        }
    }

    private static void removeMember(ServerPlayer player, LeaveReason reason) {
        Party party = PARTIES.remove(player.getUUID());
        if (party == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        boolean wasLeader = party.isLeader(player.getUUID());
        party.removeMember(player.getUUID());

        if (reason != LeaveReason.DISCONNECTED) {
            player.sendSystemMessage(Component.translatable(reason == LeaveReason.KICKED ? "text.partyaddon.kicked" : "text.partyaddon.leave_group"));
            syncPlayer(player);
        }
        Component memberMessage = Component.translatable(reason == LeaveReason.KICKED ? "text.partyaddon.kicked_member" : "text.partyaddon.left_group", player.getName().getString());

        if (party.size() <= 1) {
            for (UUID memberId : party.getMemberIds()) {
                PARTIES.remove(memberId);
                ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                if (member != null) {
                    member.sendSystemMessage(memberMessage);
                    member.sendSystemMessage(Component.translatable("text.partyaddon.group_disbanded"));
                    syncPlayer(member);
                }
            }
            return;
        }
        if (wasLeader) {
            party.setLeaderId(party.getMemberIds().get(0));
        }
        for (ServerPlayer member : party.getOnlineMembers(server)) {
            member.sendSystemMessage(memberMessage);
            if (wasLeader) {
                member.sendSystemMessage(Component.translatable("text.partyaddon.new_leader", getName(server, party.getLeaderId())));
            }
        }
        syncParty(server, party);
    }

    @Nullable
    private static Party getLeaderParty(ServerPlayer player) {
        Party party = getParty(player.getUUID());
        if (party == null) {
            player.sendSystemMessage(Component.translatable("text.partyaddon.no_group"));
            return null;
        }
        if (!party.isLeader(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("text.partyaddon.not_leader"));
            return null;
        }
        return party;
    }

    private static String getName(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return player != null ? player.getName().getString() : "?";
    }
}
