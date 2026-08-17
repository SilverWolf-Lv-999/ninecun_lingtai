package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.seraphina.ninecun_lingtai.client.BaseModel;
import net.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import net.seraphina.ninecun_lingtai.utility.BookItemUtility;

import java.util.List;

public final class BookRenderer extends BaseModel {
    public static final ModelResourceLocation BASE_MODEL_ID = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "book_base"),
            "inventory"
    );
    private static final float LABEL_LEFT = 0.265625F;
    private static final float LABEL_RIGHT = 0.34375F;
    private static final float LABEL_BOTTOM = 0.484375F;
    private static final float LABEL_TOP = 0.984375F;
    private static final float LABEL_PADDING = 0.00390625F;
    private static final float LABEL_Z = 0.4365F;
    private static final float CUSTOM_FONT_RENDER_HEIGHT = 48.0F;
    private static final int TITLE_COLOR = 0xFF2B1A10;

    public static final BookRenderer INSTANCE = new BookRenderer();

    @Override
    public void renderByItem(
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BakedModel baseModel = Minecraft.getInstance().getModelManager().getModel(BASE_MODEL_ID);
        boolean leftHand = isLeftHand(displayContext);
        renderBookBase(
                itemStack,
                displayContext,
                leftHand,
                baseModel,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
        BookItemUtility.getTitle(itemStack).ifPresent(title -> renderTitle(
                title,
                displayContext,
                leftHand,
                baseModel,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        ));
    }

    private static void renderBookBase(
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            BakedModel baseModel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().render(
                itemStack,
                displayContext,
                leftHand,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                baseModel
        );
        poseStack.popPose();
    }

    private static void renderTitle(
            String title,
            ItemDisplayContext displayContext,
            boolean leftHand,
            BakedModel baseModel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        List<Component> symbols = title.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .map(BookRenderer::createTitleSymbol)
                .toList();
        if (symbols.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        float widestSymbol = symbols.stream()
                .map(font::width)
                .max(Integer::compareTo)
                .orElse(1);
        float availableWidth = LABEL_RIGHT - LABEL_LEFT - LABEL_PADDING * 2.0F;
        float availableHeight = LABEL_TOP - LABEL_BOTTOM - LABEL_PADDING * 2.0F;
        float titleHeight = symbols.size() * CUSTOM_FONT_RENDER_HEIGHT;
        float scale = Math.min(
                availableWidth / Math.max(1.0F, widestSymbol),
                availableHeight / titleHeight
        );
        float titleTop = (LABEL_TOP + LABEL_BOTTOM + titleHeight * scale) * 0.5F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        baseModel.getTransforms().getTransform(displayContext).apply(leftHand, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate((LABEL_LEFT + LABEL_RIGHT) * 0.5F, titleTop, LABEL_Z);
        poseStack.scale(scale, -scale, scale);
        for (int symbolIndex = 0; symbolIndex < symbols.size(); symbolIndex++) {
            Component symbol = symbols.get(symbolIndex);
            font.drawInBatch(
                    symbol,
                    -font.width(symbol) * 0.5F,
                    symbolIndex * CUSTOM_FONT_RENDER_HEIGHT,
                    TITLE_COLOR,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.POLYGON_OFFSET,
                    0,
                    packedLight
            );
        }
        poseStack.popPose();
    }

    private static Component createTitleSymbol(String symbol) {
        return Component.literal(symbol).withStyle(
                style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
        );
    }

    private static boolean isLeftHand(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }
}
