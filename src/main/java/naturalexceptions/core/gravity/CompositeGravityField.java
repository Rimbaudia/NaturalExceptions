package naturalexceptions.core.gravity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Superposition of multiple gravitational fields.
 * Net acceleration is the vector sum of all constituent fields.
 * Local frame ownership uses an explicit hysteresis / contact-anchoring rule
 * so dominant source identity does not thrash near ambiguous transition regions.
 */
public final class CompositeGravityField implements GravityField {

    private final List<GravityField> fields;

    public CompositeGravityField(List<GravityField> fields) {
        Objects.requireNonNull(fields, "fields");
        for (GravityField field : fields) {
            Objects.requireNonNull(field, "field element");
        }
        this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    }

    public static CompositeGravityField of(GravityField... fields) {
        List<GravityField> list = new ArrayList<>(fields.length);
        for (GravityField f : fields) {
            list.add(Objects.requireNonNull(f, "field"));
        }
        return new CompositeGravityField(list);
    }

    public List<GravityField> fields() {
        return fields;
    }

    @Override
    public GravitySample sampleAt(double x, double y, double z, GravityQueryContext context) {
        Vector3d totalAcceleration = Vector3d.ZERO;
        GravitySourceId dominantSourceId = GravitySourceId.DEFAULT_MINECRAFT;
        double maxContribution = -1.0;
        double currentSourceContribution = -1.0;

        for (GravityField field : fields) {
            GravitySample sample = field.sampleAt(x, y, z, context);
            totalAcceleration = totalAcceleration.add(sample.acceleration());
            double mag = sample.magnitude();

            if (context != null && context.currentSourceId() != null
                && context.currentSourceId()
                    .equals(sample.sourceId())) {
                currentSourceContribution = mag;
            }

            if (mag > maxContribution) {
                maxContribution = mag;
                dominantSourceId = sample.sourceId();
            }
        }

        // Hysteresis rule: If current frame source is still active, require competing source
        // to exceed it by a hysteresis factor (1.25x ungrounded, 1.5x grounded) to prevent thrashing
        if (context != null && context.currentSourceId() != null && currentSourceContribution >= 0.0) {
            double hysteresisFactor = context.isGrounded() ? 1.5 : 1.25;
            if (currentSourceContribution * hysteresisFactor >= maxContribution) {
                dominantSourceId = context.currentSourceId();
            }
        }

        return new GravitySample(totalAcceleration, dominantSourceId);
    }
}
