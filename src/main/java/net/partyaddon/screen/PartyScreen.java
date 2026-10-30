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
import net.levelz.init.KeyInit;
import net.libz.api.Tab;
import net.libz.util.DrawTabHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.partyaddon.group.ClientParty;
import net.partyaddon.init.RenderInit;
import net.partyaddon.util.NameHelper;

@Environment(EnvType.CLIENT)
public class PartyScreen extends Screen implements Tab {

    public static final Text title = Text.translatable("text.partyaddon.gui.title");

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
    private final Set<UUID> invitedPlayers = new HashSet<UUID>();
    private WidgetButtonPage[] availablePlayerButtons;
    private int indexStartOffset;
    private boolean scrolling;
    private TextFieldWidget searchBox;
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

        this.acceptDeclineButton = this.addDrawableChild(new WidgetButtonPage(this.x + 109, this.y + 25, -1, button -> {
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
            this.groupPlayerButtons[i] = this.addDrawableChild(new WidgetButtonPage(this.x + 109, this.y + 119 + i * ROW_HEIGHT, -i - 2, button -> onGroupButtonPress(row)));
        }

        this.availablePlayerButtons = new WidgetButtonPage[AVAILABLE_ROWS];
        for (int i = 0; i < this.availablePlayerButtons.length; i++) {
            int row = i;
            this.availablePlayerButtons[i] = this.addDrawableChild(new WidgetButtonPage(this.x + 5, this.y + 41 + i * ROW_HEIGHT, i, button -> {
                UUID playerId = getAvailablePlayer(row);
                if (playerId != null && NameHelper.isPlayerUUIDNotNull(this.client, playerId)) {
                    PartyAddonClientPacket.writeC2SInvitePlayerToGroupPacket(playerId);
                    this.invitedPlayers.add(playerId);
                    button.active = false;
                }
            }));
        }

        this.searchBox = new TextFieldWidget(this.textRenderer, this.x + 5, this.y + 24, 93, 15, this.searchBox, Text.translatable("searchPlayer.search"));
        this.searchBox.setChangedListener(searchString -> refreshAvailablePlayers());
        this.addSelectableChild(this.searchBox);

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
        } else if (ClientParty.isGroupLeader(this.client)) {
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
        String search = this.searchBox == null ? "" : this.searchBox.getText().trim().toLowerCase(Locale.ROOT);
        this.availablePlayers.clear();
        for (UUID playerId : ClientParty.getAvailablePlayerIds()) {
            if (!NameHelper.isPlayerUUIDNotNull(this.client, playerId)) {
                continue;
            }
            if (!search.isEmpty() && !NameHelper.getPlayerName(this.client, playerId, 0).getString().toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }
            this.availablePlayers.add(playerId);
        }
        this.indexStartOffset = MathHelper.clamp(this.indexStartOffset, 0, getMaxScrollOffset());
    }

    private void updateButtons() {
        List<UUID> groupPlayerIds = ClientParty.getGroupPlayerIds();
        List<UUID> starPlayerIds = ClientParty.getStarPlayerIds();
        boolean canInvite = groupPlayerIds.isEmpty() || (ClientParty.isGroupLeader(this.client) && groupPlayerIds.size() < ClientParty.getGroupSize());

        for (int i = 0; i < this.availablePlayerButtons.length; i++) {
            WidgetButtonPage button = this.availablePlayerButtons[i];
            UUID playerId = getAvailablePlayer(i);
            button.visible = playerId != null;
            if (playerId != null) {
                button.active = canInvite && !groupPlayerIds.contains(playerId) && !this.invitedPlayers.contains(playerId);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refreshAvailablePlayers();
        updateButtons();

        super.render(context, mouseX, mouseY, delta);

        // Top label
        context.drawCenteredTextWithShadow(this.textRenderer, PartyScreen.title, this.width / 2, this.y + 7, 0xFFFFFF);// 0x3F3F3F

        // Available player list label
        context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.player_list"), this.x + 5, this.y + 14, 0x3F3F3F, false);

        for (int i = 0; i < this.availablePlayerButtons.length; i++) {
            UUID playerId = getAvailablePlayer(i);
            if (playerId == null) {
                break;
            }
            String playerName = NameHelper.getPlayerName(this.client, playerId, NAME_WIDTH).getString();
            context.drawText(this.textRenderer, playerName, this.x + 10, this.y + 44 + i * ROW_HEIGHT, this.availablePlayerButtons[i].active ? 0xFFFFFF : 0xA0A0A0, false);
        }
        this.renderScrollbar(context);

        // Invitation label
        context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.invitation"), this.x + 195 - this.textRenderer.getWidth(Text.translatable("text.partyaddon.gui.invitation")),
                this.y + 15, 0x3F3F3F, false);
        if (this.acceptDeclineButton.visible) {
            String playerName = NameHelper.getPlayerName(this.client, ClientParty.getInvitationPlayerId(), NAME_WIDTH).getString();
            context.drawText(this.textRenderer, playerName, this.x + 114, this.y + 28, 0xFFFFFF, false);
        }

        // Info label
        context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.info"), this.x + 195 - this.textRenderer.getWidth(Text.translatable("text.partyaddon.gui.info")), this.y + 42,
                0x3F3F3F, false);

        List<UUID> groupPlayerIds = ClientParty.getGroupPlayerIds();
        int groupSize = groupPlayerIds.size();
        if (groupSize > 0) {
            context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.info_player_count", groupSize), this.x + 109, this.y + 54, 0x3F3F3F, false);
            boolean hasXpBonus = ClientParty.getGroupXpBonus() > 0.0001f;
            if (hasXpBonus) {
                context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.info_experience", Math.round(ClientParty.getGroupXpBonus() * 100 * groupSize)), this.x + 109,
                        this.y + 66, 0x3F3F3F, false);
            }
            if (groupSize >= ClientParty.getGroupSize() && ClientParty.getFullGroupBonus() >= 0.0001f) {
                int fullGroupY = this.y + (hasXpBonus ? 82 : 66);
                context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.full_group_bonus").formatted(Formatting.DARK_GREEN), this.x + 109, fullGroupY, 0x3F3F3F, false);
                context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.full_group_bonus_amount", Math.round(ClientParty.getFullGroupBonus() * 100)).formatted(Formatting.DARK_GREEN),
                        this.x + 109, fullGroupY + 11, 0x3F3F3F, false);
            }
        } else {
            context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.empty_info"), this.x + 109, this.y + 54, 0x3F3F3F, false);
        }

