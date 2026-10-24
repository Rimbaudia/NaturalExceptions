package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntityFoundryColossus;

/**
 * The Foundry Colossus: a furnace on short legs. A grille across its belly glows with its fire; two iron plates cover
 * its furnace-heart and swing open when it is doused; a smokestack stands at its shoulder; its right arm ends in a
 * hammer, its left in a crucible of molten metal. Built at player scale and drawn 1.5 times larger, on a 128 x 64
 * texture.
 */
@SideOnly(Side.CLIENT)
public final class ModelFoundryColossus extends ModelBase {

    /** Texture offsets {u, v} of each part; the generator paints the same boxes. */
    static final int[] BODY = { 0, 0 }, PLATE = { 56, 0 }, CORE = { 74, 0 }, HEAD = { 88, 0 }, STACK = { 112, 0 };
    static final int[] ARM = { 56, 11 }, CRUCIBLE = { 80, 11 }, HAMMER = { 28, 30 }, LEG = { 0, 40 };

    final ModelRenderer body, leftPlate, rightPlate, core, head, rightArm, leftArm, rightLeg, leftLeg;

    public ModelFoundryColossus() {
        textureWidth = 128;
        textureHeight = 64;

        body = part(BODY, 0.0F, -2.0F, 0.0F);
        body.addBox(-8.0F, 0.0F, -6.0F, 16, 18, 12);
        ModelRenderer stack = part(STACK, 0.0F, 0.0F, 0.0F);
        stack.addBox(3.0F, -7.0F, 2.0F, 3, 7, 3);
        body.addChild(stack);
        core = part(CORE, 0.0F, 0.0F, 0.0F);
        core.addBox(-3.0F, 6.0F, -6.5F, 6, 6, 1);
        body.addChild(core);
        leftPlate = part(PLATE, -8.0F, 2.0F, -6.0F);
        leftPlate.addBox(0.0F, 2.0F, -1.0F, 8, 10, 1);
        body.addChild(leftPlate);
        rightPlate = part(PLATE, 8.0F, 2.0F, -6.0F);
        rightPlate.mirror = true;
        rightPlate.addBox(-8.0F, 2.0F, -1.0F, 8, 10, 1);
        body.addChild(rightPlate);

        head = part(HEAD, 0.0F, -2.0F, -2.0F);
        head.addBox(-3.0F, -5.0F, -3.0F, 6, 5, 6);

        rightArm = part(ARM, -11.0F, 1.0F, 0.0F);
        rightArm.addBox(-3.0F, -2.0F, -3.0F, 6, 12, 6);
        ModelRenderer hammer = part(HAMMER, 0.0F, 0.0F, 0.0F);
        hammer.addBox(-4.5F, 9.0F, -5.5F, 9, 7, 11);
        rightArm.addChild(hammer);
        leftArm = part(ARM, 11.0F, 1.0F, 0.0F);
        leftArm.mirror = true;
        leftArm.addBox(-3.0F, -2.0F, -3.0F, 6, 12, 6);
        ModelRenderer crucible = part(CRUCIBLE, 0.0F, 0.0F, 0.0F);
        crucible.addBox(-4.0F, 9.0F, -4.0F, 8, 7, 8);
        leftArm.addChild(crucible);

        rightLeg = part(LEG, -5.0F, 16.0F, 0.0F);
        rightLeg.addBox(-3.5F, 0.0F, -3.5F, 7, 8, 7);
        leftLeg = part(LEG, 5.0F, 16.0F, 0.0F);
        leftLeg.mirror = true;
        leftLeg.addBox(-3.5F, 0.0F, -3.5F, 7, 8, 7);
    }

    private ModelRenderer part(int[] uv, float x, float y, float z) {
        ModelRenderer part = new ModelRenderer(this, uv[0], uv[1]);
        part.setRotationPoint(x, y, z);
        return part;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch,
        float scale) {
        setRotationAngles(limbSwing, limbAmount, age, yaw, pitch, scale, entity);
        body.render(scale);
        head.render(scale);
        rightArm.render(scale);
        leftArm.render(scale);
        rightLeg.render(scale);
        leftLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbAmount, float age, float yaw, float pitch, float scale,
        Entity entity) {
        int state = entity instanceof EntityFoundryColossus ? ((EntityFoundryColossus) entity).state()
            : EntityFoundryColossus.AWAKE;
        int winding = entity instanceof EntityFoundryColossus ? ((EntityFoundryColossus) entity).winding()
            : EntityFoundryColossus.NONE;
        boolean asleep = state == EntityFoundryColossus.ASLEEP, doused = state == EntityFoundryColossus.QUENCHED;

        head.rotateAngleY = asleep ? 0.0F : yaw / (180.0F / (float) Math.PI);
        head.rotateAngleX = asleep || doused ? 0.45F : pitch / (180.0F / (float) Math.PI);

        float stride = MathHelper.cos(limbSwing * 0.45F) * 0.7F * limbAmount;
        rightLeg.rotateAngleX = stride;
        leftLeg.rotateAngleX = -stride;
        float sway = MathHelper.cos(limbSwing * 0.45F) * 0.4F * limbAmount;
        rightArm.rotateAngleX = -sway;
        leftArm.rotateAngleX = sway;
        rightArm.rotateAngleZ = 0.1F;
        leftArm.rotateAngleZ = -0.1F;

        // Its furnace-heart's plates swing open when it is doused.
        leftPlate.rotateAngleY = doused ? 1.3F : 0.0F;
        rightPlate.rotateAngleY = doused ? -1.3F : 0.0F;

        if (asleep || doused) {
            rightArm.rotateAngleX = leftArm.rotateAngleX = 0.15F;
            return;
        }
        float breathe = MathHelper.sin(age * 0.3F) * 0.05F;
        switch (winding) {
            case EntityFoundryColossus.SLAM: // the hammer raised high
                rightArm.rotateAngleX = -2.7F + breathe;
                rightArm.rotateAngleZ = 0.25F;
                break;
            case EntityFoundryColossus.POUR: // the crucible lifted out before it and tipping
                leftArm.rotateAngleX = -1.3F;
                leftArm.rotateAngleZ = -0.6F + breathe;
                break;
            case EntityFoundryColossus.THROW: // reaching back to lob
                leftArm.rotateAngleX = -2.4F + breathe;
                break;
            default:
                break;
        }
    }
}
