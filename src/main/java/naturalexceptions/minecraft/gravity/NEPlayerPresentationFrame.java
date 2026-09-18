package naturalexceptions.minecraft.gravity;

import java.nio.FloatBuffer;
import java.util.Objects;

import naturalexceptions.core.gravity.LocalEntityFrame;
import naturalexceptions.core.gravity.Vector3d;

/**
 * Single canonical presentation frame for player rendering under arbitrary gravity.
 * Consumed identically by:
 * <ul>
 * <li>Camera orientation and view matrix ({@link NECameraHandler})</li>
 * <li>Third-person entity 3D model orientation ({@link NEEntityRenderHandler})</li>
 * <li>Eye point and support point anchoring ({@code MixinEntityRenderer})</li>
 * <li>First-person hand/item viewpoint</li>
 * </ul>
 */
public final class NEPlayerPresentationFrame {

    private final LocalEntityFrame physicalFrame;
    private final Vector3d renderedUp;
    private final Vector3d renderedForward;
    private final Vector3d renderedRight;
    private final Vector3d eyePoint;
    private final Vector3d supportPoint;
    private final double localPitchDegrees;

    public NEPlayerPresentationFrame(LocalEntityFrame physicalFrame, Vector3d renderedUp, Vector3d renderedForward,
        Vector3d eyePoint, Vector3d supportPoint, double localPitchDegrees) {
        this.physicalFrame = Objects.requireNonNull(physicalFrame, "physicalFrame");
        this.renderedUp = Objects.requireNonNull(renderedUp, "renderedUp")
            .normalize();
        this.renderedForward = Objects.requireNonNull(renderedForward, "renderedForward")
            .normalize();
        this.renderedRight = this.renderedUp.cross(this.renderedForward)
            .normalize();
        this.eyePoint = Objects.requireNonNull(eyePoint, "eyePoint");
        this.supportPoint = Objects.requireNonNull(supportPoint, "supportPoint");
        this.localPitchDegrees = localPitchDegrees;
    }

    public LocalEntityFrame physicalFrame() {
        return physicalFrame;
    }

    public Vector3d renderedUp() {
        return renderedUp;
    }

    public Vector3d renderedForward() {
        return renderedForward;
    }

    public Vector3d renderedRight() {
        return renderedRight;
    }

    public Vector3d eyePoint() {
        return eyePoint;
    }

    public Vector3d supportPoint() {
        return supportPoint;
    }

    public double localPitchDegrees() {
        return localPitchDegrees;
    }

    /**
     * Computes the 3D look vector including local pitch tilt.
     * localPitchDegrees: positive tilts up toward renderedUp, negative tilts down toward nadir.
     */
    public Vector3d computeLookVector() {
        double pitchRad = Math.toRadians(localPitchDegrees);
        return renderedForward.multiply(Math.cos(pitchRad))
            .add(renderedUp.multiply(Math.sin(pitchRad)))
            .normalize();
    }

    /**
     * Returns the 16-element column-major OpenGL Modelview camera view matrix.
     * Maps world coordinates to camera eye coordinates (Row 0 = right, Row 1 = screenUp, Row 2 = -look).
     */
    public float[] cameraMatrix() {
        Vector3d look = computeLookVector();
        // Camera right is renderedForward x renderedUp = -renderedRight (maps to +X on screen)
        Vector3d camRight = renderedForward.cross(renderedUp)
            .normalize();
        // Screen up is camRight x look (maps to +Y on screen)
        Vector3d screenUp = camRight.cross(look)
            .normalize();

        float[] m = new float[16];
        // Column 0
        m[0] = (float) camRight.x;
        m[1] = (float) screenUp.x;
        m[2] = (float) -look.x;
        m[3] = 0.0F;

        // Column 1
        m[4] = (float) camRight.y;
        m[5] = (float) screenUp.y;
        m[6] = (float) -look.y;
        m[7] = 0.0F;

        // Column 2
        m[8] = (float) camRight.z;
        m[9] = (float) screenUp.z;
        m[10] = (float) -look.z;
        m[11] = 0.0F;

        // Column 3
        m[12] = 0.0F;
        m[13] = 0.0F;
        m[14] = 0.0F;
        m[15] = 1.0F;

        return m;
    }

    /**
     * Returns the 16-element column-major OpenGL Modelview model matrix for third-person player rendering.
     * Maps model space (head along +Y, face towards -Z, right arm towards -X) to local gravity frame.
     * Determinant is strictly +1.0.
     */
    public float[] modelMatrix() {
        // Column 0: maps model +X to -renderedRight
        // Column 1: maps model +Y to renderedUp
        // Column 2: maps model +Z to -renderedForward
        float[] m = new float[16];
        m[0] = (float) -renderedRight.x;
        m[1] = (float) -renderedRight.y;
        m[2] = (float) -renderedRight.z;
        m[3] = 0.0F;

        m[4] = (float) renderedUp.x;
        m[5] = (float) renderedUp.y;
        m[6] = (float) renderedUp.z;
        m[7] = 0.0F;

        m[8] = (float) -renderedForward.x;
        m[9] = (float) -renderedForward.y;
        m[10] = (float) -renderedForward.z;
        m[11] = 0.0F;

        m[12] = 0.0F;
        m[13] = 0.0F;
        m[14] = 0.0F;
        m[15] = 1.0F;

        return m;
    }

    public void uploadCameraMatrix(FloatBuffer buffer) {
        buffer.clear();
        buffer.put(cameraMatrix());
        buffer.flip();
    }

    public void uploadModelMatrix(FloatBuffer buffer) {
        buffer.clear();
        buffer.put(modelMatrix());
        buffer.flip();
    }
}
