package net.partyaddon.screen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.partyaddon.network.PartyAddonClientPacket;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.libz.api.Tab;
import net.libz.util.DrawTabHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.partyaddon.group.ClientParty;
import net.partyaddon.init.KeyInit;
import net.partyaddon.init.RenderInit;
import net.partyaddon.util.NameHelper;

@Environment(EnvType.CLIENT)
public class PartyScreen extends Screen implements Tab {

    public static final Component title = Component.translatable("text.partyaddon.gui.title");

    private static final int AVAILABLE_ROWS = 13;
    private static final int GROUP_ROWS = 7;
    private static final int ROW_HEIGHT = 13;
    private static final int SCROLLBAR_HEIGHT = AVAILABLE_ROWS * ROW_HEIGHT;
    private static final int SCROLLER_HEIGHT = 27;
    private static final int NAME_WIDTH = 60;

    private final int backgroundWidth = 200;
    private final int backgroundHeight = 215;
    private int x;
    private int y;

    private final List<UUID> availablePlayers = new ArrayList<UUID>();
    private final Set<UUID> awaitingInvitePlayers = new HashSet<UUID>();
    private int lastPartyVersion = -1;
    private int lastPendingInvitationsVersion = -1;
    private int lastTabListSize = -1;
    private WidgetButtonPage[] availablePlayerButtons;
    private int indexStartOffset;
    private boolean scrolling;
    private EditBox searchBox;
    private WidgetButtonPage acceptDeclineButton;
    private WidgetButtonPage[] groupPlayerButtons;

    public PartyScreen() {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        this.acceptDeclineButton = this.addRenderableWidget(new WidgetButtonPage(this.x + 109, this.y + 25, -1, button -> {
            UUID invitationPlayerId = ClientParty.getInvitationPlayerId();
            if (invitationPlayerId != null) {
                PartyAddonClientPacket.writeC2SAcceptInvitationPacket(invitationPlayerId);
                button.active = false;
                button.visible = false;
            }
        }));

        this.groupPlayerButtons = new WidgetButtonPage[GROUP_ROWS];
        for (int i = 0; i < this.groupPlayerButtons.length; i++) {
            int row = i;
            this.groupPlayerButtons[i] = this.addRenderableWidget(new WidgetButtonPage(this.x + 109, this.y + 119 + i * ROW_HEIGHT, -i - 2, button -> onGroupButtonPress(row)));
        }

        this.availablePlayerButtons = new WidgetButtonPage[AVAILABLE_ROWS];
        for (int i = 0; i < this.availablePlayerButtons.length; i++) {
            int row = i;
            this.availablePlayerButtons[i] = this.addRenderableWidget(new WidgetButtonPage(this.x + 5, this.y + 41 + i * ROW_HEIGHT, i, button -> {
                UUID playerId = getAvailablePlayer(row);
                if (playerId != null && NameHelper.isPlayerUUIDNotNull(this.minecraft, playerId)) {
                    PartyAddonClientPacket.writeC2SInvitePlayerToGroupPacket(playerId);
                    this.awaitingInvitePlayers.add(playerId);
                    button.active = false;
                }
            }));
        }

        this.searchBox = new EditBox(this.font, this.x + 5, this.y + 24, 93, 15, this.searchBox, Component.translatable("searchPlayer.search"));
        this.searchBox.setResponder(searchString -> refreshAvailablePlayers());
        this.addWidget(this.searchBox);

        refreshAvailablePlayers();
        updateButtons();
    }

    private void onGroupButtonPress(int row) {
        List<UUID> groupPlayerIds = ClientParty.getGroupPlayerIds();
        if (row >= groupPlayerIds.size()) {
            return;
        }
        if (row == 0) {
            PartyAddonClientPacket.writeC2SLeaveGroupPacket();
        } else if (ClientParty.isGroupLeader(this.minecraft)) {
            if (Screen.hasShiftDown()) {
                PartyAddonClientPacket.writeC2SPromotePlayerPacket(groupPlayerIds.get(row));
            } else {
                PartyAddonClientPacket.writeC2SKickPlayerPacket(groupPlayerIds.get(row));
            }
        }
    }

    @Nullable
    private UUID getAvailablePlayer(int row) {
        int index = row + this.indexStartOffset;
        return index >= 0 && index < this.availablePlayers.size() ? this.availablePlayers.get(index) : null;
    }

    @Nullable
    private UUID getGroupPlayer(int row) {
        List<UUID> groupPlayerIds = ClientParty.getGroupPlayerIds();
        return row >= 0 && row < groupPlayerIds.size() ? groupPlayerIds.get(row) : null;
    }

