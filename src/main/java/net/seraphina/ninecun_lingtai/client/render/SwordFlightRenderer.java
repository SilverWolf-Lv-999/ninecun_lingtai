package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.player.ClientSwordFlightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 御剑者脚下横陈的飞剑：放大摆平，剑尖随身体朝向。 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SwordFlightRenderer {
    private static final float SWORD_SCALE = 1.6F;
    /** 剑贴在脚底下方的高度（相对玩家原点）。 */
    private static final float SWORD_OFFSET_Y = -0.08F;

    private SwordFlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (!ClientSwordFlightManager.isFlying(player.getUUID())) {
            return;
        }
        ItemStack sword = ClientSwordFlightManager.getSword(player.getUUID());
        if (sword.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        // 剑尖随视线（头的朝向）而非身体——御剑时身体是侧着的。
        float headYaw = Mth.rotLerp(event.getPartialTick(), player.yHeadRotO, player.yHeadRot);
        poseStack.pushPose();
        poseStack.translate(0.0F, SWORD_OFFSET_Y, 0.0F);
        // 先随视线朝向转正，再放平剑身：物品模型的剑刃呈对角线，绕面内补 -45 度使剑尖指向正前。
        poseStack.mulPose(Axis.YP.rotationDegrees(-headYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.scale(SWORD_SCALE, SWORD_SCALE, SWORD_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                sword,
                ItemDisplayContext.FIXED,
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                poseStack,
                event.getMultiBufferSource(),
                player.level(),
                player.getId()
        );
        poseStack.popPose();
    }
}
