package naturalexceptions.core.gravity;

/**
 * Context passed into gravity queries for stable local-frame selection and hysteresis.
 */
public final class GravityQueryContext {

    private final GravitySourceId currentSourceId;
    private final boolean isGrounded;
    private final long tick;

    public GravityQueryContext(GravitySourceId currentSourceId, boolean isGrounded, long tick) {
        this.currentSourceId = currentSourceId;
        this.isGrounded = isGrounded;
        this.tick = tick;
    }

    public static GravityQueryContext empty() {
        return new GravityQueryContext(null, false, 0L);
    }

    public static GravityQueryContext of(GravitySourceId currentSourceId, boolean isGrounded) {
        return new GravityQueryContext(currentSourceId, isGrounded, 0L);
    }

    public GravitySourceId currentSourceId() {
        return currentSourceId;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public long tick() {
        return tick;
    }
}
