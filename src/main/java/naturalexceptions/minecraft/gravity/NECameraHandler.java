package naturalexceptions.minecraft.gravity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

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
 * Decouples visual orientation from global world Y. Applies a smooth modelview
 * rotation aligning world UP with the player's smoothed {@code localUp}, eliminating
 * vanilla global-Y pitch gimbal lock and enabling smooth first-person viewing on
 * walls and ceilings.
 */
@SideOnly(Side.CLIENT)
public final class NECameraHandler {

    private static final GravityCameraFrame CAMERA_FRAME = new GravityCameraFrame(Vector3d.UP, 0.15);

    private NECameraHandler() {}

    public static GravityCameraFrame cameraFrame() {
        return CAMERA_FRAME;
    }

    /**
     * Applies the coordinate frame alignment rotation to the current OpenGL MODELVIEW matrix.
     * Invoked at the end of {@code EntityRenderer.orientCamera()}.
     */
    public static void applyAlignment(float partialTicks) {
        NEGravityService service = NEGravityService.global();
        if (!service.isNonstandardGravityActive()) {
            CAMERA_FRAME.update(Vector3d.UP);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase viewEntity = mc.renderViewEntity;
        if (viewEntity == null) {
            return;
        }

        GravitySample sample = service.sampleFor(viewEntity);
        Vector3d targetUp = sample.localUp();
        CAMERA_FRAME.update(targetUp);

        Vector3d currentUp = CAMERA_FRAME.currentUp();
        Vector3d worldUp = Vector3d.UP;

        double dot = currentUp.dot(worldUp);
        if (dot > 0.9999) {
            return; // Standard downward gravity, no extra rotation needed
        }

        if (dot < -0.9999) {
            // Exactly inverted (ceiling): rotate 180 degrees around Z
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
            return;
        }

        // Axis-angle rotation from currentUp to worldUp
        Vector3d rotAxis = currentUp.cross(worldUp)
            .normalize();
        double angleRad = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
        float angleDeg = (float) Math.toDegrees(angleRad);

        GL11.glRotatef(angleDeg, (float) rotAxis.x, (float) rotAxis.y, (float) rotAxis.z);
    }
}
