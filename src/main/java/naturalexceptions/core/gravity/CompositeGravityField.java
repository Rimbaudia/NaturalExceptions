package naturalexceptions.core.gravity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Superposition of multiple gravitational fields.
 * Net acceleration is the vector sum of all constituent fields.
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
    public GravitySample sampleAt(double x, double y, double z) {
        Vector3d totalAcceleration = Vector3d.ZERO;
        for (GravityField field : fields) {
            GravitySample sample = field.sampleAt(x, y, z);
            totalAcceleration = totalAcceleration.add(sample.acceleration());
        }
        return new GravitySample(totalAcceleration);
    }
}
