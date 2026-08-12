package net.seraphina.ninecun_lingtai.client.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;

public final class BodyDefenseSpellModel extends PlayerModel<AbstractClientPlayer> {
    private static final float LAYER_DEFORMATION = 0.18F;

    public BodyDefenseSpellModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    public static LayerDefinition createLayerDefinition(boolean slim) {
        return LayerDefinition.create(
                PlayerModel.createMesh(new CubeDeformation(LAYER_DEFORMATION), slim),
                64,
                64
        );
    }

    public void copyFrom(PlayerModel<AbstractClientPlayer> playerModel) {
        playerModel.copyPropertiesTo(this);
        this.head.visible = playerModel.head.visible;
        this.body.visible = playerModel.body.visible;
        this.rightArm.visible = playerModel.rightArm.visible;
        this.leftArm.visible = playerModel.leftArm.visible;
        this.rightLeg.visible = playerModel.rightLeg.visible;
        this.leftLeg.visible = playerModel.leftLeg.visible;

        this.hat.visible = false;
        this.jacket.visible = false;
        this.rightSleeve.visible = false;
        this.leftSleeve.visible = false;
        this.rightPants.visible = false;
        this.leftPants.visible = false;
    }
}
