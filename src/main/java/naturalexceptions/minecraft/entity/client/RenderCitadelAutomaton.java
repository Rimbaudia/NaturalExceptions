package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.AutomatonPose;

/**
 * A Citadel Automaton: Skystone plates on a brass and iron frame, a helm with a visor slit and a brass crest, its
 * Stormglass heart showing through a grille in its chest; a halberd upright in its right hand, a tower shield on its
 * left arm, swung round before it when it guards. Dormant, its head is bowed and its arms hang, and its eye and
 * heart burn low.
 */
@SideOnly(Side.CLIENT)
public final class RenderCitadelAutomaton extends RenderBiped {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/citadel_automaton.png");
    private static final ResourceLocation GLOW = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/citadel_automaton_glow.png");

    public RenderCitadelAutomaton() {
        super(new Model(), 0.5F);
        setRenderPassModel(new Model());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        return TEXTURE;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }

    /** One more pass: the visor slit and the heart, full bright; low while it is dormant. */
    @Override
    protected int shouldRenderPass(EntityLiving entity, int pass, float partialTicks) {
        if (pass != 0) return -1;
        bindTexture(GLOW);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDepthMask(!entity.isInvisible());
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        boolean dormant = entity instanceof AutomatonPose && ((AutomatonPose) entity).dormant();
        boolean owned = entity instanceof AutomatonPose && ((AutomatonPose) entity).owned();
        float glow = dormant ? 0.25F : 0.8F + 0.2F * (float) Math.sin((entity.ticksExisted + partialTicks) * 0.15);
        // The city's machines burn Stormglass-blue; one repaired by a player, green.
        if (owned) GL11.glColor4f(glow * 0.45F, glow, glow * 0.4F, 1.0F);
        else GL11.glColor4f(glow, glow, glow, 1.0F);
        return 1;
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return entity instanceof EntityLiving ? shouldRenderPass((EntityLiving) entity, pass, partialTicks) : -1;
    }

    /** The machine on a 64 x 64 skin. */
    static final class Model extends ModelBiped {

        final ModelRenderer shieldSide, shieldFront, pole, blade, spike;

        Model() {
            super(0.0F, 0.0F, 64, 64);
            bipedHead = new ModelRenderer(this, 0, 0);
            bipedHead.addBox(-3.5F, -7.0F, -3.5F, 7, 7, 7);
            ModelRenderer crest = new ModelRenderer(this, 36, 8);
            crest.addBox(-0.5F, -9.0F, -3.0F, 1, 2, 6);
            bipedHead.addChild(crest);
            bipedHeadwear = new ModelRenderer(this, 0, 0); // no hood: the helm is all

            bipedBody = new ModelRenderer(this, 0, 16);
            bipedBody.addBox(-4.0F, 0.0F, -2.5F, 8, 11, 5);

            bipedRightArm = new ModelRenderer(this, 40, 16);
            bipedRightArm.addBox(-2.0F, -2.0F, -1.5F, 3, 11, 3);
            bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
            ModelRenderer rightPauldron = new ModelRenderer(this, 36, 0);
            rightPauldron.addBox(-3.0F, -3.0F, -2.5F, 5, 3, 5);
            bipedRightArm.addChild(rightPauldron);

            bipedLeftArm = new ModelRenderer(this, 40, 16);
            bipedLeftArm.mirror = true;
            bipedLeftArm.addBox(-1.0F, -2.0F, -1.5F, 3, 11, 3);
            bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
            ModelRenderer leftPauldron = new ModelRenderer(this, 36, 0);
            leftPauldron.mirror = true;
            leftPauldron.addBox(-2.0F, -3.0F, -2.5F, 5, 3, 5);
            bipedLeftArm.addChild(leftPauldron);

            bipedRightLeg = leg(-2.0F, false);
            bipedLeftLeg = leg(2.0F, true);

            // At its side, on the outside of its left arm.
            shieldSide = new ModelRenderer(this, 30, 40);
            shieldSide.addBox(2.0F, -3.0F, -4.5F, 1, 14, 9);
            bipedLeftArm.addChild(shieldSide);
            // Up, before its chest.
            shieldFront = new ModelRenderer(this, 0, 47);
            shieldFront.addBox(-4.0F, -1.0F, -5.0F, 8, 14, 1);
            bipedBody.addChild(shieldFront);
            // The halberd, upright through its right fist.
            pole = new ModelRenderer(this, 60, 0);
            pole.addBox(-1.0F, -14.0F, -0.5F, 1, 30, 1);
            bipedRightArm.addChild(pole);
            blade = new ModelRenderer(this, 52, 32);
            blade.addBox(-1.0F, -13.0F, -4.0F, 1, 5, 3);
            bipedRightArm.addChild(blade);
            spike = new ModelRenderer(this, 56, 40);
            spike.addBox(-1.0F, -17.0F, -0.5F, 1, 3, 1);
            bipedRightArm.addChild(spike);
        }

        private ModelRenderer leg(float x, boolean mirror) {
            ModelRenderer leg = new ModelRenderer(this, 0, 32);
            leg.mirror = mirror;
            leg.addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3);
            leg.setRotationPoint(x, 12.0F, 0.0F);
            ModelRenderer foot = new ModelRenderer(this, 12, 32);
            foot.mirror = mirror;
            foot.addBox(-2.0F, 10.0F, -3.0F, 4, 2, 5);
            leg.addChild(foot);
            return leg;
        }

        @Override
        public void setRotationAngles(float swing, float swingAmount, float age, float headYaw, float headPitch,
            float scale, Entity entity) {
            boolean dormant = entity instanceof AutomatonPose && ((AutomatonPose) entity).dormant();
            if (dormant) swingAmount = 0.0F;
            super.setRotationAngles(swing, swingAmount, age, headYaw, headPitch, scale, entity);
            boolean up = !dormant && entity instanceof AutomatonPose && ((AutomatonPose) entity).shieldUp();
            shieldFront.showModel = up;
            shieldSide.showModel = !up;
            if (dormant) {
                // Head bowed, arms slack at its sides.
                bipedHead.rotateAngleX = 0.6F;
                bipedHead.rotateAngleY = 0.0F;
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = 0.05F;
                bipedRightArm.rotateAngleZ = 0.05F;
                bipedLeftArm.rotateAngleZ = -0.05F;
                return;
            }
            if (up) {
                bipedLeftArm.rotateAngleX = -0.9F;
                bipedLeftArm.rotateAngleY = 0.5F;
            }
            // The halberd held upright, its head over its shoulder, unless it is swinging it.
            if (onGround <= 0.0F) bipedRightArm.rotateAngleX = Math.min(bipedRightArm.rotateAngleX, -0.15F);
        }
    }
}