        // Group label
        Text groupLabel = groupSize > GROUP_ROWS ? Text.translatable("text.partyaddon.gui.group_more", groupSize - GROUP_ROWS) : Text.translatable("text.partyaddon.gui.group");
        context.drawText(this.textRenderer, groupLabel, this.x + 195 - this.textRenderer.getWidth(groupLabel), this.y + 109, 0x3F3F3F, false);
        boolean isGroupLeader = ClientParty.isGroupLeader(this.client);
        for (int i = 0; i < this.groupPlayerButtons.length; i++) {
            UUID playerId = getGroupPlayer(i);
            if (playerId == null) {
                break;
            }
            String playerName = NameHelper.getPlayerName(this.client, playerId, NAME_WIDTH).getString();
            context.drawText(this.textRenderer, playerName, this.x + 114, this.y + 122 + i * ROW_HEIGHT, 0xFFFFFF, false);
            if (i != 0 && isGroupLeader) {
                context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, this.x + 176, this.y + 122 + i * ROW_HEIGHT, 21, 32, 7, 7);
            }
        }
        this.searchBox.render(context, mouseX, mouseY, delta);
        DrawTabHelper.drawTab(this.client, context, this, this.x, this.y, mouseX, mouseY);

        renderButtonTooltip(context, this.acceptDeclineButton, mouseX, mouseY);
        for (WidgetButtonPage button : this.availablePlayerButtons) {
            renderButtonTooltip(context, button, mouseX, mouseY);
        }
        for (WidgetButtonPage button : this.groupPlayerButtons) {
            renderButtonTooltip(context, button, mouseX, mouseY);
        }
    }

    private void renderButtonTooltip(DrawContext context, WidgetButtonPage button, int mouseX, int mouseY) {
        if (button.visible && button.isHovered()) {
            button.renderTooltip(context, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);

        context.drawTexture(RenderInit.PARTY_ADDON_BACKGROUND, this.x, this.y, 0, 0, 200, 215);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers) || (this.searchBox.isActive() && keyCode != GLFW.GLFW_KEY_ESCAPE))
            return true;
        if (KeyInit.screenKey.matchesKey(keyCode, scanCode) || Objects.requireNonNull(client).options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.client != null) {
            DrawTabHelper.onTabButtonClick(client, this, this.x, this.y, mouseX, mouseY, false);
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
        this.indexStartOffset = MathHelper.clamp((int) ((double) this.indexStartOffset - verticalAmount), 0, getMaxScrollOffset());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrolling) {
            int maxScrollOffset = getMaxScrollOffset();
            float f = ((float) mouseY - (float) (this.y + 41) - SCROLLER_HEIGHT / 2.0f) / (float) (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT);
            this.indexStartOffset = MathHelper.clamp(Math.round(f * maxScrollOffset), 0, maxScrollOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void renderScrollbar(DrawContext context) {
        int maxScrollOffset = getMaxScrollOffset();
        if (maxScrollOffset > 0) {
            int scrollerY = (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT) * this.indexStartOffset / maxScrollOffset;
            context.drawTexture(RenderInit.PARTY_ADDON_BACKGROUND, this.x + 92, this.y + 41 + scrollerY, 200, 0, 6, SCROLLER_HEIGHT);
        } else {
            context.drawTexture(RenderInit.PARTY_ADDON_BACKGROUND, this.x + 92, this.y + 41, 206, 0, 6, SCROLLER_HEIGHT);
        }
    }

    private class WidgetButtonPage extends ButtonWidget {
        private final int index;
        private boolean star = false;
        private boolean leader = false;

        public WidgetButtonPage(int x, int y, int index, ButtonWidget.PressAction onPress) {
            super(x, y, 86, 13, ScreenTexts.EMPTY, onPress, DEFAULT_NARRATION_SUPPLIER);
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
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            TextRenderer textRenderer = client.textRenderer;
            int i = this.getTextureY();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            context.drawTexture(RenderInit.PARTY_ADDON_BACKGROUND, this.getX(), this.getY(), star ? 86 : 0, 216 + i * 13, this.width, this.height);
            if (this.leader) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
                RenderSystem.enableBlend();
                context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, this.getX() + 67, this.getY() + 3, 0, 32, 7, 7);
            }

            int j = this.active ? 0xFFFFFF : 0xA0A0A0;
            context.drawCenteredTextWithShadow(textRenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0f) << 24);
        }

        public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
            List<Text> lines = new ArrayList<Text>();
            UUID playerId = this.getPlayerId();
            if (playerId != null && NameHelper.isPlayerUUIDNotNull(client, playerId)) {
                String playerName = NameHelper.getPlayerName(client, playerId, 0).getString();
                if (PartyScreen.this.textRenderer.getWidth(playerName) > NAME_WIDTH) {
                    lines.add(Text.of(playerName));
                }
            }
            if (this.index == -1) {
                lines.add(Text.translatable("text.partyaddon.gui.hint_accept").formatted(Formatting.GRAY));
                lines.add(Text.translatable("text.partyaddon.gui.hint_decline").formatted(Formatting.GRAY));
            } else if (this.index == -2) {
                lines.add(Text.translatable("text.partyaddon.gui.hint_leave").formatted(Formatting.GRAY));
            } else if (this.index < -1) {
                if (ClientParty.isGroupLeader(client)) {
                    lines.add(Text.translatable("text.partyaddon.gui.hint_kick").formatted(Formatting.GRAY));
                    lines.add(Text.translatable("text.partyaddon.gui.hint_promote").formatted(Formatting.GRAY));
                }
                lines.add(Text.translatable("text.partyaddon.gui.hint_star").formatted(Formatting.GRAY));
            } else {
                if (this.active) {
                    lines.add(Text.translatable("text.partyaddon.gui.hint_invite").formatted(Formatting.GRAY));
                }
                lines.add(Text.translatable("text.partyaddon.gui.hint_star").formatted(Formatting.GRAY));
            }
            context.drawTooltip(client.textRenderer, lines, mouseX, mouseY);
        }

        private int getTextureY() {
            int i = 1;
            if (!this.active) {
                i = 0;
            } else if (this.isSelected()) {
                i = 2;
            }
            return i;
        }
    }

}
