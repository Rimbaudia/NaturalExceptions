package naturalexceptions.core.biology;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.id.ResourceId;

/** Deterministic registry of data-defined body plans. */
public final class BodyPlanRegistry {
    private final Map<ResourceId, BodyPlan> plans = new LinkedHashMap<>();

    public static BodyPlanRegistry standard() {
        BodyPlanRegistry registry = new BodyPlanRegistry();
        BuiltinBodyPlans.registerInto(registry);
        return registry;
    }

    public synchronized void register(BodyPlan plan) {
        Objects.requireNonNull(plan, "plan");
        BodyPlan previous = plans.putIfAbsent(plan.id(), plan);
        if (previous != null && previous != plan) throw new IllegalArgumentException("Body plan ID is already registered: " + plan.id());
    }

    public synchronized BodyPlan get(ResourceId id) { return plans.get(id); }
    public synchronized BodyPlan require(ResourceId id) {
        BodyPlan plan = plans.get(id);
        if (plan == null) throw new IllegalArgumentException("Unknown body plan: " + id);
        return plan;
    }
    public synchronized Map<ResourceId, BodyPlan> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(plans));
    }
}
