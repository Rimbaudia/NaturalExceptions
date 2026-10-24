package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntityGathered;

/**
 * The Gathered: a towering knot of the dead. A great mass of shrouds with faces pressed in it, three heads leaning
 * together at its top, four long arms reaching, and a tail of shroud trailing down to just above the floor. Built at
 * player scale and drawn 1.3 times larger, on a 128 x 64 texture.
 */
@SideOnly(Side.CLIENT)
public final class ModelGathered extends ModelBase {

    /** Texture offsets {u, v} of each part; the generator paints the same boxes. */
    static final int[] BODY = { 0, 0 }, HEAD = { 48, 0 }, SMALL_HEAD = { 72, 0 }, ARM = { 92, 0 }, HAND = { 100, 0 };
    static final int[] TAIL1 = { 0, 30 }, TAIL2 = { 42, 30 }, TAIL3 = { 70, 30 };

    final ModelRenderer body, head, leftHead, rightHead, tail;
    final ModelRenderer[] arms = new ModelRenderer[4];
    /** Where each arm hangs from: x, y, z, and which side (-1 right, 1 left). */
    private static final float[][] SHOULDERS = { { -7.5F, -8.0F, 0.0F, -1 }, { 7.5F, -8.0F, 0.0F, 1 },
        { -6.5F, 1.0F, -2.0F, -1 }, { 6.5F, 1.0F, -2.0F, 1 } };

    public ModelGathered() {
        textureWidth = 128;
        textureHeight = 64;

        body = part(BODY, 0.0F, -4.0F, 0.0F);
        body.addBox(-7.0F, -10.0F, -5.0F, 14, 20, 10);
        head = part(HEAD, 0.0F, -10.0F, -1.0F);
        head.addBox(-3.0F, -7.0F, -3.0F, 6, 7, 6);
        body.addChild(head);
        leftHead = part(SMALL_HEAD, 5.5F, -8.0F, 0.0F);
        leftHead.addBox(-2.5F, -5.0F, -2.5F, 5, 5, 5);
        body.addChild(leftHead);
        rightHead = part(SMALL_HEAD, -5.5F, -8.0F, 0.0F);
        rightHead.mirror = true;
        rightHead.addBox(-2.5F, -5.0F, -2.5F, 5, 5, 5);
        body.addChild(rightHead);

        for (int i = 0; i < arms.length; i++) {
            float[] s = SHOULDERS[i];
            arms[i] = part(ARM, s[0], s[1], s[2]);
            arms[i].mirror = s[3] > 0;
            arms[i].addBox(-1.0F, 0.0F, -1.0F, 2, 16, 2);
            ModelRenderer hand = part(HAND, 0.0F, 0.0F, 0.0F);
            hand.mirror = s[3] > 0;
            hand.addBox(-1.5F, 15.0F, -1.5F, 3, 3, 3);
            arms[i].addChild(hand);
            body.addChild(arms[i]);
        }

        tail = part(TAIL1, 0.0F, 10.0F, 0.0F);
        tail.addBox(-6.0F, 0.0F, -4.5F, 12, 6, 9);
        ModelRenderer lower = part(TAIL2, 0.0F, 0.0F, 0.0F);
        lower.addBox(-4.0F, 6.0F, -3.0F, 8, 6, 6);
        tail.addChild(lower);
        ModelRenderer wisp = part(TAIL3, 0.0F, 0.0F, 0.0F);
        wisp.addBox(-2.0F, 12.0F, -1.5F, 4, 6, 3);
        tail.addChild(wisp);
        body.addChild(tail);
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
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbAmount, float age, float yaw, float pitch, float scale,
        Entity entity) {
        boolean kneeling = entity instanceof EntityGathered && ((EntityGathered) entity).kneeling();
        int winding = entity instanceof EntityGathered ? ((EntityGathered) entity).winding() : EntityGathered.NONE;

        body.rotateAngleX = kneeling ? 0.5F : 0.05F;
        head.rotateAngleY = kneeling ? 0.0F : yaw / (180.0F / (float) Math.PI) * 0.5F;
        head.rotateAngleX = kneeling ? 0.6F : MathHelper.sin(age * 0.04F) * 0.08F;
        leftHead.rotateAngleZ = 0.45F + MathHelper.sin(age * 0.05F + 1.0F) * 0.06F;
        rightHead.rotateAngleZ = -0.45F + MathHelper.sin(age * 0.05F + 2.0F) * 0.06F;
        leftHead.rotateAngleX = rightHead.rotateAngleX = kneeling ? 0.5F : 0.15F;
        tail.rotateAngleX = MathHelper.sin(age * 0.06F) * 0.08F;
        tail.rotateAngleZ = MathHelper.cos(age * 0.05F) * 0.06F;

        for (int i = 0; i < arms.length; i++) {
            float side = SHOULDERS[i][3];
            float sway = MathHelper.sin(age * 0.07F + i * 1.3F) * 0.12F;
            arms[i].rotateAngleX = kneeling ? 0.0F : -0.25F + sway;
            arms[i].rotateAngleZ = side * (0.15F + (i >= 2 ? 0.2F : 0.0F));
            if (winding == EntityGathered.GRASP) arms[i].rotateAngleX = -2.0F + sway * 0.5F;
            if (winding == EntityGathered.WAIL) arms[i].rotateAngleZ = side * 1.1F;
        }
        if (winding == EntityGathered.WAIL) {
            head.rotateAngleX = -0.6F;
            leftHead.rotateAngleX = rightHead.rotateAngleX = -0.4F;
        }
    }
}
