package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntityCastellan;

/**
 * The Castellan: the master machine of a dead city, still keeping court. Tall and gaunt, in tarnished regalia: a
 * narrow helm under a crown, a cage of ribbed armour with its Stormglass heart burning through the gaps, a tabard
 * and a tattered cloak, long arms ending in heavy gauntlets, and three fragments of rock circling its waist. Built at
 * player scale and drawn 1.3 times larger, on a 128 x 64 texture.
 */
@SideOnly(Side.CLIENT)
public final class ModelCastellan extends ModelBase {

    /** Texture offsets {u, v} of each part; the generator paints the same boxes. */
    static final int[] HEAD = { 0, 0 }, CROWN = { 24, 0 }, SPIKE = { 52, 0 }, NECK = { 56, 0 };
    static final int[] TABARD = { 64, 0 }, CLOAK = { 80, 0 }, FRAGMENT = { 106, 0 };
    static final int[] TORSO = { 0, 14 }, CORE = { 32, 14 }, WAIST = { 48, 14 }, GAUNTLET = { 64, 23 };
    static final int[] LEG = { 0, 30 }, GREAVE = { 12, 30 }, PAULDRON = { 28, 30 }, ARM = { 52, 30 };

    final ModelRenderer head;
    final ModelRenderer torso;
    final ModelRenderer core;
    final ModelRenderer waist;
    final ModelRenderer tabard;
    final ModelRenderer cloak;
    final ModelRenderer rightLeg;
    final ModelRenderer leftLeg;
    final ModelRenderer rightPauldron;
    final ModelRenderer leftPauldron;
    final ModelRenderer rightArm;
    final ModelRenderer leftArm;
    final ModelRenderer rightGauntlet;
    final ModelRenderer leftGauntlet;
    final ModelRenderer[] fragments = new ModelRenderer[3];

