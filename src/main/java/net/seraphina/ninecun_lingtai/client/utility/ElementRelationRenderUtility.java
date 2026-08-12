package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.api.element.ElementRelation;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.client.render.ElementFlowShader;
import net.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ElementRelationRenderUtility {
    private static final int PATH_SEGMENTS = 28;
    private static final int ORBIT_PARTICLE_COUNT = 9;
    private static final int TRAVEL_PARTICLE_COUNT = 11;
    private static final float WATER_RIPPLE_DURATION = 2350.0F;

    public static void drawSourceAura(
            GuiGraphics guiGraphics,
            ElementType sourceElement,
            ScreenPoint sourcePosition,
            float formationRadius,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        int sourceColor = SpiritualRootUtility.getElementColor(sourceElement);
        switch (sourceElement) {
            case WATER -> drawWaterRippleAura(
                    guiGraphics,
                    sourcePosition,
                    formationRadius,
                    nodeRadius,
                    currentTime,
                    alpha
            );
            case FIRE -> drawFireAura(
                    guiGraphics,
                    sourcePosition,
                    formationRadius,
                    nodeRadius,
                    sourceColor,
                    currentTime,
                    alpha
            );
            case WOOD -> drawWoodAura(guiGraphics, sourcePosition, nodeRadius, currentTime, alpha);
            case EARTH -> drawEarthAura(guiGraphics, sourcePosition, nodeRadius, sourceColor, currentTime, alpha);
            case METAL -> drawMetalAura(guiGraphics, sourcePosition, nodeRadius, sourceColor, currentTime, alpha);
            case YIN_YANG, THUNDER, CHAOS -> {
            }
        }
    }

    public static void drawRelation(
            GuiGraphics guiGraphics,
            ElementType sourceElement,
            ElementType targetElement,
            ScreenPoint sourcePosition,
            ScreenPoint targetPosition,
            float formationRadius,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F || sourceElement == targetElement) {
            return;
        }
        ElementRelation relation = sourceElement.getRelationTo(targetElement);
        if (relation != ElementRelation.GENERATES && relation != ElementRelation.OVERCOMES) {
            return;
        }

        boolean generation = relation == ElementRelation.GENERATES;
        float targetInset = nodeRadius * (generation ? 1.48F : 1.95F);
        ScreenPath trimmedPath = trimPath(sourcePosition, targetPosition, nodeRadius * 1.5F, targetInset);
        float curveDirection = ((sourceElement.ordinal() + targetElement.ordinal()) & 1) == 0 ? 1.0F : -1.0F;
        float curvature = nodeRadius * (generation ? 0.82F : 1.08F) * curveDirection;
        float motion = Mth.sin(currentTime * 0.0011F + sourceElement.ordinal() * 1.7F) * nodeRadius * 0.15F;
        List<ScreenPoint> mainPath = buildCurvedPath(trimmedPath.start(), trimmedPath.end(), curvature + motion);
        int sourceColor = SpiritualRootUtility.getElementColor(sourceElement);
        float thickness = Mth.clamp(nodeRadius * getThicknessScale(sourceElement), 2.2F, 7.0F);
        float flowMode = generation ? 0.0F : 1.0F;

        drawFlowPath(
                guiGraphics,
                mainPath,
                thickness,
                sourceColor,
                sourceElement,
                currentTime,
                1.0F,
                flowMode,
                getFlowSeed(sourceElement, targetElement),
                alpha
        );
        if (sourceElement == ElementType.WATER || sourceElement == ElementType.FIRE) {
            List<ScreenPoint> secondaryPath = buildCurvedPath(
                    trimmedPath.start(),
                    trimmedPath.end(),
                    curvature * -0.58F - motion * 0.4F
            );
            drawFlowPath(
                    guiGraphics,
                    secondaryPath,
                    thickness * (sourceElement == ElementType.WATER ? 0.52F : 0.38F),
                    ColorUtility.brighten(sourceColor, sourceElement == ElementType.WATER ? 0.38F : 0.62F),
                    sourceElement,
                    currentTime,
                    1.0F,
                    flowMode,
                    getFlowSeed(targetElement, sourceElement) + 0.37F,
                    alpha * 0.5F
            );
        }
        drawTravelParticles(
                guiGraphics,
                mainPath,
                sourceElement,
                sourceColor,
                currentTime,
                generation ? 1.0F : 0.94F,
                alpha * (generation ? 0.84F : 0.72F)
        );

        if (generation) {
            drawGenerationResponse(
                    guiGraphics,
                    sourceElement,
                    targetPosition,
                    nodeRadius,
                    currentTime,
                    alpha
            );
        } else {
            drawOvercomeResponse(
                    guiGraphics,
                    sourceElement,
                    targetElement,
                    trimmedPath.end(),
                    targetPosition,
                    nodeRadius,
                    currentTime,
                    alpha
            );
        }
        if (sourceElement == ElementType.WATER) {
            drawWaterContactFeedback(
                    guiGraphics,
                    sourcePosition,
                    targetPosition,
                    formationRadius,
                    nodeRadius,
                    currentTime,
                    alpha
            );
        } else if (sourceElement == ElementType.FIRE) {
            drawFireContactFeedback(
                    guiGraphics,
                    sourcePosition,
                    targetPosition,
                    formationRadius,
                    nodeRadius,
                    currentTime,
                    alpha
            );
        }
    }

    public static void drawYinYangBalance(
            GuiGraphics guiGraphics,
            ScreenPoint centerPosition,
            float taijiRadius,
            List<ScreenPoint> elementPositions,
            Set<ElementType> selectedElements,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F || elementPositions.size() != 5) {
            return;
        }
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.005F);
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                centerPosition.x(),
                centerPosition.y(),
                taijiRadius * (1.7F + pulse * 0.1F),
                ColorUtility.withAlpha(0xE8C96A, alpha * (0.055F + pulse * 0.035F))
        );
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                centerPosition.x(),
                centerPosition.y(),
                taijiRadius * (1.55F + pulse * 0.08F),
                1.35F,
                ColorUtility.withAlpha(0xF4D77B, alpha * 0.62F)
        );
        float rotation = currentTime * 0.04F;
        for (int arc = 0; arc < 4; arc++) {
            float start = rotation + arc * 90.0F;
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    centerPosition.x(),
                    centerPosition.y(),
                    taijiRadius * 1.9F,
                    start,
                    start + 46.0F,
                    1.1F,
                    ColorUtility.withAlpha(0xFFF0B4, alpha * 0.48F)
            );
        }

        for (int index = 0; index < elementPositions.size(); index++) {
            ElementType targetElement = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(index);
            ScreenPoint targetPosition = elementPositions.get(index);
            boolean excess = selectedElements.contains(targetElement);
            boolean balanceFocus = excess || isGenerationNeighbor(targetElement, selectedElements);
            float bubbleAlpha = alpha * (balanceFocus ? 0.74F : 0.22F);
            drawBalanceBubble(
                    guiGraphics,
                    targetPosition,
                    nodeRadius,
                    excess,
                    balanceFocus,
                    currentTime + index * 173L,
                    bubbleAlpha
            );
            if (!balanceFocus) {
                continue;
            }
            ScreenPath path = trimPath(
                    centerPosition,
                    targetPosition,
                    taijiRadius * 1.95F,
                    nodeRadius * 1.72F
            );
            List<ScreenPoint> balancePath = buildCurvedPath(
                    path.start(),
                    path.end(),
                    nodeRadius * (((index & 1) == 0 ? 0.55F : -0.55F))
            );
            int balanceColor = excess ? 0xD7DBE2 : 0xF1DFC0;
            drawFlowPath(
                    guiGraphics,
                    balancePath,
                    Mth.clamp(nodeRadius * 0.1F, 1.45F, 2.8F),
                    balanceColor,
                    ElementType.YIN_YANG,
                    currentTime,
                    excess ? -1.0F : 1.0F,
                    excess ? 3.0F : 2.0F,
                    index * 0.19F,
                    alpha * (excess ? 0.5F : 0.42F)
            );
        }
    }

    private static void drawWaterRippleAura(
            GuiGraphics guiGraphics,
            ScreenPoint sourcePosition,
            float formationRadius,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        for (int ripple = 0; ripple < 3; ripple++) {
            float progress = getWaterRippleProgress(currentTime, ripple);
            float radius = Mth.lerp(progress, nodeRadius * 1.45F, formationRadius * 1.03F);
            float fade = Mth.sin(progress * Mth.PI) * (1.0F - progress * 0.38F);
            AptitudeRenderUtility.drawRing(
                    guiGraphics,
                    sourcePosition.x(),
                    sourcePosition.y(),
                    radius,
                    0.8F + (1.0F - progress) * 1.05F,
                    ColorUtility.withAlpha(0x65C7FF, alpha * fade * 0.42F)
            );
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    sourcePosition.x(),
                    sourcePosition.y(),
                    radius * 0.985F,
                    currentTime * 0.022F + ripple * 117.0F,
                    currentTime * 0.022F + ripple * 117.0F + 58.0F,
                    1.4F,
                    ColorUtility.withAlpha(0xBDEBFF, alpha * fade * 0.5F)
            );
        }
        List<Particle> droplets = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            float angle = index * 30.0F + currentTime * 0.018F;
            float radius = nodeRadius * (1.55F + 0.18F * Mth.sin(index * 1.9F + currentTime * 0.003F));
            droplets.add(orbitParticle(sourcePosition, radius, angle, 1.25F, 0xA7E4FF, alpha * 0.68F));
        }
        drawParticles(guiGraphics, droplets);
    }

    private static void drawFireAura(
            GuiGraphics guiGraphics,
            ScreenPoint sourcePosition,
            float formationRadius,
            float nodeRadius,
            int sourceColor,
            long currentTime,
            float alpha
    ) {
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                sourcePosition.x(),
                sourcePosition.y(),
                nodeRadius * 1.72F,
                ColorUtility.withAlpha(0xFF3A20, alpha * 0.08F)
        );
        List<Particle> embers = new ArrayList<>();
        for (int index = 0; index < 18; index++) {
            float phase = fract(currentTime * 0.00042F + index * 0.137F);
            float side = Mth.sin(index * 2.17F) * nodeRadius * 1.45F * (0.35F + phase * 0.65F);
            float height = nodeRadius * (0.75F + phase * 2.25F);
            float flicker = 0.55F + 0.45F * Mth.sin(currentTime * 0.014F + index);
            embers.add(new Particle(
                    sourcePosition.x() + side,
                    sourcePosition.y() + nodeRadius * 0.82F - height,
                    0.9F + (1.0F - phase) * 1.6F,
                    ColorUtility.withAlpha(
                            index % 3 == 0 ? 0xFFE08A : ColorUtility.brighten(sourceColor, 0.2F),
                            alpha * (1.0F - phase) * flicker
                    ),
                    index * 0.61F
            ));
        }
        for (int front = 0; front < 3; front++) {
            float progress = getFireFrontProgress(currentTime, front);
            float radius = Mth.lerp(progress, nodeRadius * 1.42F, formationRadius * 1.0F);
            float fade = Mth.sin(progress * Mth.PI) * (1.0F - progress * 0.3F);
            float rotation = currentTime * 0.034F + front * 47.0F;
            for (int segment = 0; segment < 8; segment++) {
                float start = rotation + segment * 45.0F + Mth.sin(segment * 1.8F + currentTime * 0.006F) * 5.0F;
                float arcLength = 18.0F + 9.0F * Mth.sin(currentTime * 0.009F + segment * 1.4F);
                AptitudeRenderUtility.drawArc(
                        guiGraphics,
                        sourcePosition.x(),
                        sourcePosition.y(),
                        radius,
                        start,
                        start + arcLength,
                        0.8F + (1.0F - progress) * 1.55F,
                        ColorUtility.withAlpha(
                                segment % 3 == 0 ? 0xFFE092 : sourceColor,
                                alpha * fade * (segment % 3 == 0 ? 0.5F : 0.34F)
                        )
                );
                if ((segment + front) % 2 == 0) {
                    float particleAngle = (float) Math.toRadians(start + arcLength * 0.5F);
                    embers.add(new Particle(
                            sourcePosition.x() + Mth.cos(particleAngle) * radius,
                            sourcePosition.y() + Mth.sin(particleAngle) * radius - nodeRadius * progress * 0.18F,
                            0.9F + (1.0F - progress) * 1.15F,
                            ColorUtility.withAlpha(segment % 3 == 0 ? 0xFFF0B0 : 0xFF6B38, alpha * fade * 0.62F),
                            particleAngle
                    ));
                }
            }
        }
        drawParticles(guiGraphics, embers);
        for (int flame = 0; flame < 5; flame++) {
            float angle = -150.0F + flame * 19.0F + Mth.sin(currentTime * 0.007F + flame) * 7.0F;
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    sourcePosition.x(),
                    sourcePosition.y() + nodeRadius * 0.22F,
                    nodeRadius * (1.26F + flame * 0.09F),
                    angle,
                    angle + 42.0F,
                    1.35F + flame * 0.12F,
                    ColorUtility.withAlpha(flame < 2 ? 0xFFE58F : sourceColor, alpha * (0.5F - flame * 0.055F))
            );
        }
    }

    private static void drawWoodAura(
            GuiGraphics guiGraphics,
            ScreenPoint sourcePosition,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        float growth = 0.5F + 0.5F * Mth.sin(currentTime * 0.0027F);
        for (int branch = 0; branch < 3; branch++) {
            float rotation = currentTime * 0.009F + branch * 120.0F;
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    sourcePosition.x(),
                    sourcePosition.y(),
                    nodeRadius * (1.24F + branch * 0.22F + growth * 0.08F),
                    rotation,
                    rotation + 96.0F,
                    1.0F + branch * 0.24F,
                    ColorUtility.withAlpha(branch == 0 ? 0xB4EA8D : 0x62B968, alpha * (0.58F - branch * 0.1F))
            );
        }
        List<Particle> leaves = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            float angle = currentTime * 0.014F + index * 36.0F;
            float radius = nodeRadius * (1.35F + 0.38F * Mth.sin(currentTime * 0.002F + index * 1.4F));
            leaves.add(orbitParticle(sourcePosition, radius, angle, 1.5F, index % 2 == 0 ? 0xA9E77F : 0x5EBB62, alpha * 0.65F));
        }
        drawParticles(guiGraphics, leaves);
    }

    private static void drawEarthAura(
            GuiGraphics guiGraphics,
            ScreenPoint sourcePosition,
            float nodeRadius,
            int sourceColor,
            long currentTime,
            float alpha
    ) {
        List<Particle> fragments = new ArrayList<>();
        for (int index = 0; index < 14; index++) {
            float angle = index * 25.714F + Mth.sin(currentTime * 0.0018F + index) * 6.0F;
            float radius = nodeRadius * (1.28F + (index % 3) * 0.18F);
            fragments.add(orbitParticle(
                    sourcePosition,
                    radius,
                    angle,
                    1.25F + index % 3 * 0.45F,
                    index % 2 == 0 ? ColorUtility.brighten(sourceColor, 0.28F) : 0x8D653A,
                    alpha * (0.42F + (index % 4) * 0.06F)
            ));
        }
        drawParticles(guiGraphics, fragments);
        for (int arc = 0; arc < 5; arc++) {
            float start = arc * 72.0F + currentTime * 0.004F;
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    sourcePosition.x(),
                    sourcePosition.y(),
                    nodeRadius * 1.62F,
                    start,
                    start + 31.0F,
                    1.7F,
                    ColorUtility.withAlpha(sourceColor, alpha * 0.44F)
            );
        }
    }

    private static void drawMetalAura(
            GuiGraphics guiGraphics,
            ScreenPoint sourcePosition,
            float nodeRadius,
            int sourceColor,
            long currentTime,
            float alpha
    ) {
        float rotation = currentTime * 0.036F;
        for (int slash = 0; slash < 4; slash++) {
            float angle = (float) Math.toRadians(rotation + slash * 90.0F);
            float innerRadius = nodeRadius * 1.05F;
            float outerRadius = nodeRadius * (1.78F + 0.12F * Mth.sin(currentTime * 0.006F + slash));
            ScreenPoint start = new ScreenPoint(
                    sourcePosition.x() + Mth.cos(angle) * innerRadius,
                    sourcePosition.y() + Mth.sin(angle) * innerRadius
            );
            ScreenPoint end = new ScreenPoint(
                    sourcePosition.x() + Mth.cos(angle) * outerRadius,
                    sourcePosition.y() + Mth.sin(angle) * outerRadius
            );
            AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    start,
                    end,
                    slash % 2 == 0 ? 1.45F : 0.8F,
                    ColorUtility.withAlpha(
                            slash % 2 == 0 ? 0xFFF5C7 : ColorUtility.brighten(sourceColor, 0.2F),
                            alpha * (slash % 2 == 0 ? 0.72F : 0.42F)
                    )
            );
        }
        AptitudeRenderUtility.drawArc(
                guiGraphics,
                sourcePosition.x(),
                sourcePosition.y(),
                nodeRadius * 1.52F,
                -rotation,
                -rotation + 132.0F,
                1.1F,
                ColorUtility.withAlpha(0xFFF0A8, alpha * 0.56F)
        );
    }

    private static void drawGenerationResponse(
            GuiGraphics guiGraphics,
            ElementType sourceElement,
            ScreenPoint targetPosition,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        int sourceColor = SpiritualRootUtility.getElementColor(sourceElement);
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.0058F + sourceElement.ordinal());
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                targetPosition.x(),
                targetPosition.y(),
                nodeRadius * (1.42F + pulse * 0.1F),
                ColorUtility.withAlpha(ColorUtility.brighten(sourceColor, 0.36F), alpha * (0.035F + pulse * 0.035F))
        );
        List<Particle> orbitParticles = new ArrayList<>();
        float rotation = currentTime * getOrbitSpeed(sourceElement);
        for (int index = 0; index < ORBIT_PARTICLE_COUNT; index++) {
            float angle = rotation + index * (360.0F / ORBIT_PARTICLE_COUNT);
            float radius = nodeRadius * (1.42F + 0.14F * Mth.sin(currentTime * 0.003F + index * 1.31F));
            int particleColor = switch (sourceElement) {
                case WATER -> index % 3 == 0 ? 0xD8F5FF : 0x65C7FF;
                case FIRE -> index % 3 == 0 ? 0xFFE58A : 0xFF7044;
                case WOOD -> index % 2 == 0 ? 0xB7EB83 : 0x62BC65;
                case EARTH -> index % 2 == 0 ? 0xDFA76A : 0x9A6D3D;
                case METAL -> index % 2 == 0 ? 0xFFF5C7 : 0xDDBB62;
                case YIN_YANG, THUNDER, CHAOS -> 0xFFFFFF;
            };
            orbitParticles.add(orbitParticle(
                    targetPosition,
                    radius,
                    angle,
                    sourceElement == ElementType.WATER ? 1.4F : 1.1F + index % 3 * 0.3F,
                    particleColor,
                    alpha * (0.52F + pulse * 0.18F)
            ));
        }
        drawParticles(guiGraphics, orbitParticles);
        if (sourceElement == ElementType.WATER) {
            for (int arc = 0; arc < 3; arc++) {
                float start = rotation * 0.72F + arc * 120.0F;
                AptitudeRenderUtility.drawArc(
                        guiGraphics,
                        targetPosition.x(),
                        targetPosition.y(),
                        nodeRadius * (1.5F + arc * 0.055F),
                        start,
                        start + 48.0F,
                        1.05F,
                        ColorUtility.withAlpha(0x8AD9FF, alpha * 0.48F)
                );
            }
        }
    }

    private static void drawOvercomeResponse(
            GuiGraphics guiGraphics,
            ElementType sourceElement,
            ElementType targetElement,
            ScreenPoint impactPosition,
            ScreenPoint targetPosition,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        float deltaX = impactPosition.x() - targetPosition.x();
        float deltaY = impactPosition.y() - targetPosition.y();
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0.0001F) {
            return;
        }
        float directionX = deltaX / length;
        float directionY = deltaY / length;
        float tangentX = -directionY;
        float tangentY = directionX;
        float pulse = 0.5F + 0.5F * Mth.sin(currentTime * 0.012F + sourceElement.ordinal());
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                impactPosition.x(),
                impactPosition.y(),
                2.2F + pulse * 3.8F,
                ColorUtility.withAlpha(0xFFFFFF, alpha * (1.0F - pulse) * 0.28F)
        );
        switch (sourceElement) {
            case WATER -> drawSteamDissipation(
                    guiGraphics,
                    impactPosition,
                    tangentX,
                    tangentY,
                    nodeRadius,
                    currentTime,
                    alpha
            );
            case FIRE -> drawSparkImpact(
                    guiGraphics,
                    impactPosition,
                    directionX,
                    directionY,
                    nodeRadius,
                    currentTime,
                    alpha
            );
            case WOOD -> drawRootImpact(
                    guiGraphics,
                    impactPosition,
                    tangentX,
                    tangentY,
                    nodeRadius,
                    currentTime,
                    alpha
            );
            case EARTH -> drawFragmentImpact(
                    guiGraphics,
                    impactPosition,
                    tangentX,
                    tangentY,
                    nodeRadius,
                    currentTime,
                    alpha
            );
            case METAL -> drawSlashImpact(
                    guiGraphics,
                    impactPosition,
                    tangentX,
                    tangentY,
                    nodeRadius,
                    currentTime,
                    alpha
            );
            case YIN_YANG, THUNDER, CHAOS -> {
            }
        }
        int targetColor = SpiritualRootUtility.getElementColor(targetElement);
        AptitudeRenderUtility.drawArc(
                guiGraphics,
                targetPosition.x(),
                targetPosition.y(),
                nodeRadius * 1.56F,
                (float) Math.toDegrees(Math.atan2(directionY, directionX)) - 31.0F,
                (float) Math.toDegrees(Math.atan2(directionY, directionX)) + 31.0F,
                1.15F,
                ColorUtility.withAlpha(ColorUtility.brighten(targetColor, 0.28F), alpha * (0.24F + pulse * 0.18F))
        );
    }

    private static void drawSteamDissipation(
            GuiGraphics guiGraphics,
            ScreenPoint impactPosition,
            float tangentX,
            float tangentY,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        for (int index = 0; index < 8; index++) {
            float progress = fract(currentTime * 0.00058F + index * 0.161F);
            float side = Mth.sin(index * 2.4F) * nodeRadius * 0.42F * progress;
            float rise = nodeRadius * (0.25F + progress * 1.25F);
            float centerX = impactPosition.x() + tangentX * side;
            float centerY = impactPosition.y() + tangentY * side - rise;
            AptitudeRenderUtility.drawDisc(
                    guiGraphics,
                    centerX,
                    centerY,
                    1.1F + progress * 2.2F,
                    ColorUtility.withAlpha(index % 2 == 0 ? 0xE8F5F8 : 0xA7D9E6, alpha * (1.0F - progress) * 0.46F)
            );
        }
    }

    private static void drawSparkImpact(
            GuiGraphics guiGraphics,
            ScreenPoint impactPosition,
            float directionX,
            float directionY,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        float baseAngle = (float) Math.atan2(directionY, directionX);
        for (int index = 0; index < 9; index++) {
            float angle = baseAngle + Mth.lerp((float) index / 8.0F, -1.2F, 1.2F);
            float length = nodeRadius * (0.42F + 0.45F * Mth.sin(currentTime * 0.009F + index * 1.7F));
            ScreenPoint end = new ScreenPoint(
                    impactPosition.x() + Mth.cos(angle) * length,
                    impactPosition.y() + Mth.sin(angle) * length
            );
            AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    impactPosition,
                    end,
                    index % 3 == 0 ? 1.4F : 0.75F,
                    ColorUtility.withAlpha(index % 2 == 0 ? 0xFFF1A3 : 0xFF8B3D, alpha * 0.68F)
            );
        }
    }

    private static void drawRootImpact(
            GuiGraphics guiGraphics,
            ScreenPoint impactPosition,
            float tangentX,
            float tangentY,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        float sway = Mth.sin(currentTime * 0.006F) * nodeRadius * 0.12F;
        for (int side = -1; side <= 1; side += 2) {
            ScreenPoint middle = new ScreenPoint(
                    impactPosition.x() + tangentX * nodeRadius * 0.45F * side,
                    impactPosition.y() + tangentY * nodeRadius * 0.45F * side
            );
            ScreenPoint end = new ScreenPoint(
                    impactPosition.x() + tangentX * (nodeRadius + sway) * side,
                    impactPosition.y() + tangentY * (nodeRadius + sway) * side
            );
            AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    impactPosition,
                    middle,
                    1.5F,
                    ColorUtility.withAlpha(0x78C66D, alpha * 0.62F)
            );
            AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    middle,
                    end,
                    0.85F,
                    ColorUtility.withAlpha(0xB0E287, alpha * 0.5F)
            );
        }
    }

    private static void drawFragmentImpact(
            GuiGraphics guiGraphics,
            ScreenPoint impactPosition,
            float tangentX,
            float tangentY,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        List<Particle> fragments = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            float progress = fract(currentTime * 0.00036F + index * 0.173F);
            float side = (index - 4.5F) / 4.5F;
            fragments.add(new Particle(
                    impactPosition.x() + tangentX * side * nodeRadius * progress,
                    impactPosition.y() + tangentY * side * nodeRadius * progress + progress * progress * nodeRadius * 0.72F,
                    1.1F + (index % 3) * 0.45F,
                    ColorUtility.withAlpha(index % 2 == 0 ? 0xD09A5B : 0x7D5A35, alpha * (1.0F - progress) * 0.66F),
                    index * 0.67F
            ));
        }
        drawParticles(guiGraphics, fragments);
    }

    private static void drawSlashImpact(
            GuiGraphics guiGraphics,
            ScreenPoint impactPosition,
            float tangentX,
            float tangentY,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        float flash = 0.5F + 0.5F * Mth.sin(currentTime * 0.016F);
        for (int slash = -1; slash <= 1; slash += 2) {
            ScreenPoint start = new ScreenPoint(
                    impactPosition.x() - tangentX * nodeRadius * 0.62F * slash,
                    impactPosition.y() - tangentY * nodeRadius * 0.62F * slash
            );
            ScreenPoint end = new ScreenPoint(
                    impactPosition.x() + tangentX * nodeRadius * 0.62F * slash,
                    impactPosition.y() + tangentY * nodeRadius * 0.62F * slash
            );
            AptitudeRenderUtility.drawLine(
                    guiGraphics,
                    start,
                    end,
                    slash > 0 ? 1.65F : 0.75F,
                    ColorUtility.withAlpha(slash > 0 ? 0xFFF7D2 : 0xDABF72, alpha * (0.46F + flash * 0.36F))
            );
        }
    }

    private static void drawWaterContactFeedback(
            GuiGraphics guiGraphics,
            ScreenPoint sourcePosition,
            ScreenPoint targetPosition,
            float formationRadius,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        float targetDistance = distance(sourcePosition, targetPosition);
        float contactStrength = 0.0F;
        for (int ripple = 0; ripple < 3; ripple++) {
            float progress = getWaterRippleProgress(currentTime, ripple);
            float rippleRadius = Mth.lerp(progress, nodeRadius * 1.45F, formationRadius * 1.03F);
            float distanceDifference = Math.abs(rippleRadius - targetDistance);
            contactStrength = Math.max(
                    contactStrength,
                    1.0F - Mth.clamp(distanceDifference / (nodeRadius * 0.72F), 0.0F, 1.0F)
            );
        }
        if (contactStrength <= 0.0F) {
            return;
        }
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                targetPosition.x(),
                targetPosition.y(),
                nodeRadius * (1.2F + contactStrength * 0.24F),
                ColorUtility.withAlpha(0x8EDCFF, alpha * contactStrength * 0.12F)
        );
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                targetPosition.x(),
                targetPosition.y(),
                nodeRadius * (1.26F + contactStrength * 0.18F),
                1.0F,
                ColorUtility.withAlpha(0xBEEBFF, alpha * contactStrength * 0.52F)
        );
    }

    private static void drawFireContactFeedback(
            GuiGraphics guiGraphics,
            ScreenPoint sourcePosition,
            ScreenPoint targetPosition,
            float formationRadius,
            float nodeRadius,
            long currentTime,
            float alpha
    ) {
        float targetDistance = distance(sourcePosition, targetPosition);
        float contactStrength = 0.0F;
        for (int front = 0; front < 3; front++) {
            float progress = getFireFrontProgress(currentTime, front);
            float frontRadius = Mth.lerp(progress, nodeRadius * 1.42F, formationRadius);
            float distanceDifference = Math.abs(frontRadius - targetDistance);
            contactStrength = Math.max(
                    contactStrength,
                    1.0F - Mth.clamp(distanceDifference / (nodeRadius * 0.78F), 0.0F, 1.0F)
            );
        }
        if (contactStrength <= 0.0F) {
            return;
        }
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                targetPosition.x(),
                targetPosition.y(),
                nodeRadius * (1.18F + contactStrength * 0.2F),
                ColorUtility.withAlpha(0xFF7A3D, alpha * contactStrength * 0.1F)
        );
        for (int arc = 0; arc < 4; arc++) {
            float start = currentTime * 0.04F + arc * 90.0F;
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    targetPosition.x(),
                    targetPosition.y(),
                    nodeRadius * (1.35F + arc * 0.04F),
                    start,
                    start + 27.0F,
                    1.0F,
                    ColorUtility.withAlpha(arc % 2 == 0 ? 0xFFE091 : 0xFF6C3C, alpha * contactStrength * 0.55F)
            );
        }
    }

    private static void drawBalanceBubble(
            GuiGraphics guiGraphics,
            ScreenPoint targetPosition,
            float nodeRadius,
            boolean excess,
            boolean balanceFocus,
            long currentTime,
            float alpha
    ) {
        float progress = fract(currentTime / 1900.0F);
        float bubbleProgress = excess ? 1.0F - progress : progress;
        float radius = nodeRadius * Mth.lerp(bubbleProgress, 1.22F, 1.72F);
        float fade = Mth.sin(progress * Mth.PI);
        int bubbleColor = excess ? 0xC9CFD9 : 0xF2E5CE;
        AptitudeRenderUtility.drawDisc(
                guiGraphics,
                targetPosition.x(),
                targetPosition.y(),
                nodeRadius * 1.38F,
                ColorUtility.withAlpha(bubbleColor, alpha * (balanceFocus ? 0.055F : 0.025F))
        );
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                targetPosition.x(),
                targetPosition.y(),
                radius,
                balanceFocus ? 1.05F : 0.55F,
                ColorUtility.withAlpha(bubbleColor, alpha * fade * (balanceFocus ? 0.46F : 0.18F))
        );
        if (balanceFocus) {
            float orbitAngle = currentTime * (excess ? -0.018F : 0.021F);
            List<Particle> balanceParticles = new ArrayList<>();
            for (int particle = 0; particle < 3; particle++) {
                float angle = orbitAngle + particle * 120.0F;
                balanceParticles.add(orbitParticle(
                        targetPosition,
                        nodeRadius * 1.48F,
                        angle,
                        1.15F,
                        bubbleColor,
                        alpha * 0.58F
                ));
            }
            drawParticles(guiGraphics, balanceParticles);
        }
    }

    private static boolean isGenerationNeighbor(ElementType targetElement, Set<ElementType> selectedElements) {
        for (ElementType selectedElement : selectedElements) {
            if (!selectedElement.isFiveElement()) {
                continue;
            }
            ElementRelation relation = selectedElement.getRelationTo(targetElement);
            if (relation == ElementRelation.GENERATES || relation == ElementRelation.GENERATED_BY) {
                return true;
            }
        }
        return false;
    }

    private static void drawTravelParticles(
            GuiGraphics guiGraphics,
            List<ScreenPoint> path,
            ElementType sourceElement,
            int sourceColor,
            long currentTime,
            float maximumProgress,
            float alpha
    ) {
        List<Particle> particles = new ArrayList<>();
        float speed = getParticleSpeed(sourceElement);
        for (int index = 0; index < TRAVEL_PARTICLE_COUNT; index++) {
            float progress = fract(currentTime * speed + index / (float) TRAVEL_PARTICLE_COUNT);
            progress *= maximumProgress;
            ScreenPoint position = interpolatePath(path, progress);
            float flicker = 0.55F + 0.45F * Mth.sin(currentTime * 0.011F + index * 2.1F);
            float size = switch (sourceElement) {
                case WATER -> 1.1F + (index % 3) * 0.35F;
                case FIRE -> 0.8F + (index % 4) * 0.28F;
                case WOOD -> 1.15F + (index % 2) * 0.55F;
                case EARTH -> 1.25F + (index % 3) * 0.45F;
                case METAL -> 0.75F + (index % 3) * 0.3F;
                case YIN_YANG, THUNDER, CHAOS -> 1.0F;
            };
            int particleColor = index % 4 == 0
                    ? ColorUtility.brighten(sourceColor, 0.62F)
                    : ColorUtility.brighten(sourceColor, 0.22F);
            particles.add(new Particle(
                    position.x(),
                    position.y(),
                    size,
                    ColorUtility.withAlpha(particleColor, alpha * flicker),
                    index * 0.71F + currentTime * 0.001F
            ));
        }
        drawParticles(guiGraphics, particles);
    }

    private static void drawFlowPath(
            GuiGraphics guiGraphics,
            List<ScreenPoint> points,
            float thickness,
            int color,
            ElementType element,
            long currentTime,
            float direction,
            float flowMode,
            float flowSeed,
            float alpha
    ) {
        if (points.size() < 2 || thickness <= 0.0F || alpha <= 0.0F) {
            return;
        }
        ShaderInstance shaderInstance = ElementFlowShader.getElementFlowShader();
        if (shaderInstance == null) {
            int fallbackColor = ColorUtility.withAlpha(color, alpha * 0.58F);
            for (int index = 0; index < points.size() - 1; index++) {
                AptitudeRenderUtility.drawLine(guiGraphics, points.get(index), points.get(index + 1), thickness * 0.46F, fallbackColor);
            }
            return;
        }

        float totalLength = calculateLength(points);
        if (totalLength <= 0.0001F) {
            return;
        }
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("FlowTime").set((currentTime % 240000L) / 1000.0F);
        shaderInstance.safeGetUniform("FlowStyle").set(getFlowStyle(element));
        shaderInstance.safeGetUniform("FlowMode").set(flowMode);
        shaderInstance.safeGetUniform("FlowSeed").set(flowSeed);
        shaderInstance.safeGetUniform("FlowCutoff").set(1.0F);
        shaderInstance.safeGetUniform("FlowDirection").set(direction);

        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        float traversedLength = 0.0F;
        int renderedColor = ColorUtility.withAlpha(color, alpha);
        for (int index = 0; index < points.size() - 1; index++) {
            ScreenPoint start = points.get(index);
            ScreenPoint end = points.get(index + 1);
            float segmentLength = distance(start, end);
            float startProgress = traversedLength / totalLength;
            float endProgress = (traversedLength + segmentLength) / totalLength;
            appendFlowQuad(
                    bufferBuilder,
                    matrix,
                    start,
                    end,
                    thickness,
                    renderedColor,
                    startProgress,
                    endProgress
            );
            traversedLength += segmentLength;
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawParticles(GuiGraphics guiGraphics, List<Particle> particles) {
        if (particles.isEmpty()) {
            return;
        }
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Particle particle : particles) {
            appendParticle(bufferBuilder, matrix, particle);
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void appendParticle(BufferBuilder bufferBuilder, Matrix4f matrix, Particle particle) {
        if ((particle.color() >>> 24) == 0 || particle.size() <= 0.0F) {
            return;
        }
        float axisX = Mth.cos(particle.rotation()) * particle.size();
        float axisY = Mth.sin(particle.rotation()) * particle.size();
        float normalX = -axisY * 0.55F;
        float normalY = axisX * 0.55F;
        vertex(bufferBuilder, matrix, particle.x() + axisX, particle.y() + axisY, particle.color());
        vertex(bufferBuilder, matrix, particle.x() + normalX, particle.y() + normalY, particle.color());
        vertex(bufferBuilder, matrix, particle.x() - axisX, particle.y() - axisY, particle.color());
        vertex(bufferBuilder, matrix, particle.x() - normalX, particle.y() - normalY, particle.color());
    }

    private static void appendFlowQuad(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            ScreenPoint start,
            ScreenPoint end,
            float thickness,
            int color,
            float startProgress,
            float endProgress
    ) {
        float deltaX = end.x() - start.x();
        float deltaY = end.y() - start.y();
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0.0001F) {
            return;
        }
        float normalX = -deltaY / length * thickness * 0.5F;
        float normalY = deltaX / length * thickness * 0.5F;
        vertex(bufferBuilder, matrix, start.x() + normalX, start.y() + normalY, color, startProgress, 0.0F);
        vertex(bufferBuilder, matrix, start.x() - normalX, start.y() - normalY, color, startProgress, 1.0F);
        vertex(bufferBuilder, matrix, end.x() - normalX, end.y() - normalY, color, endProgress, 1.0F);
        vertex(bufferBuilder, matrix, end.x() + normalX, end.y() + normalY, color, endProgress, 0.0F);
    }

    private static List<ScreenPoint> buildCurvedPath(ScreenPoint start, ScreenPoint end, float curvature) {
        float deltaX = end.x() - start.x();
        float deltaY = end.y() - start.y();
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0.0001F) {
            return List.of(start, end);
        }
        float normalX = -deltaY / length;
        float normalY = deltaX / length;
        ScreenPoint control = new ScreenPoint(
                (start.x() + end.x()) * 0.5F + normalX * curvature,
                (start.y() + end.y()) * 0.5F + normalY * curvature
        );
        List<ScreenPoint> points = new ArrayList<>(PATH_SEGMENTS + 1);
        for (int segment = 0; segment <= PATH_SEGMENTS; segment++) {
            float progress = (float) segment / PATH_SEGMENTS;
            float inverse = 1.0F - progress;
            points.add(new ScreenPoint(
                    inverse * inverse * start.x() + 2.0F * inverse * progress * control.x() + progress * progress * end.x(),
                    inverse * inverse * start.y() + 2.0F * inverse * progress * control.y() + progress * progress * end.y()
            ));
        }
        return points;
    }

    private static ScreenPath trimPath(
            ScreenPoint start,
            ScreenPoint end,
            float startInset,
            float endInset
    ) {
        float deltaX = end.x() - start.x();
        float deltaY = end.y() - start.y();
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= startInset + endInset + 1.0F) {
            return new ScreenPath(start, end);
        }
        float directionX = deltaX / length;
        float directionY = deltaY / length;
        return new ScreenPath(
                new ScreenPoint(start.x() + directionX * startInset, start.y() + directionY * startInset),
                new ScreenPoint(end.x() - directionX * endInset, end.y() - directionY * endInset)
        );
    }

    private static ScreenPoint interpolatePath(List<ScreenPoint> path, float progress) {
        if (progress <= 0.0F) {
            return path.get(0);
        }
        if (progress >= 1.0F) {
            return path.get(path.size() - 1);
        }
        float scaledProgress = progress * (path.size() - 1);
        int index = Mth.clamp((int) scaledProgress, 0, path.size() - 2);
        float localProgress = scaledProgress - index;
        ScreenPoint start = path.get(index);
        ScreenPoint end = path.get(index + 1);
        return new ScreenPoint(
                Mth.lerp(localProgress, start.x(), end.x()),
                Mth.lerp(localProgress, start.y(), end.y())
        );
    }

    private static Particle orbitParticle(
            ScreenPoint center,
            float radius,
            float angleDegrees,
            float size,
            int color,
            float alpha
    ) {
        float angle = (float) Math.toRadians(angleDegrees);
        return new Particle(
                center.x() + Mth.cos(angle) * radius,
                center.y() + Mth.sin(angle) * radius,
                size,
                ColorUtility.withAlpha(color, alpha),
                angle + Mth.HALF_PI
        );
    }

    private static float calculateLength(List<ScreenPoint> points) {
        float length = 0.0F;
        for (int index = 0; index < points.size() - 1; index++) {
            length += distance(points.get(index), points.get(index + 1));
        }
        return length;
    }

    private static float distance(ScreenPoint start, ScreenPoint end) {
        float deltaX = end.x() - start.x();
        float deltaY = end.y() - start.y();
        return Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private static float getWaterRippleProgress(long currentTime, int rippleIndex) {
        return fract(currentTime / WATER_RIPPLE_DURATION + rippleIndex / 3.0F);
    }

    private static float getFireFrontProgress(long currentTime, int frontIndex) {
        return fract(currentTime / 1850.0F + frontIndex / 3.0F);
    }

    private static float getThicknessScale(ElementType element) {
        return switch (element) {
            case METAL -> 0.18F;
            case WOOD -> 0.28F;
            case WATER -> 0.34F;
            case FIRE -> 0.38F;
            case EARTH -> 0.32F;
            case YIN_YANG, THUNDER, CHAOS -> 0.2F;
        };
    }

    private static float getParticleSpeed(ElementType element) {
        return switch (element) {
            case METAL -> 0.00115F;
            case WOOD -> 0.00045F;
            case WATER -> 0.00062F;
            case FIRE -> 0.00092F;
            case EARTH -> 0.00036F;
            case YIN_YANG, THUNDER, CHAOS -> 0.0005F;
        };
    }

    private static float getOrbitSpeed(ElementType element) {
        return switch (element) {
            case METAL -> 0.07F;
            case WOOD -> 0.024F;
            case WATER -> 0.04F;
            case FIRE -> 0.055F;
            case EARTH -> 0.015F;
            case YIN_YANG, THUNDER, CHAOS -> 0.025F;
        };
    }

    private static float getFlowStyle(ElementType element) {
        return switch (element) {
            case METAL -> 0.0F;
            case WOOD -> 1.0F;
            case WATER -> 2.0F;
            case FIRE -> 3.0F;
            case EARTH -> 4.0F;
            case YIN_YANG -> 5.0F;
            case THUNDER, CHAOS -> 0.0F;
        };
    }

    private static float getFlowSeed(ElementType sourceElement, ElementType targetElement) {
        return sourceElement.ordinal() * 0.173F + targetElement.ordinal() * 0.317F;
    }

    private static float fract(float value) {
        return value - Mth.floor(value);
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            int color,
            float textureU,
            float textureV
    ) {
        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        bufferBuilder.vertex(matrix, x, y, 0.0F)
                .color(red, green, blue, alpha)
                .uv(textureU, textureV)
                .endVertex();
    }

    private static void vertex(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, int color) {
        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    private record ScreenPath(ScreenPoint start, ScreenPoint end) {
    }

    private record Particle(float x, float y, float size, int color, float rotation) {
    }
}
