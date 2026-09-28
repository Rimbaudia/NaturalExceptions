package naturalexceptions.core.biology;

/** Lightweight process counters for developer acceptance and coarse performance review. */
public final class BiologicalDiagnostics {
    private static long interactions;
    private static long elapsedResolutions;
    private static long exposureDeposits;
    private static long activeResolutions;
    private static long sleepingResolutions;
    private BiologicalDiagnostics() { }
    public static synchronized void interaction() { interactions++; }
    public static synchronized void exposure() { exposureDeposits++; }
    public static synchronized void resolved(boolean active) {
        elapsedResolutions++;
        if (active) activeResolutions++; else sleepingResolutions++;
    }
    public static synchronized Snapshot snapshot() {
        return new Snapshot(interactions, elapsedResolutions, exposureDeposits, activeResolutions, sleepingResolutions);
    }
    public static final class Snapshot {
        public final long interactions, elapsedResolutions, exposureDeposits, activeResolutions, sleepingResolutions;
        private Snapshot(long interactions, long elapsedResolutions, long exposureDeposits,
            long activeResolutions, long sleepingResolutions) {
            this.interactions = interactions; this.elapsedResolutions = elapsedResolutions;
            this.exposureDeposits = exposureDeposits; this.activeResolutions = activeResolutions;
            this.sleepingResolutions = sleepingResolutions;
        }
    }
}
