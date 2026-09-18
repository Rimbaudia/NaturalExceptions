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

    private NEEntityRenderHandler() {}

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
        Vector3d center = new Vector3d(entity.posX, entity.posY + hh, entity.posZ);
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
