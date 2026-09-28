package naturalexceptions.minecraft.event;

import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.StarRainImpact;

/** Small sensory notices derived from persisted event history; it owns no event or impact state. */
public final class StarRainPresentation {
    private StarRainPresentation() { }

    public static String eventStartMessage(EventRecord event) {
        if (event == null || !EventRecord.PROTECTED_STAR_RAIN.equals(event.type())) return null;
        return "The sky brightens with a Star Rain. Impacts may scar the ground and leave dangerously hot matter.";
    }

    public static String impactMessage(StarRainImpact impact) {
        return impact == null ? null : "\u00a7bA star has struck nearby, leaving a smoking crater. Its heart still glows.";
    }
}
