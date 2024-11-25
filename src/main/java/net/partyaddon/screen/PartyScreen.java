package net.partyaddon.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.partyaddon.access.GroupManagerAccess;
import net.partyaddon.group.GroupManager;
import net.partyaddon.init.ConfigInit;
import net.partyaddon.init.RenderInit;
import net.partyaddon.util.NameHelper;

@Environment(EnvType.CLIENT)
public class PartyScreen extends Screen implements Tab {

    public static final Text title = Text.translatable("text.partyaddon.gui.title");

    private int left;
    private int top;

    private PlayerEntity playerEntity;
    private GroupManager groupManager;

    private List<UUID> availablePlayers;
    private WidgetButtonPage[] availablePlayerButtons;
    int indexStartOffset;
    private boolean scrolling;
    private int selectedIndex;
    private TextFieldWidget searchBox;
    private WidgetButtonPage acceptDeclineButton;
    private WidgetButtonPage[] groupPlayerButtons;

    public PartyScreen() {
        super(title);
    }

    @Override
    protected void init() {
        this.left = this.width / 2 - 100;
        this.top = this.height / 2 - 108;
        this.playerEntity = client.player;
        this.groupManager = ((GroupManagerAccess) playerEntity).getGroupManager();
        this.availablePlayers = new ArrayList<UUID>();
        this.availablePlayers.addAll(groupManager.getAvailablePlayerIdList());

        if (this.availablePlayers.size() >= 13) {
            this.availablePlayerButtons = new WidgetButtonPage[13];
        } else {
            this.availablePlayerButtons = new WidgetButtonPage[this.availablePlayers.size()];
        }
        this.acceptDeclineButton = this.addDrawableChild(new WidgetButtonPage(this.left + 109, this.top + 26, -1, button -> {
            if (this.groupManager.getInvitationPlayerId() != null) {
                PartyAddonClientPacket.writeC2SAcceptInvitationPacket(this.groupManager.getInvitationPlayerId());
                this.acceptDeclineButton.active = false;
                this.acceptDeclineButton.visible = false;
            }
        }));
        boolean acceptDeclineButton = this.groupManager.getInvitationPlayerId() != null && !this.groupManager.getGroupPlayerIdList().contains(this.groupManager.getInvitationPlayerId());
        this.acceptDeclineButton.visible = acceptDeclineButton;
        this.acceptDeclineButton.active = acceptDeclineButton;
        if (acceptDeclineButton) {
            this.acceptDeclineButton.setStar(this.groupManager.getStarPlayerIdList().contains(this.groupManager.getInvitationPlayerId()));
        }

        this.groupPlayerButtons = new WidgetButtonPage[ConfigInit.CONFIG.groupSize];
        int o = this.top + 120;

        for (int i = 0; i < this.groupPlayerButtons.length; i++) {
            // if (i < 7) {// && i < groupManager.getGroupPlayerIdList().size()
            this.groupPlayerButtons[i] = this.addDrawableChild(new WidgetButtonPage(this.left + 109, o, -i - 2, button -> {
                if (((WidgetButtonPage) button).getIndex() == -2) {
                    PartyAddonClientPacket.writeC2SLeaveGroupPacket();
                } else if (this.groupManager.isGroupLeader() && this.playerEntity != null) {
                    PartyAddonClientPacket.writeC2SKickPlayerPacket(this.playerEntity.getUuid(), this.groupManager.getGroupPlayerIdList().get(Math.abs(((WidgetButtonPage) button).getIndex()) - 2));
                }
            }));
            if (i < this.groupManager.getGroupPlayerIdList().size()) {
                if (!this.groupManager.getStarPlayerIdList().isEmpty() && this.groupManager.getStarPlayerIdList().contains(this.groupManager.getGroupPlayerIdList().get(i))) {
                    this.groupPlayerButtons[i].setStar(true);
                }
                if (this.groupManager.getGroupLeaderId().equals(this.groupManager.getGroupPlayerIdList().get(i))) {
                    this.groupPlayerButtons[i].setLeader(true);
                }
            } else {
                this.groupPlayerButtons[i].active = false;
                this.groupPlayerButtons[i].visible = false;
            }
            if (i >= 7) {
                this.groupPlayerButtons[i].active = false;
                this.groupPlayerButtons[i].visible = false;
            }
            o += 13;
        }

        int k = this.top + 42;
        for (int l = 0; l < this.availablePlayerButtons.length; ++l) {
            this.availablePlayerButtons[l] = this.addDrawableChild(new WidgetButtonPage(this.left + 5, k, l, button -> {
                this.selectedIndex = ((WidgetButtonPage) button).getIndex() + this.indexStartOffset;
                if (NameHelper.isPlayerUUIDNotNull(client, this.availablePlayers.get(this.selectedIndex))) {
                    PartyAddonClientPacket.writeC2SInvitePlayerToGroupPacket(this.availablePlayers.get(this.selectedIndex));
                    button.active = false;
                }
            }));
            if (!groupManager.getStarPlayerIdList().isEmpty()
                    && groupManager.getStarPlayerIdList().contains(this.availablePlayers.get(this.availablePlayerButtons[l].getIndex() + this.indexStartOffset))) {
                this.availablePlayerButtons[l].setStar(true);
            }

            k += 13;
        }

        this.searchBox = new TextFieldWidget(this.textRenderer, this.left + 5, this.top + 25, 93, 15, this.searchBox, Text.translatable("searchPlayer.search"));
        this.searchBox.setChangedListener(searchString -> {

            if (searchString.equals("")) {

                groupManager.getAvailablePlayerIdList().forEach(id -> {
                    if (!this.availablePlayers.contains(id)) {
                        this.availablePlayers.add(id);
                    }
                });
            } else {
                for (int i = 0; i < groupManager.getAvailablePlayerIdList().size(); i++) {
                    UUID id = groupManager.getAvailablePlayerIdList().get(i);
                    if (!NameHelper.isPlayerUUIDNotNull(client, id)) {
                        this.availablePlayers.remove((Object) id);
                        continue;
                    }
                    if (NameHelper.getPlayerName(client, id, 0).getString().contains(searchString)) {
                        if (!this.availablePlayers.contains(id))
                            this.availablePlayers.add(id);
                    } else {
                        this.availablePlayers.remove((Object) id);
                    }
                }
            }
        });
        this.addSelectableChild(this.searchBox);

    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.searchBox.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Top label
        context.drawCenteredTextWithShadow(this.textRenderer, PartyScreen.title, this.width / 2, this.top + 7, 0xFFFFFF);// 0x3F3F3F

        // Available player list label
        context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.player_list"), this.left + 5, this.top + 15, 0x3F3F3F, false);

        int k = this.top + 45;
        this.renderScrollbar(context, this.left, this.top, this.availablePlayers);
        int m = 0;
        if (!this.availablePlayers.isEmpty()) {
            for (int i = 0; i < this.availablePlayers.size(); i++) {
                if (this.canScroll(this.availablePlayers.size()) && (m < this.indexStartOffset || m >= availablePlayerButtons.length + this.indexStartOffset)) {
                    ++m;
                    continue;
                }
                if (!NameHelper.isPlayerUUIDNotNull(client, this.availablePlayers.get(i))) {
                    this.availablePlayers.remove(i);
                    continue;
                }

                String playerName = NameHelper.getPlayerName(client, this.availablePlayers.get(i), 60).getString();
                context.drawText(this.textRenderer, playerName, this.left + 10, k, 0xFFFFFF, false);
                k += 13;
                ++m;
            }
        }

        for (WidgetButtonPage widgetButtonPage : this.availablePlayerButtons) {
            if (widgetButtonPage.isHovered()) {
                widgetButtonPage.renderTooltip(context, mouseX, mouseY);
            }
            widgetButtonPage.visible = widgetButtonPage.index < this.availablePlayers.size();
            if (widgetButtonPage.visible) {
                widgetButtonPage.setStar(groupManager.getStarPlayerIdList().contains(this.availablePlayers.get(widgetButtonPage.getIndex() + this.indexStartOffset)));
            }
        }

        if (this.acceptDeclineButton.active && this.acceptDeclineButton.visible && this.acceptDeclineButton.isHovered()) {
            this.acceptDeclineButton.renderTooltip(context, mouseX, mouseY);
        }

        // Invitation label
        context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.invitation"), this.left + 195 - this.textRenderer.getWidth(Text.translatable("text.partyaddon.gui.invitation")),
                this.top + 16, 0x3F3F3F, false);