    private void refreshAvailablePlayers() {
        String search = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        this.availablePlayers.clear();
        for (UUID playerId : ClientParty.getAvailablePlayerIds()) {
            if (!NameHelper.isPlayerUUIDNotNull(this.minecraft, playerId)) {
                continue;
            }
            if (!search.isEmpty() && !NameHelper.getPlayerName(this.minecraft, playerId, 0).getString().toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }
            this.availablePlayers.add(playerId);
        }
        this.indexStartOffset = Mth.clamp(this.indexStartOffset, 0, getMaxScrollOffset());
    }

    private void refreshIfStale() {
        int pendingInvitationsVersion = ClientParty.getPendingInvitationsVersion();
        if (pendingInvitationsVersion != this.lastPendingInvitationsVersion) {
            this.lastPendingInvitationsVersion = pendingInvitationsVersion;
            this.awaitingInvitePlayers.clear();
        }
        int partyVersion = ClientParty.getVersion();
        int tabListSize = this.minecraft != null && this.minecraft.getConnection() != null ? this.minecraft.getConnection().getOnlinePlayers().size() : 0;
        if (partyVersion != this.lastPartyVersion || tabListSize != this.lastTabListSize) {
            this.lastPartyVersion = partyVersion;
            this.lastTabListSize = tabListSize;
            refreshAvailablePlayers();
        }
    }

    private void updateButtons() {
        List<UUID> groupPlayerIds = ClientParty.getGroupPlayerIds();
        List<UUID> starPlayerIds = ClientParty.getStarPlayerIds();
        List<UUID> pendingInvitationIds = ClientParty.getPendingInvitationIds();
        boolean canInvite = groupPlayerIds.isEmpty() || (ClientParty.isGroupLeader(this.minecraft) && groupPlayerIds.size() < ClientParty.getGroupSize());

        for (int i = 0; i < this.availablePlayerButtons.length; i++) {
            WidgetButtonPage button = this.availablePlayerButtons[i];
            UUID playerId = getAvailablePlayer(i);
            button.visible = playerId != null;
            if (playerId != null) {
                button.active = canInvite && !groupPlayerIds.contains(playerId) && !pendingInvitationIds.contains(playerId) && !this.awaitingInvitePlayers.contains(playerId);
                button.setStar(starPlayerIds.contains(playerId));
            }
        }

        UUID invitationPlayerId = ClientParty.getInvitationPlayerId();
        boolean showInvitation = invitationPlayerId != null && !groupPlayerIds.contains(invitationPlayerId);
        this.acceptDeclineButton.visible = showInvitation;
        this.acceptDeclineButton.active = showInvitation;
        if (showInvitation) {
            this.acceptDeclineButton.setStar(starPlayerIds.contains(invitationPlayerId));
        }

        for (int i = 0; i < this.groupPlayerButtons.length; i++) {
            WidgetButtonPage button = this.groupPlayerButtons[i];
            UUID playerId = getGroupPlayer(i);
            button.visible = playerId != null;
            button.active = playerId != null;
            if (playerId != null) {
                button.setStar(i != 0 && starPlayerIds.contains(playerId));
                button.setLeader(ClientParty.isGroupLeader(playerId));
            }
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.searchBox.charTyped(chr, modifiers);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        refreshIfStale();
        updateButtons();

        super.render(context, mouseX, mouseY, delta);

        // Top label
        context.drawCenteredString(this.font, PartyScreen.title, this.width / 2, this.y + 7, 0xFFFFFF);// 0x3F3F3F

        // Available player list label
        context.drawString(this.font, Component.translatable("text.partyaddon.gui.player_list"), this.x + 5, this.y + 14, 0x3F3F3F, false);

        for (int i = 0; i < this.availablePlayerButtons.length; i++) {
            UUID playerId = getAvailablePlayer(i);
            if (playerId == null) {
                break;
            }
            String playerName = NameHelper.getPlayerName(this.minecraft, playerId, NAME_WIDTH).getString();
            context.drawString(this.font, playerName, this.x + 10, this.y + 44 + i * ROW_HEIGHT, this.availablePlayerButtons[i].active ? 0xFFFFFF : 0xA0A0A0, false);
        }
        this.renderScrollbar(context);

        // Invitation label
        context.drawString(this.font, Component.translatable("text.partyaddon.gui.invitation"), this.x + 195 - this.font.width(Component.translatable("text.partyaddon.gui.invitation")),
                this.y + 15, 0x3F3F3F, false);
        if (this.acceptDeclineButton.visible) {
            String playerName = NameHelper.getPlayerName(this.minecraft, ClientParty.getInvitationPlayerId(), NAME_WIDTH).getString();
            context.drawString(this.font, playerName, this.x + 114, this.y + 28, 0xFFFFFF, false);
        }

        // Info label
        context.drawString(this.font, Component.translatable("text.partyaddon.gui.info"), this.x + 195 - this.font.width(Component.translatable("text.partyaddon.gui.info")), this.y + 42,
                0x3F3F3F, false);

        List<UUID> groupPlayerIds = ClientParty.getGroupPlayerIds();
        int groupSize = groupPlayerIds.size();
        if (groupSize > 0) {
            context.drawString(this.font, Component.translatable("text.partyaddon.gui.info_player_count", groupSize), this.x + 109, this.y + 54, 0x3F3F3F, false);
            boolean hasXpBonus = ClientParty.getGroupXpBonus() > 0.0001f;
            if (hasXpBonus) {
                context.drawString(this.font, Component.translatable("text.partyaddon.gui.info_experience", Math.round(ClientParty.getGroupXpBonus() * 100 * groupSize)), this.x + 109,
                        this.y + 66, 0x3F3F3F, false);
            }
            if (groupSize >= ClientParty.getGroupSize() && ClientParty.getFullGroupBonus() >= 0.0001f) {
                int fullGroupY = this.y + (hasXpBonus ? 82 : 66);
                context.drawString(this.font, Component.translatable("text.partyaddon.gui.full_group_bonus").withStyle(ChatFormatting.DARK_GREEN), this.x + 109, fullGroupY, 0x3F3F3F, false);
                context.drawString(this.font, Component.translatable("text.partyaddon.gui.full_group_bonus_amount", Math.round(ClientParty.getFullGroupBonus() * 100)).withStyle(ChatFormatting.DARK_GREEN),
                        this.x + 109, fullGroupY + 11, 0x3F3F3F, false);
            }
        } else {
            context.drawString(this.font, Component.translatable("text.partyaddon.gui.empty_info"), this.x + 109, this.y + 54, 0x3F3F3F, false);
        }

        // Group label
        Component groupLabel = groupSize > GROUP_ROWS ? Component.translatable("text.partyaddon.gui.group_more", groupSize - GROUP_ROWS) : Component.translatable("text.partyaddon.gui.group");
        context.drawString(this.font, groupLabel, this.x + 195 - this.font.width(groupLabel), this.y + 109, 0x3F3F3F, false);
        boolean isGroupLeader = ClientParty.isGroupLeader(this.minecraft);
        for (int i = 0; i < this.groupPlayerButtons.length; i++) {
            UUID playerId = getGroupPlayer(i);
            if (playerId == null) {
                break;
            }
            String playerName = NameHelper.getPlayerName(this.minecraft, playerId, NAME_WIDTH).getString();
            context.drawString(this.font, playerName, this.x + 114, this.y + 122 + i * ROW_HEIGHT, 0xFFFFFF, false);
            if (i != 0 && isGroupLeader) {
                context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, this.x + 176, this.y + 122 + i * ROW_HEIGHT, 21, 32, 7, 7);
            }
        }
        this.searchBox.render(context, mouseX, mouseY, delta);
        DrawTabHelper.drawTab(this.minecraft, context, this, this.x, this.y, mouseX, mouseY);

        renderButtonTooltip(context, this.acceptDeclineButton, mouseX, mouseY);
        for (WidgetButtonPage button : this.availablePlayerButtons) {
            renderButtonTooltip(context, button, mouseX, mouseY);
        }
        for (WidgetButtonPage button : this.groupPlayerButtons) {
            renderButtonTooltip(context, button, mouseX, mouseY);
        }
    }

