package naturalexceptions.core.fabrication;

/**
 * Physical damage and degradation state of an individual component.
 *
 * <p>
 * Simulates four distinct physical failure modes:
 * <ul>
 * <li><b>Wear (0.0 - 1.0):</b> Edge dulling / surface abrasion that reduces cutting and penetration sharpness.</li>
 * <li><b>Permanent Deformation (0.0 - 1.0):</b> Plastic bending or warping under high stress.</li>
 * <li><b>Cracking (0.0 - 1.0):</b> Fatigue or impact microcracks reducing failure margin.</li>
 * <li><b>Fracture (boolean):</b> Structural severance; fractured components stop providing physical capability.</li>
 * </ul>
 */
public final class ComponentDamage {

    public static final ComponentDamage PRISTINE = new ComponentDamage(0.0, 0.0, 0.0, false);

    private double wear;
    private double deformation;
    private double cracking;
    private boolean fractured;

    public ComponentDamage() {
        this(0.0, 0.0, 0.0, false);
    }

    public ComponentDamage(double wear, double deformation, double cracking, boolean fractured) {
        this.wear = Math.max(0.0, Math.min(1.0, wear));
        this.deformation = Math.max(0.0, Math.min(1.0, deformation));
        this.cracking = Math.max(0.0, Math.min(1.0, cracking));
        this.fractured = fractured || this.cracking >= 1.0;
    }

    public double wear() {
        return wear;
    }

    public double deformation() {
        return deformation;
    }

    public double cracking() {
        return cracking;
    }

    public boolean isFractured() {
        return fractured;
    }

    public boolean isDamaged() {
        return wear > 0.0 || deformation > 0.0 || cracking > 0.0 || fractured;
    }

    public void addWear(double amount) {
        if (amount <= 0.0 || fractured) return;
        this.wear = Math.min(1.0, this.wear + amount);
    }

    public void addDeformation(double amount) {
        if (amount <= 0.0 || fractured) return;
        this.deformation = Math.min(1.0, this.deformation + amount);
    }

    public void addCracking(double amount) {
        if (amount <= 0.0 || fractured) return;
        this.cracking = Math.min(1.0, this.cracking + amount);
        if (this.cracking >= 1.0) {
            this.fractured = true;
        }
    }

    public void fracture() {
        this.cracking = 1.0;
        this.fractured = true;
    }

    public void repairWear(double amount) {
        if (amount <= 0.0 || fractured) return;
        this.wear = Math.max(0.0, this.wear - amount);
    }

    public void repairDeformation(double amount) {
        if (amount <= 0.0 || fractured) return;
        this.deformation = Math.max(0.0, this.deformation - amount);
    }

    /**
     * Continuous capability degradation factor in [0.0, 1.0].
     * Returns 0.0 if fractured.
     */
    public double effectiveMultiplier() {
        if (fractured) {
            return 0.0;
        }
        double factor = 1.0 - (wear * 0.40) - (deformation * 0.35) - (cracking * 0.25);
        return Math.max(0.05, factor);
    }
}