        boolean acceptDeclineButton = this.groupManager.getInvitationPlayerId() != null && !this.groupManager.getGroupPlayerIdList().contains(this.groupManager.getInvitationPlayerId());
        this.acceptDeclineButton.visible = acceptDeclineButton;
        this.acceptDeclineButton.active = acceptDeclineButton;
        if (acceptDeclineButton) {
            this.acceptDeclineButton.setStar(this.groupManager.getStarPlayerIdList().contains(groupManager.getInvitationPlayerId()));

            if (NameHelper.isPlayerUUIDNotNull(client, groupManager.getInvitationPlayerId())) {
                String playerName = NameHelper.getPlayerName(client, groupManager.getInvitationPlayerId(), 60).getString();
                context.drawText(this.textRenderer, playerName, this.left + 114, this.top + 29, 0xFFFFFF, false);
            }
        }
        // Info label
        context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.info"), this.left + 195 - this.textRenderer.getWidth(Text.translatable("text.partyaddon.gui.info")), this.top + 43,
                0x3F3F3F, false);

        if (!this.groupManager.getGroupPlayerIdList().isEmpty()) {
            context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.info_player_count", this.groupManager.getGroupPlayerIdList().size()), this.left + 109, this.top + 55, 0x3F3F3F,
                    false);
            if (ConfigInit.CONFIG.groupXpBonus > 0.0001f) {
                context.drawText(this.textRenderer,
                        Text.translatable("text.partyaddon.gui.info_experience", Math.round(ConfigInit.CONFIG.groupXpBonus * 100 * this.groupManager.getGroupPlayerIdList().size())), this.left + 109,
                        this.top + 67, 0x3F3F3F, false);
            }
            if (this.groupManager.getGroupPlayerIdList().size() == ConfigInit.CONFIG.groupSize && ConfigInit.CONFIG.fullGroupBonus >= 0.0001f) {
                context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.full_group_bonus").formatted(Formatting.DARK_GREEN), this.left + 109,
                        this.top + 83 - (ConfigInit.CONFIG.groupXpBonus > 0.0001f ? 0 : 16), 0x3F3F3F, false);
                context.drawText(this.textRenderer,
                        Text.translatable("text.partyaddon.gui.full_group_bonus_amount", Math.round(ConfigInit.CONFIG.fullGroupBonus * 100)).formatted(Formatting.DARK_GREEN), this.left + 109,
                        this.top + 95, 0x3F3F3F, false);
            }
        } else {
            context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.empty_info"), this.left + 109, this.top + 55, 0x3F3F3F, false);
        }

        // Group label
        context.drawText(this.textRenderer, Text.translatable("text.partyaddon.gui.group"), this.left + 195 - this.textRenderer.getWidth(Text.translatable("text.partyaddon.gui.group")),
                this.top + 110, 0x3F3F3F, false);
        int g = 0;
        for (int i = 0; i < ConfigInit.CONFIG.groupSize; i++) {
            if (i < this.groupManager.getGroupPlayerIdList().size() && NameHelper.isPlayerUUIDNotNull(client, this.groupManager.getGroupPlayerIdList().get(i))) {
                UUID playerId = this.groupManager.getGroupPlayerIdList().get(i);

                this.groupPlayerButtons[i].active = true;
                this.groupPlayerButtons[i].visible = true;

                String playerName = NameHelper.getPlayerName(client, playerId, 60).getString();
                context.drawText(this.textRenderer, playerName, this.left + 114, this.top + 123 + g, 0xFFFFFF, false);

                if (i != 0) {
                    this.groupPlayerButtons[i].setStar(this.groupManager.getStarPlayerIdList().contains(playerId));
                }
                if (i != 0 && this.groupManager.isGroupLeader()) {
                    context.drawTexture(RenderInit.PARTY_ADDON_GUI_ICONS, this.left + 176, this.top + 123 + g, 21, 32, 7, 7);
                }
                if (this.groupPlayerButtons[i].isLeader()) {
                    if (!this.groupManager.getGroupLeaderId().equals(playerId)) {
                        this.groupPlayerButtons[i].setLeader(false);
                    }
                } else {
                    if (this.groupManager.getGroupLeaderId().equals(playerId)) {
                        this.groupPlayerButtons[i].setLeader(true);
                    }
                }
                g += 13;

            } else {
                this.groupPlayerButtons[i].active = false;
                this.groupPlayerButtons[i].visible = false;
            }
        }
        this.searchBox.render(context, mouseX, mouseY, delta);
        DrawTabHelper.drawTab(client, context, this, this.left, this.top, mouseX, mouseY);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);

        context.drawTexture(RenderInit.PARTY_ADDON_BACKGROUND, this.left, this.top, 0, 0, 200, 216);
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
            DrawTabHelper.onTabButtonClick(client, this, this.left, this.top, mouseX, mouseY, false);
        }

        this.scrolling = false;
        int i = (this.width - 200) / 2;
        int j = (this.height - 216) / 2;
        if (this.canScroll(this.availablePlayers.size()) && mouseX > (double) (i + 94) && mouseX < (double) (i + 94 + 6) && mouseY > (double) (j + 18) && mouseY <= (double) (j + 18 + 139 + 1)) {
            this.scrolling = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean canScroll(int listSize) {
        return listSize > availablePlayerButtons.length;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int i = this.availablePlayers.size();
        if (this.canScroll(i)) {
            int j = i - availablePlayerButtons.length;
            this.indexStartOffset = MathHelper.clamp((int) ((double) this.indexStartOffset - verticalAmount), 0, j);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        int i = this.availablePlayers.size();
        if (this.scrolling) {
            int j = this.top + 18;
            int k = j + 139;
            int l = i - availablePlayerButtons.length;
            float f = ((float) mouseY - (float) j - 13.5f) / ((float) (k - j) - 27.0f);
            f = f * (float) l + 0.5f;
            this.indexStartOffset = MathHelper.clamp((int) f, 0, l);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void renderScrollbar(DrawContext context, int x, int y, List<UUID> availablePlayers) {
        int i = availablePlayers.size() + 1 - availablePlayerButtons.length;
        if (i > 1) {
            int j = 167 - (27 + (i - 1) * 167 / i);
            int k = 1 + j / i + 167 / i;
            int m = Math.min(142, this.indexStartOffset * k);
            if (this.indexStartOffset == i - 1) {
                m = 142;
            }
            context.drawTexture(RenderInit.PARTY_ADDON_BACKGROUND, x + 92, y + 42 + m, 200, 0, 6, 27);
        } else {
            context.drawTexture(RenderInit.PARTY_ADDON_BACKGROUND, x + 92, y + 42, 206, 0, 6, 27);
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

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.visible && this.active && button == 1 && this.isHovered()) {
                if (this.getIndex() == -1) {
                    if (PartyScreen.this.groupManager.getInvitationPlayerId() != null) {
                        PartyAddonClientPacket.writeC2SDeclineInvitationPacket(PartyScreen.this.groupManager.getInvitationPlayerId());
                    }
                    this.active = false;
                } else if (this.getIndex() != -2) {
                    UUID playerId = null;

                    if (this.getIndex() < -1) {
                        int index = Math.abs(this.getIndex()) - 2;
                        if (!PartyScreen.this.groupManager.getGroupPlayerIdList().isEmpty() && PartyScreen.this.groupManager.getGroupPlayerIdList().size() >= index) {
                            playerId = PartyScreen.this.groupManager.getGroupPlayerIdList().get(index);
                        }
                    } else {
                        int index = this.getIndex() + PartyScreen.this.indexStartOffset;
                        if (!PartyScreen.this.availablePlayers.isEmpty() && PartyScreen.this.availablePlayers.size() >= index) {
                            playerId = PartyScreen.this.availablePlayers.get(index);
                        }
                    }
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

            // this.renderBackground(context, client, mouseX, mouseY);
            int j = this.active ? 0xFFFFFF : 0xA0A0A0;
            context.drawCenteredTextWithShadow(textRenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0f) << 24);

            if (this.isHovered()) {
                this.renderTooltip(context, mouseX, mouseY);
            }
        }

        public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
            if (this.hovered && this.active) {

                UUID playerId = null;
                if (this.getIndex() == -1) {
                    if (NameHelper.isPlayerUUIDNotNull(client, PartyScreen.this.groupManager.getInvitationPlayerId())) {
                        playerId = PartyScreen.this.groupManager.getInvitationPlayerId();
                    }
                } else {
                    if (this.getIndex() < -1) {
                        int index = Math.abs(this.index) - 2;
                        if (!PartyScreen.this.groupManager.getGroupPlayerIdList().isEmpty() && PartyScreen.this.groupManager.getGroupPlayerIdList().size() >= index) {
                            playerId = PartyScreen.this.groupManager.getGroupPlayerIdList().get(index);
                        }
                    } else {
                        int index = this.index + PartyScreen.this.indexStartOffset;
                        if (!PartyScreen.this.availablePlayers.isEmpty() && PartyScreen.this.availablePlayers.size() >= index) {
                            playerId = PartyScreen.this.availablePlayers.get(index);
                        }
                    }
                }

                if (playerId != null) {
                    if (NameHelper.isPlayerUUIDNotNull(client, playerId)) {
                        String playerName = NameHelper.getPlayerName(client, playerId, 0).getString();
                        if (PartyScreen.this.textRenderer.getWidth(playerName) > 60) {
                            context.drawTooltip(textRenderer, Text.of(playerName), mouseX, mouseY);
                        }
                    }
                }
            }
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
