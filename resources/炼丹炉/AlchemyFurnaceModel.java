// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;


public class AlchemyFurnaceModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "alchemy_furnace"), "main");
	private final ModelPart root;
	private final ModelPart radial_0;
	private final ModelPart radial_45;
	private final ModelPart radial_90;
	private final ModelPart radial_135;
	private final ModelPart radial_180;
	private final ModelPart radial_225;
	private final ModelPart radial_270;
	private final ModelPart radial_315;
	private final ModelPart leg_0;
	private final ModelPart leg_120;
	private final ModelPart leg_240;
	private final ModelPart handle_left;
	private final ModelPart handle_right;
	private final ModelPart center_accents;

	public AlchemyFurnaceModel(ModelPart root) {
		this.root = root.getChild("root");
		this.radial_0 = root.getChild("radial_0");
		this.radial_45 = root.getChild("radial_45");
		this.radial_90 = root.getChild("radial_90");
		this.radial_135 = root.getChild("radial_135");
		this.radial_180 = root.getChild("radial_180");
		this.radial_225 = root.getChild("radial_225");
		this.radial_270 = root.getChild("radial_270");
		this.radial_315 = root.getChild("radial_315");
		this.leg_0 = root.getChild("leg_0");
		this.leg_120 = root.getChild("leg_120");
		this.leg_240 = root.getChild("leg_240");
		this.handle_left = root.getChild("handle_left");
		this.handle_right = root.getChild("handle_right");
		this.center_accents = root.getChild("center_accents");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition radial_0 = partdefinition.addOrReplaceChild("radial_0", CubeListBuilder.create().texOffs(0, 0).addBox(-6.1867F, -9.0F, -14.26F, 12.3734F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.5205F, -14.0F, -17.48F, 15.0409F, 5.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-8.8542F, -22.5F, -20.7F, 17.7084F, 10.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.9015F, -27.0F, -18.4F, 15.8031F, 6.0F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.5678F, -29.0F, -15.18F, 13.1355F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.4875F, -14.5F, -20.056F, 16.9749F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.2969F, -22.7F, -19.596F, 16.5939F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -31.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9573F, -36.0F, -13.7062F, 11.9146F, 6.0F, 3.2895F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -38.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-5.6091F, -34.2F, -12.8656F, 11.2182F, 1.2F, 1.0965F, new CubeDeformation(0.0F))
		.texOffs(212, 96).addBox(-1.8275F, -35.5F, -13.2676F, 3.655F, 3.5F, 0.2924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.7037F, -39.2F, -8.2656F, 7.4074F, 1.7F, 2.0664F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.3328F, -43.0F, -7.3702F, 6.6656F, 4.0F, 1.7909F, new CubeDeformation(0.0F))
		.texOffs(200, 96).addBox(-1.5498F, -42.6F, -7.5768F, 3.0996F, 2.8F, 0.2755F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.7634F, -44.0F, -10.824F, 9.5269F, 1.5F, 2.8864F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.0203F, -45.2F, -9.03F, 8.0407F, 1.4F, 2.8896F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.9731F, -46.2F, -6.5016F, 5.9461F, 1.2F, 2.3117F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.0155F, -47.2F, -4.1899F, 4.031F, 1.2F, 1.8782F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition radial_45 = partdefinition.addOrReplaceChild("radial_45", CubeListBuilder.create().texOffs(0, 0).addBox(-6.1867F, -9.0F, -14.26F, 12.3734F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.5205F, -14.0F, -17.48F, 15.0409F, 5.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-8.8542F, -22.5F, -20.7F, 17.7084F, 10.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.9015F, -27.0F, -18.4F, 15.8031F, 6.0F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.5678F, -29.0F, -15.18F, 13.1355F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.4875F, -14.5F, -20.056F, 16.9749F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.2969F, -22.7F, -19.596F, 16.5939F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -31.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9573F, -36.0F, -13.7062F, 11.9146F, 6.0F, 3.2895F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -38.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-5.6091F, -34.2F, -12.8656F, 11.2182F, 1.2F, 1.0965F, new CubeDeformation(0.0F))
		.texOffs(212, 96).addBox(-1.8275F, -35.5F, -13.2676F, 3.655F, 3.5F, 0.2924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.7037F, -39.2F, -8.2656F, 7.4074F, 1.7F, 2.0664F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.3328F, -43.0F, -7.3702F, 6.6656F, 4.0F, 1.7909F, new CubeDeformation(0.0F))
		.texOffs(200, 96).addBox(-1.5498F, -42.6F, -7.5768F, 3.0996F, 2.8F, 0.2755F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.7634F, -44.0F, -10.824F, 9.5269F, 1.5F, 2.8864F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.0203F, -45.2F, -9.03F, 8.0407F, 1.4F, 2.8896F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.9731F, -46.2F, -6.5016F, 5.9461F, 1.2F, 2.3117F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.0155F, -47.2F, -4.1899F, 4.031F, 1.2F, 1.8782F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

		PartDefinition radial_90 = partdefinition.addOrReplaceChild("radial_90", CubeListBuilder.create().texOffs(0, 0).addBox(-6.1867F, -9.0F, -14.26F, 12.3734F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.5205F, -14.0F, -17.48F, 15.0409F, 5.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-8.8542F, -22.5F, -20.7F, 17.7084F, 10.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.9015F, -27.0F, -18.4F, 15.8031F, 6.0F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.5678F, -29.0F, -15.18F, 13.1355F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.4875F, -14.5F, -20.056F, 16.9749F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.2969F, -22.7F, -19.596F, 16.5939F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -31.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9573F, -36.0F, -13.7062F, 11.9146F, 6.0F, 3.2895F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -38.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-5.6091F, -34.2F, -12.8656F, 11.2182F, 1.2F, 1.0965F, new CubeDeformation(0.0F))
		.texOffs(212, 96).addBox(-1.8275F, -35.5F, -13.2676F, 3.655F, 3.5F, 0.2924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.7037F, -39.2F, -8.2656F, 7.4074F, 1.7F, 2.0664F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.3328F, -43.0F, -7.3702F, 6.6656F, 4.0F, 1.7909F, new CubeDeformation(0.0F))
		.texOffs(200, 96).addBox(-1.5498F, -42.6F, -7.5768F, 3.0996F, 2.8F, 0.2755F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.7634F, -44.0F, -10.824F, 9.5269F, 1.5F, 2.8864F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.0203F, -45.2F, -9.03F, 8.0407F, 1.4F, 2.8896F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.9731F, -46.2F, -6.5016F, 5.9461F, 1.2F, 2.3117F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.0155F, -47.2F, -4.1899F, 4.031F, 1.2F, 1.8782F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition radial_135 = partdefinition.addOrReplaceChild("radial_135", CubeListBuilder.create().texOffs(0, 0).addBox(-6.1867F, -9.0F, -14.26F, 12.3734F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.5205F, -14.0F, -17.48F, 15.0409F, 5.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-8.8542F, -22.5F, -20.7F, 17.7084F, 10.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.9015F, -27.0F, -18.4F, 15.8031F, 6.0F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.5678F, -29.0F, -15.18F, 13.1355F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.4875F, -14.5F, -20.056F, 16.9749F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.2969F, -22.7F, -19.596F, 16.5939F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -31.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9573F, -36.0F, -13.7062F, 11.9146F, 6.0F, 3.2895F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -38.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-5.6091F, -34.2F, -12.8656F, 11.2182F, 1.2F, 1.0965F, new CubeDeformation(0.0F))
		.texOffs(212, 96).addBox(-1.8275F, -35.5F, -13.2676F, 3.655F, 3.5F, 0.2924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.7037F, -39.2F, -8.2656F, 7.4074F, 1.7F, 2.0664F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.3328F, -43.0F, -7.3702F, 6.6656F, 4.0F, 1.7909F, new CubeDeformation(0.0F))
		.texOffs(200, 96).addBox(-1.5498F, -42.6F, -7.5768F, 3.0996F, 2.8F, 0.2755F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.7634F, -44.0F, -10.824F, 9.5269F, 1.5F, 2.8864F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.0203F, -45.2F, -9.03F, 8.0407F, 1.4F, 2.8896F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.9731F, -46.2F, -6.5016F, 5.9461F, 1.2F, 2.3117F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.0155F, -47.2F, -4.1899F, 4.031F, 1.2F, 1.8782F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -2.3562F, 0.0F));

		PartDefinition radial_180 = partdefinition.addOrReplaceChild("radial_180", CubeListBuilder.create().texOffs(0, 0).addBox(-6.1867F, -9.0F, -14.26F, 12.3734F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.5205F, -14.0F, -17.48F, 15.0409F, 5.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-8.8542F, -22.5F, -20.7F, 17.7084F, 10.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.9015F, -27.0F, -18.4F, 15.8031F, 6.0F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.5678F, -29.0F, -15.18F, 13.1355F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.4875F, -14.5F, -20.056F, 16.9749F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.2969F, -22.7F, -19.596F, 16.5939F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -31.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9573F, -36.0F, -13.7062F, 11.9146F, 6.0F, 3.2895F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -38.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-5.6091F, -34.2F, -12.8656F, 11.2182F, 1.2F, 1.0965F, new CubeDeformation(0.0F))
		.texOffs(212, 96).addBox(-1.8275F, -35.5F, -13.2676F, 3.655F, 3.5F, 0.2924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.7037F, -39.2F, -8.2656F, 7.4074F, 1.7F, 2.0664F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.3328F, -43.0F, -7.3702F, 6.6656F, 4.0F, 1.7909F, new CubeDeformation(0.0F))
		.texOffs(200, 96).addBox(-1.5498F, -42.6F, -7.5768F, 3.0996F, 2.8F, 0.2755F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.7634F, -44.0F, -10.824F, 9.5269F, 1.5F, 2.8864F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.0203F, -45.2F, -9.03F, 8.0407F, 1.4F, 2.8896F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.9731F, -46.2F, -6.5016F, 5.9461F, 1.2F, 2.3117F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.0155F, -47.2F, -4.1899F, 4.031F, 1.2F, 1.8782F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -3.1416F, 0.0F));

		PartDefinition radial_225 = partdefinition.addOrReplaceChild("radial_225", CubeListBuilder.create().texOffs(0, 0).addBox(-6.1867F, -9.0F, -14.26F, 12.3734F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.5205F, -14.0F, -17.48F, 15.0409F, 5.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-8.8542F, -22.5F, -20.7F, 17.7084F, 10.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.9015F, -27.0F, -18.4F, 15.8031F, 6.0F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.5678F, -29.0F, -15.18F, 13.1355F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.4875F, -14.5F, -20.056F, 16.9749F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.2969F, -22.7F, -19.596F, 16.5939F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -31.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9573F, -36.0F, -13.7062F, 11.9146F, 6.0F, 3.2895F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -38.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-5.6091F, -34.2F, -12.8656F, 11.2182F, 1.2F, 1.0965F, new CubeDeformation(0.0F))
		.texOffs(212, 96).addBox(-1.8275F, -35.5F, -13.2676F, 3.655F, 3.5F, 0.2924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.7037F, -39.2F, -8.2656F, 7.4074F, 1.7F, 2.0664F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.3328F, -43.0F, -7.3702F, 6.6656F, 4.0F, 1.7909F, new CubeDeformation(0.0F))
		.texOffs(200, 96).addBox(-1.5498F, -42.6F, -7.5768F, 3.0996F, 2.8F, 0.2755F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.7634F, -44.0F, -10.824F, 9.5269F, 1.5F, 2.8864F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.0203F, -45.2F, -9.03F, 8.0407F, 1.4F, 2.8896F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.9731F, -46.2F, -6.5016F, 5.9461F, 1.2F, 2.3117F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.0155F, -47.2F, -4.1899F, 4.031F, 1.2F, 1.8782F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -3.927F, 0.0F));

		PartDefinition radial_270 = partdefinition.addOrReplaceChild("radial_270", CubeListBuilder.create().texOffs(0, 0).addBox(-6.1867F, -9.0F, -14.26F, 12.3734F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.5205F, -14.0F, -17.48F, 15.0409F, 5.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-8.8542F, -22.5F, -20.7F, 17.7084F, 10.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.9015F, -27.0F, -18.4F, 15.8031F, 6.0F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.5678F, -29.0F, -15.18F, 13.1355F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.4875F, -14.5F, -20.056F, 16.9749F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.2969F, -22.7F, -19.596F, 16.5939F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -31.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9573F, -36.0F, -13.7062F, 11.9146F, 6.0F, 3.2895F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -38.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-5.6091F, -34.2F, -12.8656F, 11.2182F, 1.2F, 1.0965F, new CubeDeformation(0.0F))
		.texOffs(212, 96).addBox(-1.8275F, -35.5F, -13.2676F, 3.655F, 3.5F, 0.2924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.7037F, -39.2F, -8.2656F, 7.4074F, 1.7F, 2.0664F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.3328F, -43.0F, -7.3702F, 6.6656F, 4.0F, 1.7909F, new CubeDeformation(0.0F))
		.texOffs(200, 96).addBox(-1.5498F, -42.6F, -7.5768F, 3.0996F, 2.8F, 0.2755F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.7634F, -44.0F, -10.824F, 9.5269F, 1.5F, 2.8864F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.0203F, -45.2F, -9.03F, 8.0407F, 1.4F, 2.8896F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.9731F, -46.2F, -6.5016F, 5.9461F, 1.2F, 2.3117F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.0155F, -47.2F, -4.1899F, 4.031F, 1.2F, 1.8782F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -4.7124F, 0.0F));

		PartDefinition radial_315 = partdefinition.addOrReplaceChild("radial_315", CubeListBuilder.create().texOffs(0, 0).addBox(-6.1867F, -9.0F, -14.26F, 12.3734F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.5205F, -14.0F, -17.48F, 15.0409F, 5.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-8.8542F, -22.5F, -20.7F, 17.7084F, 10.5F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.9015F, -27.0F, -18.4F, 15.8031F, 6.0F, 4.6F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.5678F, -29.0F, -15.18F, 13.1355F, 3.0F, 3.68F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.4875F, -14.5F, -20.056F, 16.9749F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-8.2969F, -22.7F, -19.596F, 16.5939F, 1.2F, 1.38F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -31.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.9573F, -36.0F, -13.7062F, 11.9146F, 6.0F, 3.2895F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.8219F, -38.0F, -10.965F, 9.6437F, 3.0F, 2.924F, new CubeDeformation(0.0F))
		.texOffs(96, 64).addBox(-5.6091F, -34.2F, -12.8656F, 11.2182F, 1.2F, 1.0965F, new CubeDeformation(0.0F))
		.texOffs(212, 96).addBox(-1.8275F, -35.5F, -13.2676F, 3.655F, 3.5F, 0.2924F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.7037F, -39.2F, -8.2656F, 7.4074F, 1.7F, 2.0664F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-3.3328F, -43.0F, -7.3702F, 6.6656F, 4.0F, 1.7909F, new CubeDeformation(0.0F))
		.texOffs(200, 96).addBox(-1.5498F, -42.6F, -7.5768F, 3.0996F, 2.8F, 0.2755F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.7634F, -44.0F, -10.824F, 9.5269F, 1.5F, 2.8864F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-4.0203F, -45.2F, -9.03F, 8.0407F, 1.4F, 2.8896F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.9731F, -46.2F, -6.5016F, 5.9461F, 1.2F, 2.3117F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.0155F, -47.2F, -4.1899F, 4.031F, 1.2F, 1.8782F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -5.4978F, 0.0F));

		PartDefinition leg_0 = partdefinition.addOrReplaceChild("leg_0", CubeListBuilder.create().texOffs(0, 96).addBox(-4.0F, -10.0F, -16.0F, 8.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 96).addBox(-3.5F, -6.0F, -15.5F, 7.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(0, 96).addBox(-3.0F, -2.5F, -16.0F, 6.0F, 2.5F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition leg_120 = partdefinition.addOrReplaceChild("leg_120", CubeListBuilder.create().texOffs(0, 96).addBox(-4.0F, -10.0F, -16.0F, 8.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 96).addBox(-3.5F, -6.0F, -15.5F, 7.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(0, 96).addBox(-3.0F, -2.5F, -16.0F, 6.0F, 2.5F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -2.0944F, 0.0F));

		PartDefinition leg_240 = partdefinition.addOrReplaceChild("leg_240", CubeListBuilder.create().texOffs(0, 96).addBox(-4.0F, -10.0F, -16.0F, 8.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 96).addBox(-3.5F, -6.0F, -15.5F, 7.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(0, 96).addBox(-3.0F, -2.5F, -16.0F, 6.0F, 2.5F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, -4.1888F, 0.0F));

		PartDefinition handle_left = partdefinition.addOrReplaceChild("handle_left", CubeListBuilder.create().texOffs(64, 64).addBox(1.0F, -1.0F, -3.5F, 3.5F, 12.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(1.0F, -8.0F, -3.5F, 3.5F, 7.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(1.0F, -11.0F, -3.5F, 3.5F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(-2.5F, 6.5F, -4.0F, 4.5F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(-5.5F, -7.0F, -4.0F, 7.5F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(1.0F, -8.0F, 1.5F, 3.5F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(20.0F, 1.0F, 0.0F));

		PartDefinition handle_right = partdefinition.addOrReplaceChild("handle_right", CubeListBuilder.create().texOffs(64, 64).addBox(-4.5F, -1.0F, -3.5F, 3.5F, 12.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(-4.5F, -8.0F, -3.5F, 3.5F, 7.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(-4.5F, -11.0F, -3.5F, 3.5F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(-2.0F, 6.5F, -4.0F, 4.5F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(-2.0F, -7.0F, -4.0F, 7.5F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(64, 64).addBox(-4.5F, -8.0F, 1.5F, 3.5F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-20.0F, 1.0F, 0.0F));

		PartDefinition center_accents = partdefinition.addOrReplaceChild("center_accents", CubeListBuilder.create().texOffs(176, 128).addBox(-5.0F, -22.5F, -20.85F, 10.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.8F, -46.8F, -2.8F, 5.6F, 0.9F, 5.6F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-2.2F, -47.5F, -2.2F, 4.4F, 0.9F, 4.4F, new CubeDeformation(0.0F))
		.texOffs(96, 0).addBox(-1.2F, -48.0F, -1.2F, 2.4F, 0.6F, 2.4F, new CubeDeformation(0.0F))
		.texOffs(0, 96).addBox(-10.4F, -7.2F, -10.4F, 20.8F, 1.4F, 20.8F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		radial_0.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		radial_45.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		radial_90.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		radial_135.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		radial_180.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		radial_225.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		radial_270.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		radial_315.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg_0.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg_120.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg_240.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		handle_left.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		handle_right.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		center_accents.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
