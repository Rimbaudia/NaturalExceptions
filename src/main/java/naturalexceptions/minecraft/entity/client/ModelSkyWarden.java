package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntitySkyWarden;

/**
 * The Sky Warden: a stone knight held together by wind and light. A broad Skystone torso with a Lumenite core in its
 * chest, a helm floating free above the shoulders, slab pauldrons, oversized gauntlets hovering off the upper arms,
 * short plated legs, and three fragments of rock circling the waist. Built at player scale (32 px tall) and drawn
 * 1.3 times larger. The texture layout (128 x 64) is shared with tools/textures/generate_playable_pass_sprites.py.
 */
@SideOnly(Side.CLIENT)
public final class ModelSkyWarden extends ModelBase {

    /** Texture offsets {u, v} of each part; the generator paints the same boxes. */
    static final int[] HEAD = { 0, 0 }, CREST = { 28, 0 }, CORE = { 44, 0 }, FRAGMENT = { 58, 0 };
    static final int[] TORSO = { 0, 14 }, SKIRT = { 0, 33 }, LEG = { 40, 14 };
    static final int[] PAULDRON = { 60, 14 }, UPPER_ARM = { 60, 26 }, GAUNTLET = { 88, 0 };

    final ModelRenderer head;
    final ModelRenderer torso;
    final ModelRenderer core;
    final ModelRenderer skirt;
    final ModelRenderer rightLeg;
    final ModelRenderer leftLeg;
    final ModelRenderer rightPauldron;
    final ModelRenderer leftPauldron;
    final ModelRenderer rightArm;
    final ModelRenderer leftArm;
    final ModelRenderer rightGauntlet;
    final ModelRenderer leftGauntlet;
    final ModelRenderer[] fragments = new ModelRenderer[3];

    public ModelSkyWarden() {
        textureWidth = 128;
        textureHeight = 64;

        head = part(HEAD, 0.0F, -2.0F, 0.0F);
        head.addBox(-3.5F, -7.0F, -3.5F, 7, 7, 7);
        ModelRenderer crest = part(CREST, 0.0F, 0.0F, 0.0F);
        crest.addBox(-0.5F, -9.0F, -3.0F, 1, 2, 6);
        head.addChild(crest);

        torso = part(TORSO, 0.0F, 0.0F, 0.0F);
        torso.addBox(-6.0F, 0.0F, -3.5F, 12, 12, 7);
        core = part(CORE, 0.0F, 0.0F, 0.0F);
        core.addBox(-2.0F, 3.0F, -4.5F, 4, 4, 2);
        skirt = part(SKIRT, 0.0F, 11.0F, 0.0F);
        skirt.addBox(-6.5F, 0.0F, -4.0F, 13, 3, 8);

        rightLeg = part(LEG, -3.2F, 13.0F, 0.0F);
        rightLeg.addBox(-2.5F, 0.0F, -2.5F, 5, 11, 5);
        leftLeg = part(LEG, 3.2F, 13.0F, 0.0F);
        leftLeg.mirror = true;
        leftLeg.addBox(-2.5F, 0.0F, -2.5F, 5, 11, 5);

        rightPauldron = part(PAULDRON, -7.5F, -1.0F, 0.0F);
        rightPauldron.addBox(-3.0F, -1.5F, -4.0F, 6, 3, 8);
        leftPauldron = part(PAULDRON, 7.5F, -1.0F, 0.0F);
        leftPauldron.mirror = true;
        leftPauldron.addBox(-3.0F, -1.5F, -4.0F, 6, 3, 8);

        rightArm = part(UPPER_ARM, -8.5F, 2.0F, 0.0F);
        rightArm.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4);
        rightGauntlet = part(GAUNTLET, 0.0F, 7.0F, 0.0F);
        rightGauntlet.addBox(-3.0F, 0.0F, -3.0F, 6, 9, 6);
        rightArm.addChild(rightGauntlet);
        leftArm = part(UPPER_ARM, 8.5F, 2.0F, 0.0F);
        leftArm.mirror = true;
        leftArm.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4);
        leftGauntlet = part(GAUNTLET, 0.0F, 7.0F, 0.0F);
        leftGauntlet.mirror = true;
        leftGauntlet.addBox(-3.0F, 0.0F, -3.0F, 6, 9, 6);
        leftArm.addChild(leftGauntlet);

        for (int i = 0; i < fragments.length; i++) {
            fragments[i] = part(FRAGMENT, 0.0F, 10.0F, 0.0F);
            fragments[i].addBox(-1.0F, -1.0F, -1.0F, 2, 2, 2);
        }
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
        head.render(scale);
        torso.render(scale);
        core.render(scale);
        skirt.render(scale);
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
        boolean charging = entity instanceof EntitySkyWarden
            && (((EntitySkyWarden) entity).chargingSlam() || ((EntitySkyWarden) entity).chargingFan());
        windup += ((charging ? 1.0F : 0.0F) - windup) * 0.25F;
        unbound = entity instanceof EntitySkyWarden && ((EntitySkyWarden) entity).unbound();
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbAmount, float age, float yaw, float pitch, float scale,
        Entity entity) {
        head.rotateAngleY = yaw / (180.0F / (float) Math.PI);
        head.rotateAngleX = pitch / (180.0F / (float) Math.PI);
        head.rotationPointY = -2.0F + MathHelper.sin(age * 0.1F) * 0.5F; // it floats

        float stride = MathHelper.cos(limbSwing * 0.5F) * 1.1F * limbAmount;
        rightLeg.rotateAngleX = stride;
        leftLeg.rotateAngleX = -stride;

        // Arms swing heavily when walking, and rise overhead while a slam gathers.
        float swing = MathHelper.cos(limbSwing * 0.5F) * 0.8F * limbAmount;
        float raised = -2.8F * windup;
        rightArm.rotateAngleX = swing * (1.0F - windup) + raised;
        leftArm.rotateAngleX = -swing * (1.0F - windup) + raised;
        rightArm.rotateAngleZ = 0.1F + 0.25F * windup;
        leftArm.rotateAngleZ = -0.1F - 0.25F * windup;
        if (onGround > 0.0F) rightArm.rotateAngleX = -1.8F + MathHelper.sin(onGround * (float) Math.PI) * 1.6F;
        float hover = MathHelper.sin(age * 0.15F) * 0.4F;
        rightGauntlet.rotationPointY = 7.0F + hover;
        leftGauntlet.rotationPointY = 7.0F - hover;
        rightPauldron.rotationPointY = -1.0F - Math.abs(hover) * 0.5F;
        leftPauldron.rotationPointY = rightPauldron.rotationPointY;

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
