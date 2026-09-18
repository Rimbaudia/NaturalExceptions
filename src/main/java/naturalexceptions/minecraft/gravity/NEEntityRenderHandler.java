package naturalexceptions.minecraft.gravity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.LocalBodyStance;
import naturalexceptions.core.gravity.Vector3d;

/**
 * Client-side entity rendering handler for arbitrary local gravity.
 *
 * <p>
 * Positions and orients living entity 3D models relative to local gravity, ensuring
 * that the entity's feet are planted on the support surface (floor, wall, ceiling)
 * and the body extends outward along {@code renderedUp}, consuming the exact same
 * canonical presentation frame as the camera.
 */
@SideOnly(Side.CLIENT)
public final class NEEntityRenderHandler {

    private static final FloatBuffer MODEL_BUFFER = ByteBuffer.allocateDirect(64)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer();

    private static float lastPartialTicks = 1.0F;

    private NEEntityRenderHandler() {}

    public static void recordPartialTicks(float partialTicks) {
        lastPartialTicks = partialTicks;
    }

    public static float getLastPartialTicks() {
        return lastPartialTicks;
    }

    /**
     * Determines whether custom orientation handling should override standard vanilla rotateCorpse.
     */
    public static boolean shouldApplyCustomOrientation(EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }
        NEGravityService service = NEGravityService.global();
        if (!service.isNonstandardGravityActive()) {
            return false;
        }
        GravitySample sample = service.sampleFor(entity);
        Vector3d localUp = sample.localUp();
        return Vector3d.UP.dot(localUp) <= 0.99999;
    }

    /**
     * Applies gravity-relative stance translation and orientation to the OpenGL MODELVIEW matrix.
     * Invoked from {@code MixinRendererLivingEntity.rotateCorpse()} when nonstandard gravity is active.
     */
    public static void applyEntityOrientation(EntityLivingBase entity, float bodyYaw, float partialTicks) {
        recordPartialTicks(partialTicks);
        if (entity == null) {
            return;
        }

        NEGravityService service = NEGravityService.global();
        if (!service.isNonstandardGravityActive()) {
            applyStandardOrientation(entity, bodyYaw, partialTicks);
            return;
        }

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, partialTicks);
        Vector3d localUp = frame.renderedUp();
        Vector3d worldUp = Vector3d.UP;
        double dot = worldUp.dot(localUp);

        if (dot > 0.99999) {
            applyStandardOrientation(entity, bodyYaw, partialTicks);
            return;
        }

        // 1. Calculate stance geometry and translate origin to support surface (feet)
        double hw = entity.width * 0.5;
        double hh = entity.height * 0.5;
        double yOffset = entity instanceof net.minecraft.entity.player.EntityPlayer ? (double) entity.yOffset
            : (entity.yOffset > 0.0F ? (double) entity.yOffset : 0.0);
        double bottomY = entity.posY - yOffset;
        Vector3d center = new Vector3d(entity.posX, bottomY + hh, entity.posZ);
        LocalBodyStance stance = new LocalBodyStance(
            center,
            frame.renderedUp()
                .negate(),
            hw,
            hh);
        Vector3d offset = stance.supportSurfaceOffset();

        GL11.glTranslatef((float) offset.x, (float) offset.y, (float) offset.z);

        // 2. Apply canonical model orientation matrix derived from rendered presentation frame
        frame.uploadModelMatrix(MODEL_BUFFER);
        GL11.glMultMatrix(MODEL_BUFFER);

        // 3. Handle death animation if dying
        applyDeathRotation(entity, partialTicks);
    }

    /**
     * Computes local head yaw and pitch in degrees relative to the rendered body frame.
     *
     * <p>
     * Projects the canonical 3D look vector into the body's orthonormal basis
     * (renderedRight, renderedUp, renderedForward):
     * <ul>
     * <li>localHeadYaw: angle around renderedUp (degrees, positive = right, negative = left). Clamped to [-75,
     * 75].</li>
     * <li>localHeadPitch: angle relative to local horizon (degrees, positive = down, negative = up). Clamped to [-89,
     * 89].</li>
     * </ul>
     */
    public static float[] computeLocalHeadAngles(Vector3d lookVector, Vector3d renderedUp, Vector3d renderedForward,
        Vector3d renderedRight) {
        if (lookVector == null || renderedUp == null || renderedForward == null || renderedRight == null) {
            return new float[] { 0.0F, 0.0F };
        }

        double lR = lookVector.dot(renderedRight);
        double lU = lookVector.dot(renderedUp);
        double lF = lookVector.dot(renderedForward);

        double yawDeg = Math.toDegrees(Math.atan2(lR, lF));
        double pitchDeg = -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, lU))));

        float clampedYaw = (float) Math.max(-75.0, Math.min(75.0, yawDeg));
        float clampedPitch = (float) Math.max(-89.0, Math.min(89.0, pitchDeg));

        return new float[] { clampedYaw, clampedPitch };
    }

    /**
     * Applies gravity-relative head orientation and bow aim angles to a biped model.
     * Invoked from {@code MixinModelBiped.setRotationAngles()} at RETURN.
     */
    public static void applyModelAngles(net.minecraft.client.model.ModelBiped model, EntityLivingBase entity) {
        if (model == null || entity == null) {
            return;
        }
        if (!shouldApplyCustomOrientation(entity)) {
            return;
        }

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(entity, lastPartialTicks);

        float[] angles = computeLocalHeadAngles(
            frame.computeLookVector(),
            frame.renderedUp(),
            frame.renderedForward(),
            frame.renderedRight());

        float yawRad = (float) Math.toRadians(angles[0]);
        float pitchRad = (float) Math.toRadians(angles[1]);

        model.bipedHead.rotateAngleY = yawRad;
        model.bipedHead.rotateAngleX = pitchRad;
        model.bipedHead.rotateAngleZ = 0.0F;

        model.bipedHeadwear.rotateAngleY = yawRad;
        model.bipedHeadwear.rotateAngleX = pitchRad;
        model.bipedHeadwear.rotateAngleZ = 0.0F;

        if (model.bipedEars != null) {
            model.bipedEars.rotateAngleY = yawRad;
            model.bipedEars.rotateAngleX = pitchRad;
        }

        if (model.aimedBow) {
            model.bipedRightArm.rotateAngleY = -0.1F + yawRad;
            model.bipedLeftArm.rotateAngleY = 0.1F + yawRad + 0.4F;
            model.bipedRightArm.rotateAngleX = -((float) Math.PI / 2.0F) + pitchRad;
            model.bipedLeftArm.rotateAngleX = -((float) Math.PI / 2.0F) + pitchRad;
        }
    }

    private static void applyStandardOrientation(EntityLivingBase entity, float bodyYaw, float partialTicks) {
        GL11.glRotatef(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
        applyDeathRotation(entity, partialTicks);
    }

    private static void applyDeathRotation(EntityLivingBase entity, float partialTicks) {
        if (entity.deathTime > 0) {
            float f3 = ((float) entity.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f3 = (float) Math.sqrt(f3);
            if (f3 > 1.0F) {
                f3 = 1.0F;
            }
            GL11.glRotatef(f3 * 90.0F, 0.0F, 0.0F, 1.0F);
        }
    }
}
