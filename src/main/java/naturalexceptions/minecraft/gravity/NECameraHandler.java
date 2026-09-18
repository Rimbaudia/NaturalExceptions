package naturalexceptions.minecraft.gravity;

import java.nio.FloatBuffer;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.gravity.GravityCameraFrame;
import naturalexceptions.core.gravity.LocalEntityFrame;
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

        LocalEntityFrame frame = service.getOrUpdateLocalFrame(viewEntity);
        Vector3d targetUp = frame.localUp();
        CAMERA_FRAME.update(targetUp);

        float mcPitch = viewEntity.prevRotationPitch
            + (viewEntity.rotationPitch - viewEntity.prevRotationPitch) * partialTicks;
        float mathPitch = -mcPitch;
        double pitchRad = Math.toRadians(mathPitch);

        Vector3d tangentForward = frame.tangentForward();
        Vector3d look = tangentForward.multiply(Math.cos(pitchRad))
            .add(targetUp.multiply(Math.sin(pitchRad)))
            .normalize();

        Vector3d right = tangentForward.cross(targetUp)
            .normalize();
        Vector3d screenUp = right.cross(look)
            .normalize();

        // Column-major FloatBuffer for OpenGL Modelview matrix:
        // View matrix transforms world vectors to eye vectors via M * v:
        // Row 0 = right -> eye X
        // Row 1 = screenUp -> eye Y
        // Row 2 = -look -> eye Z (into screen is -Z)
        // Row 3 = (0, 0, 0, 1)
        MATRIX_BUFFER.clear();
        // Column 0
        MATRIX_BUFFER.put((float) right.x);
        MATRIX_BUFFER.put((float) screenUp.x);
        MATRIX_BUFFER.put((float) -look.x);
        MATRIX_BUFFER.put(0.0F);

        // Column 1
        MATRIX_BUFFER.put((float) right.y);
        MATRIX_BUFFER.put((float) screenUp.y);
        MATRIX_BUFFER.put((float) -look.y);
        MATRIX_BUFFER.put(0.0F);

        // Column 2
        MATRIX_BUFFER.put((float) right.z);
        MATRIX_BUFFER.put((float) screenUp.z);
        MATRIX_BUFFER.put((float) -look.z);
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