    public ModelCastellan() {
        textureWidth = 128;
        textureHeight = 64;

        head = part(HEAD, 0.0F, -3.0F, 0.0F);
        head.addBox(-3.0F, -8.0F, -3.0F, 6, 8, 6);
        ModelRenderer crown = part(CROWN, 0.0F, 0.0F, 0.0F);
        crown.addBox(-3.5F, -9.0F, -3.5F, 7, 2, 7);
        head.addChild(crown);
        for (float[] at : new float[][] { { -3.5F, -3.5F }, { 2.5F, -3.5F }, { -3.5F, 2.5F }, { 2.5F, 2.5F } }) {
            ModelRenderer spike = part(SPIKE, 0.0F, 0.0F, 0.0F);
            spike.addBox(at[0], -11.0F, at[1], 1, 2, 1);
            head.addChild(spike);
        }
        ModelRenderer high = part(SPIKE, 0.0F, 0.0F, 0.0F);
        high.addBox(-0.5F, -12.0F, -3.5F, 1, 3, 1);
        head.addChild(high);

        torso = part(TORSO, 0.0F, 0.0F, 0.0F);
        torso.addBox(-5.0F, 0.0F, -3.0F, 10, 10, 6);
        ModelRenderer neck = part(NECK, 0.0F, 0.0F, 0.0F);
        neck.addBox(-1.0F, -3.0F, -1.0F, 2, 3, 2);
        torso.addChild(neck);
        core = part(CORE, 0.0F, 0.0F, 0.0F);
        core.addBox(-2.0F, 3.0F, -2.0F, 4, 4, 4);
        waist = part(WAIST, 0.0F, 10.0F, 0.0F);
        waist.addBox(-2.0F, 0.0F, -2.0F, 4, 3, 4);
        tabard = part(TABARD, 0.0F, 10.0F, -2.0F);
        tabard.addBox(-3.5F, 0.0F, -1.0F, 7, 10, 1);
        cloak = part(CLOAK, 0.0F, -0.5F, 3.0F);
        cloak.addBox(-6.0F, 0.0F, 0.0F, 12, 22, 1);

        rightLeg = leg(-2.5F, false);
        leftLeg = leg(2.5F, true);

        rightPauldron = part(PAULDRON, -6.5F, -1.0F, 0.0F);
        rightPauldron.addBox(-2.5F, -1.5F, -3.5F, 5, 3, 7);
        leftPauldron = part(PAULDRON, 6.5F, -1.0F, 0.0F);
        leftPauldron.mirror = true;
        leftPauldron.addBox(-2.5F, -1.5F, -3.5F, 5, 3, 7);

        rightArm = part(ARM, -7.0F, 1.0F, 0.0F);
        rightArm.addBox(-1.5F, 0.0F, -1.5F, 3, 9, 3);
        rightGauntlet = part(GAUNTLET, 0.0F, 9.0F, 0.0F);
        rightGauntlet.addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4);
        rightArm.addChild(rightGauntlet);
        leftArm = part(ARM, 7.0F, 1.0F, 0.0F);
        leftArm.mirror = true;
        leftArm.addBox(-1.5F, 0.0F, -1.5F, 3, 9, 3);
        leftGauntlet = part(GAUNTLET, 0.0F, 9.0F, 0.0F);
        leftGauntlet.mirror = true;
        leftGauntlet.addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4);
        leftArm.addChild(leftGauntlet);

        for (int i = 0; i < fragments.length; i++) {
            fragments[i] = part(FRAGMENT, 0.0F, 10.0F, 0.0F);
            fragments[i].addBox(-1.0F, -1.0F, -1.0F, 2, 2, 2);
        }
    }

    private ModelRenderer leg(float x, boolean mirror) {
        ModelRenderer leg = part(LEG, x, 13.0F, 0.0F);
        leg.mirror = mirror;
        leg.addBox(-1.5F, 0.0F, -1.5F, 3, 11, 3);
        ModelRenderer greave = part(GREAVE, 0.0F, 0.0F, 0.0F);
        greave.mirror = mirror;
        greave.addBox(-2.0F, 5.0F, -2.0F, 4, 5, 4);
        leg.addChild(greave);
        return leg;
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
        core.render(scale); // first, so the ribs are drawn over it and it shows only through their gaps
        head.render(scale);
        torso.render(scale);
        waist.render(scale);
        tabard.render(scale);
        cloak.render(scale);
        rightLeg.render(scale);
        leftLeg.render(scale);
        rightPauldron.render(scale);
        leftPauldron.render(scale);
        rightArm.render(scale);
        leftArm.render(scale);
        for (ModelRenderer fragment : fragments) fragment.render(scale);
    }

    private float windup;
    private boolean unbound;

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbAmount, float partialTicks) {
        boolean charging = entity instanceof EntityCastellan
            && (((EntityCastellan) entity).chargingSlam() || ((EntityCastellan) entity).chargingFan());
        windup += ((charging ? 1.0F : 0.0F) - windup) * 0.25F;
        unbound = entity instanceof EntityCastellan && ((EntityCastellan) entity).unbound();
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbAmount, float age, float yaw, float pitch, float scale,
        Entity entity) {
        head.rotateAngleY = yaw / (180.0F / (float) Math.PI);
        head.rotateAngleX = pitch / (180.0F / (float) Math.PI);

        float stride = MathHelper.cos(limbSwing * 0.5F) * 0.9F * limbAmount;
        rightLeg.rotateAngleX = stride;
        leftLeg.rotateAngleX = -stride;
        // The cloak and tabard trail as it walks, and stir always.
        float stir = MathHelper.sin(age * 0.08F) * 0.04F;
        cloak.rotateAngleX = 0.08F + limbAmount * 0.5F + stir;
        tabard.rotateAngleX = -Math.abs(stride) * 0.3F - stir;

        // Long arms swing when walking, and rise overhead while a slam gathers.
        float swing = MathHelper.cos(limbSwing * 0.5F) * 0.6F * limbAmount;
        float raised = -2.8F * windup;
        rightArm.rotateAngleX = swing * (1.0F - windup) + raised;
        leftArm.rotateAngleX = -swing * (1.0F - windup) + raised;
        rightArm.rotateAngleZ = 0.08F + 0.25F * windup;
        leftArm.rotateAngleZ = -0.08F - 0.25F * windup;
        if (onGround > 0.0F) rightArm.rotateAngleX = -1.8F + MathHelper.sin(onGround * (float) Math.PI) * 1.6F;

        float spin = age * (unbound ? 0.16F : 0.07F);
        for (int i = 0; i < fragments.length; i++) {
            float a = spin + i * (float) (Math.PI * 2.0 / fragments.length);
            fragments[i].rotationPointX = MathHelper.cos(a) * 9.5F;
            fragments[i].rotationPointZ = MathHelper.sin(a) * 9.5F;
            fragments[i].rotationPointY = 9.0F + MathHelper.sin(age * 0.2F + i) * 1.2F;
            fragments[i].rotateAngleY = a * 2.0F;
            fragments[i].rotateAngleX = a;
        }
    }
}
