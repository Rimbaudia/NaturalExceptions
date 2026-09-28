package naturalexceptions.minecraft.multiblock;

/**
 * Result of validating an in-world structure against a MultiblockDefinition.
 */
public final class MultiblockValidationResult {

    private static final MultiblockValidationResult VALID = new MultiblockValidationResult(true, 0, 0, 0, null, null);

    private final boolean valid;
    private final int errorX;
    private final int errorY;
    private final int errorZ;
    private final MultiblockPattern failedPattern;
    private final String failureReason;

    public MultiblockValidationResult(boolean valid, int errorX, int errorY, int errorZ,
        MultiblockPattern failedPattern, String failureReason) {
        this.valid = valid;
        this.errorX = errorX;
        this.errorY = errorY;
        this.errorZ = errorZ;
        this.failedPattern = failedPattern;
        this.failureReason = failureReason;
    }

    public static MultiblockValidationResult ok() {
        return VALID;
    }

    public static MultiblockValidationResult fail(int x, int y, int z, MultiblockPattern pattern, String reason) {
        return new MultiblockValidationResult(false, x, y, z, pattern, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isFailed() {
        return !valid;
    }

    public int errorX() {
        return errorX;
    }

    public int errorY() {
        return errorY;
    }

    public int errorZ() {
        return errorZ;
    }

    public MultiblockPattern failedPattern() {
        return failedPattern;
    }

    public String failureReason() {
        return failureReason;
    }
}
