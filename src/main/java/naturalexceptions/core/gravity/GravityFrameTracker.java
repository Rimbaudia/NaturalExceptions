package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Tracks the active gravity frame and hysteresis state for an entity over time.
 *
 * <p>
 * Stores the currently bound {@link GravitySourceId} and grounding state across ticks,
 * querying the gravity field with a {@link GravityQueryContext} to ensure continuous,
 * thrash-free frame transitions.
 */
public final class GravityFrameTracker {

    private GravitySourceId currentSourceId;
    private boolean isGrounded;
    private long lastSwitchTick;

    public GravityFrameTracker(GravitySourceId initialSourceId) {
        this.currentSourceId = initialSourceId != null ? initialSourceId : GravitySourceId.DEFAULT_MINECRAFT;
        this.isGrounded = false;
        this.lastSwitchTick = 0L;
    }

    public static GravityFrameTracker standard() {
        return new GravityFrameTracker(GravitySourceId.DEFAULT_MINECRAFT);
    }

    public GravitySourceId currentSourceId() {
        return currentSourceId;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public void setGrounded(boolean grounded) {
        this.isGrounded = grounded;
    }

    public long lastSwitchTick() {
        return lastSwitchTick;
    }

    /**
     * Evaluates the gravity field at (x, y, z) on the given tick, applying hysteresis
     * anchoring and updating the active source ID if the switching threshold is crossed.
     */
    public GravitySample sampleAndUpdate(GravityField field, double x, double y, double z, long tick) {
        Objects.requireNonNull(field, "field");
        GravityQueryContext context = new GravityQueryContext(currentSourceId, isGrounded, tick);
        GravitySample sample = field.sampleAt(x, y, z, context);

        if (!sample.sourceId()
            .equals(currentSourceId)) {
            currentSourceId = sample.sourceId();
            lastSwitchTick = tick;
        }

        return sample;
    }
}
