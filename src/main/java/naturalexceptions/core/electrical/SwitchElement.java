package naturalexceptions.core.electrical;

/** Topology-controlling normally-open or closed switch. */
public final class SwitchElement {
    private final String stableId;
    private boolean closed;
    private Runnable changeListener;
    public SwitchElement(String stableId, boolean closed) {
        if (stableId == null || stableId.trim().isEmpty()) throw new IllegalArgumentException("Switch ID required");
        this.stableId = stableId; this.closed = closed;
    }
    public String stableId() { return stableId; }
    public boolean closed() { return closed; }
    public void setClosed(boolean value) {
        if (closed == value) return;
        closed = value;
        if (changeListener != null) changeListener.run();
    }
    void attach(Runnable listener) { changeListener = listener; }
}
