package naturalexceptions.minecraft.gravity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-side camera orientation handler for arbitrary local gravity.
 *
 * <p>
 * Decouples visual orientation from global world Y. Applies the exact orthonormal
 * camera orientation matrix derived from the canonical {@link NEPlayerPresentationFrame},
 * completely eliminating gimbal lock, 180-degree upside-down flips, and visual snaps.
 */
@SideOnly(Side.CLIENT)
public final class NECameraHandler {

    private static final FloatBuffer MATRIX_BUFFER = ByteBuffer.allocateDirect(64)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer();

    private NECameraHandler() {}

    /**
     * Applies the complete 3D orthonormal camera orientation to the OpenGL MODELVIEW matrix.
     * Invoked from {@code MixinEntityRenderer.orientCamera()}.
     */
    public static void applyOrientation(EntityLivingBase viewEntity, float partialTicks) {
        NEGravityService service = NEGravityService.global();
        if (!service.isNonstandardGravityActive()) {
            return;
        }

        if (viewEntity == null) {
            return;
        }

        NEPlayerPresentationFrame frame = NEPlayerPresentationService.instance()
            .getPresentationFrame(viewEntity, partialTicks);
        frame.uploadCameraMatrix(MATRIX_BUFFER);
        GL11.glMultMatrix(MATRIX_BUFFER);
    }
}
