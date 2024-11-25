package net.partyaddon.mixin.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.partyaddon.network.packet.MapPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.partyaddon.access.compat.WorldMapAccess;
import net.partyaddon.network.PartyAddonServerPacket;
import net.partyaddon.util.NameHelper;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.gui.CursorBox;
import xaero.map.gui.GuiMap;
import xaero.map.gui.ScreenBase;

@Environment(EnvType.CLIENT)
@Mixin(GuiMap.class)
public abstract class GuiMapMixin extends ScreenBase implements WorldMapAccess {

    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;
    @Shadow
    private double scale;
    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    private int mouseBlockPosX;
    @Shadow
    private int mouseBlockPosZ;

    private List<UUID> groupPlayerUUIDList;
    private List<BlockPos> groupPlayerPosList;
    private List<Float> groupPlayerYawsList;
    private int syncCount;

    public GuiMapMixin(Screen parent, Screen escape, Text titleIn) {
        super(parent, escape, titleIn);
    }

    @Override
    public void setGroupPlayers(List<UUID> groupPlayerUUIDList, List<BlockPos> groupPlayerPosList, List<Float> groupPlayerYawsList) {
        this.groupPlayerUUIDList = groupPlayerUUIDList;
        this.groupPlayerPosList = groupPlayerPosList;
        this.groupPlayerYawsList = groupPlayerYawsList;
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/settings/ModSettings;renderArrow:Z", ordinal = 0))
    private void renderMixin(DrawContext context, int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo info) {
        if (WorldMap.settings.renderArrow) {
            if (groupPlayerUUIDList != null && !groupPlayerUUIDList.isEmpty()) {
                double leftBorder = this.cameraX - (double) (client.getWindow().getFramebufferWidth() / 2) / this.scale;
                double rightBorder = leftBorder + (double) client.getWindow().getFramebufferWidth() / this.scale;
                double topBorder = this.cameraZ - (double) (client.getWindow().getFramebufferHeight() / 2) / this.scale;
                double bottomBorder = topBorder + (double) client.getWindow().getFramebufferHeight() / this.scale;
                double scaleMultiplier = this.getScaleMultiplier(Math.min(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight()));
                double multiplier = scaleMultiplier / this.scale + 1;

                String playerNames = "";
                for (int i = 0; i < groupPlayerUUIDList.size(); i++) {
                    VertexConsumer regularUIObjectConsumer = this.mapProcessor.getCvc().getRenderTypeBuffers().getBuffer(CustomRenderTypes.GUI_BILINEAR);
                    boolean toTheLeft = this.groupPlayerPosList.get(i).getX() < leftBorder;
                    boolean toTheRight = this.groupPlayerPosList.get(i).getX() > rightBorder;
                    boolean down = this.groupPlayerPosList.get(i).getZ() > bottomBorder;
                    boolean up = this.groupPlayerPosList.get(i).getZ() < topBorder;

                    if (!toTheLeft && !toTheRight && !up && !down) {
                        this.setColourBuffer(0.0F, 0.0F, 0.0F, 0.9F);
                        this.drawArrowOnMap(context.getMatrices(), regularUIObjectConsumer, this.groupPlayerPosList.get(i).getX() - this.cameraX,
                                this.groupPlayerPosList.get(i).getZ() + 2.0D * scaleMultiplier / this.scale - this.cameraZ, this.groupPlayerYawsList.get(i), scaleMultiplier / this.scale);

                        this.setColourBuffer(0.0F, 0.55F, 1.0F, 1.0F);
                        this.drawArrowOnMap(context.getMatrices(), regularUIObjectConsumer, this.groupPlayerPosList.get(i).getX() - this.cameraX, this.groupPlayerPosList.get(i).getZ() - this.cameraZ,
                                this.groupPlayerYawsList.get(i), scaleMultiplier / this.scale);

                        if (isPointWithinBounds((int) (this.groupPlayerPosList.get(i).getX()), (int) (this.groupPlayerPosList.get(i).getZ()), (int) (8 * multiplier), (int) (8 * multiplier),
                                this.mouseBlockPosX, this.mouseBlockPosZ)) {
                            playerNames += NameHelper.getPlayerName(client, this.groupPlayerUUIDList.get(i), 0).getString() + " \n ";
                        }
                    } else {
                        double arrowX = this.groupPlayerPosList.get(i).getX();
                        double arrowZ = this.groupPlayerPosList.get(i).getZ();
                        float a = 0.0F;
                        if (toTheLeft) {
                            a = up ? 1.5F : (down ? 0.5F : 1.0F);
                            arrowX = leftBorder;
                        } else if (toTheRight) {
                            a = up ? 2.5F : (down ? 3.5F : 3.0F);
                            arrowX = rightBorder;
                        }

                        if (down) {
                            arrowZ = bottomBorder;
                        } else if (up) {
                            if (a == 0.0F) {
                                a = 2.0F;
                            }

                            arrowZ = topBorder;
                        }

                        this.setColourBuffer(0.0F, 0.0F, 0.0F, 0.9F);
                        this.drawFarArrowOnMap(context.getMatrices(), regularUIObjectConsumer, arrowX - this.cameraX, arrowZ + 2.0D * scaleMultiplier / this.scale - this.cameraZ,
                                this.groupPlayerYawsList.get(i), scaleMultiplier / this.scale);
                        this.setColourBuffer(0.0F, 0.55F, 1.0F, 1.0F);
                        this.drawFarArrowOnMap(context.getMatrices(), regularUIObjectConsumer, arrowX - this.cameraX, arrowZ - this.cameraZ, this.groupPlayerYawsList.get(i),
                                scaleMultiplier / this.scale);

                        if (isPointWithinBounds((int) (this.groupPlayerPosList.get(i).getX()), (int) (this.groupPlayerPosList.get(i).getZ()), (int) (8 * multiplier), (int) (8 * multiplier),
                                this.mouseBlockPosX, this.mouseBlockPosZ)) {
                            playerNames += NameHelper.getPlayerName(client, this.groupPlayerUUIDList.get(i), 0).getString() + " \n ";
                        }
                    }
                }
                if (!playerNames.equals("")) {
                    // MatrixStack stack = new MatrixStack();
                    CursorBox playerNamesTooltip = new CursorBox(playerNames);
                    playerNamesTooltip.drawBox(context, scaledMouseX, scaledMouseY, width, height);
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTailMixin(DrawContext context, int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo info) {
        this.syncCount++;
        if (this.syncCount != 0 && this.syncCount % 200 == 0) {
            ClientPlayNetworking.send(new MapPacket(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        }
    }

    private boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
        return pointX < (x + width / 2) && pointX > (x - width / 2) && pointY < (y + height / 2) && pointY > (y - height / 2);
    }

    @Shadow
    private void setColourBuffer(float r, float g, float b, float a) {
    }

    @Shadow
    public void drawFarArrowOnMap(MatrixStack matrixStack, VertexConsumer guiLinearBuffer, double x, double z, float angle, double sc) {
    }

    @Shadow
    public void drawArrowOnMap(MatrixStack matrixStack, VertexConsumer guiLinearBuffer, double x, double z, float angle, double sc) {
    }

    @Shadow
    private double getScaleMultiplier(int screenShortSide) {
        return 0D;
    }

}