    private void renderButtonTooltip(GuiGraphics context, WidgetButtonPage button, int mouseX, int mouseY) {
        if (button.visible && button.isHovered()) {
            button.renderTooltip(context, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderTransparentBackground(context);

        context.blit(RenderInit.PARTY_ADDON_BACKGROUND, this.x, this.y, 0, 0, 200, 215);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers) || (this.searchBox.canConsumeInput() && keyCode != GLFW.GLFW_KEY_ESCAPE))
            return true;
        if (KeyInit.screenKey.matches(keyCode, scanCode) || net.levelz.init.KeyInit.screenKey.matches(keyCode, scanCode)
                || Objects.requireNonNull(minecraft).options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.minecraft != null) {
            DrawTabHelper.onTabButtonClick(minecraft, this, this.x, this.y, mouseX, mouseY, false);
        }

        this.scrolling = false;
        if (getMaxScrollOffset() > 0 && mouseX >= this.x + 92 && mouseX < this.x + 98 && mouseY >= this.y + 41 && mouseY < this.y + 41 + SCROLLBAR_HEIGHT) {
            this.scrolling = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int getMaxScrollOffset() {
        return Math.max(0, this.availablePlayers.size() - AVAILABLE_ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.indexStartOffset = Mth.clamp((int) ((double) this.indexStartOffset - verticalAmount), 0, getMaxScrollOffset());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrolling) {
            int maxScrollOffset = getMaxScrollOffset();
            float f = ((float) mouseY - (float) (this.y + 41) - SCROLLER_HEIGHT / 2.0f) / (float) (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT);
            this.indexStartOffset = Mth.clamp(Math.round(f * maxScrollOffset), 0, maxScrollOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void renderScrollbar(GuiGraphics context) {
        int maxScrollOffset = getMaxScrollOffset();
        if (maxScrollOffset > 0) {
            int scrollerY = (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT) * this.indexStartOffset / maxScrollOffset;
            context.blit(RenderInit.PARTY_ADDON_BACKGROUND, this.x + 92, this.y + 41 + scrollerY, 200, 0, 6, SCROLLER_HEIGHT);
        } else {
            context.blit(RenderInit.PARTY_ADDON_BACKGROUND, this.x + 92, this.y + 41, 206, 0, 6, SCROLLER_HEIGHT);
        }
    }

    private class WidgetButtonPage extends Button {
        private final int index;
        private boolean star = false;
        private boolean leader = false;

        public WidgetButtonPage(int x, int y, int index, Button.OnPress onPress) {
            super(x, y, 86, 13, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.index = index;
            this.visible = false;
        }

        public boolean isStar() {
            return this.star;
        }

        public void setStar(boolean star) {
            this.star = star;
        }

        public int getIndex() {
            return this.index;
        }

        public void setLeader(boolean leader) {
            this.leader = leader;
        }

        public boolean isLeader() {
            return this.leader;
        }

        @Nullable
        private UUID getPlayerId() {
            if (this.index == -1) {
                return ClientParty.getInvitationPlayerId();
            } else if (this.index < -1) {
                return PartyScreen.this.getGroupPlayer(-this.index - 2);
            }
            return PartyScreen.this.getAvailablePlayer(this.index);
        }

        private boolean isWithinBounds(double mouseX, double mouseY) {
            return mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.visible && button == 1 && this.isWithinBounds(mouseX, mouseY)) {
                UUID playerId = this.getPlayerId();
                if (this.getIndex() == -1) {
                    if (this.active && playerId != null) {
                        PartyAddonClientPacket.writeC2SDeclineInvitationPacket(playerId);
                    }
                    this.active = false;
                } else if (this.getIndex() != -2 && playerId != null) {
                    this.setStar(!this.isStar());
                    PartyAddonClientPacket.writeC2SChangeStarListPacket(playerId);
                }
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            Font textRenderer = minecraft.font;
            int i = this.getTextureY();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            context.blit(RenderInit.PARTY_ADDON_BACKGROUND, this.getX(), this.getY(), star ? 86 : 0, 216 + i * 13, this.width, this.height);
            if (this.leader) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
                RenderSystem.enableBlend();
                context.blit(RenderInit.PARTY_ADDON_GUI_ICONS, this.getX() + 67, this.getY() + 3, 0, 32, 7, 7);
            }

            int j = this.active ? 0xFFFFFF : 0xA0A0A0;
            context.drawCenteredString(textRenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0f) << 24);
        }

        public void renderTooltip(GuiGraphics context, int mouseX, int mouseY) {
            List<Component> lines = new ArrayList<Component>();
            UUID playerId = this.getPlayerId();
            if (playerId != null && NameHelper.isPlayerUUIDNotNull(minecraft, playerId)) {
                String playerName = NameHelper.getPlayerName(minecraft, playerId, 0).getString();
                if (PartyScreen.this.font.width(playerName) > NAME_WIDTH) {
                    lines.add(Component.nullToEmpty(playerName));
                }
            }
            if (this.index == -1) {
                lines.add(Component.translatable("text.partyaddon.gui.hint_accept").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("text.partyaddon.gui.hint_decline").withStyle(ChatFormatting.GRAY));
            } else if (this.index == -2) {
                lines.add(Component.translatable("text.partyaddon.gui.hint_leave").withStyle(ChatFormatting.GRAY));
            } else if (this.index < -1) {
                if (ClientParty.isGroupLeader(minecraft)) {
                    lines.add(Component.translatable("text.partyaddon.gui.hint_kick").withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable("text.partyaddon.gui.hint_promote").withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.translatable("text.partyaddon.gui.hint_star").withStyle(ChatFormatting.GRAY));
            } else {
                if (this.active) {
                    lines.add(Component.translatable("text.partyaddon.gui.hint_invite").withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.translatable("text.partyaddon.gui.hint_star").withStyle(ChatFormatting.GRAY));
            }
            context.renderComponentTooltip(minecraft.font, lines, mouseX, mouseY);
        }

        private int getTextureY() {
            int i = 1;
            if (!this.active) {
                i = 0;
            } else if (this.isHoveredOrFocused()) {
                i = 2;
            }
            return i;
        }
    }

}
