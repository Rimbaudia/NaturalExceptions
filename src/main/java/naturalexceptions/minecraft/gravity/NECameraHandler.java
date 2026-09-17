package naturalexceptions.minecraft.gravity;

import java.nio.FloatBuffer;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.gravity.GravityCameraFrame;
import naturalexceptions.core.gravity.GravitySample;
import naturalexceptions.core.gravity.Vector3d;

/**
 * Client-side camera orientation handler for arbitrary local gravity.
 *
 * <p>
 * Decouples visual orientation from global world Y. Applies an exact orthonormal
 * camera orientation matrix derived from the smoothed {@code GravityCameraFrame},
 * completely eliminating gimbal lock, 180-degree upside-down flips, and visual snaps.
 */
@SideOnly(Side.CLIENT)
public final class NECameraHandler {

    private static final GravityCameraFrame CAMERA_FRAME = new GravityCameraFrame(Vector3d.UP, 0.15);
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    private NECameraHandler() {}

    public static GravityCameraFrame cameraFrame() {
        return CAMERA_FRAME;
    }

    /**
     * Applies the complete 3D orthonormal camera orientation to the OpenGL MODELVIEW matrix.
     * Invoked from {@code MixinEntityRenderer.orientCamera()}.
     */
    public static void applyOrientation(EntityLivingBase viewEntity, float partialTicks) {
        NEGravityService service = NEGravityService.global();
        if (!service.isNonstandardGravityActive()) {
            CAMERA_FRAME.update(Vector3d.UP);
            return;
        }

        if (viewEntity == null) {
            return;
        }

        GravitySample sample = service.sampleFor(viewEntity);
        Vector3d targetUp = sample.localUp();
        CAMERA_FRAME.update(targetUp);

        float yaw = viewEntity.prevRotationYaw + (viewEntity.rotationYaw - viewEntity.prevRotationYaw) * partialTicks;
        float mcPitch = viewEntity.prevRotationPitch
            + (viewEntity.rotationPitch - viewEntity.prevRotationPitch) * partialTicks;
        float mathPitch = -mcPitch;

        GravityCameraFrame.CameraBasis basis = CAMERA_FRAME.computeCameraBasis(yaw, mathPitch);

        // Column-major FloatBuffer for OpenGL Modelview matrix:
        // View matrix transforms world vectors to eye vectors via M * v:
        // Row 0 = basis.right -> eye X
        // Row 1 = basis.up -> eye Y
        // Row 2 = -basis.look -> eye Z (into screen is -Z)
        // Row 3 = (0, 0, 0, 1)
        MATRIX_BUFFER.clear();
        // Column 0
        MATRIX_BUFFER.put((float) basis.right.x);
        MATRIX_BUFFER.put((float) basis.up.x);
        MATRIX_BUFFER.put((float) -basis.look.x);
        MATRIX_BUFFER.put(0.0F);

        // Column 1
        MATRIX_BUFFER.put((float) basis.right.y);
        MATRIX_BUFFER.put((float) basis.up.y);
        MATRIX_BUFFER.put((float) -basis.look.y);
        MATRIX_BUFFER.put(0.0F);

        // Column 2
        MATRIX_BUFFER.put((float) basis.right.z);
        MATRIX_BUFFER.put((float) basis.up.z);
        MATRIX_BUFFER.put((float) -basis.look.z);
        MATRIX_BUFFER.put(0.0F);

        // Column 3
        MATRIX_BUFFER.put(0.0F);
        MATRIX_BUFFER.put(0.0F);
        MATRIX_BUFFER.put(0.0F);
        MATRIX_BUFFER.put(1.0F);
        MATRIX_BUFFER.flip();

        GL11.glMultMatrix(MATRIX_BUFFER);
    }
}
