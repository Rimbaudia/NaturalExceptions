package naturalexceptions.minecraft.biology;

import java.util.Locale;

/** Plain words for the Body panel: how you are, what is wrong and where, without the numbers behind it. */
public final class BodyWords {

    private BodyWords() {}

    /** Overall state: Healthy, Hurt, Badly hurt or Critical. */
    public static String status(BodyStatusSnapshot body) {
        float condition = body.condition();
        if (condition < 0.35F) return "Critical";
        if (condition < 0.6F) return "Badly hurt";
        if (condition < 0.85F || !body.injuries()
            .isEmpty()) return "Hurt";
        return "Healthy";
    }

    /** "left arm", "head". */
    public static String place(String regionId) {
        return BodyStatusSnapshot.label(regionId)
            .toLowerCase(Locale.ROOT);
    }

    /** e.g. "Deep cut on the left arm, bleeding", "Broken right leg, splinted", "Burn on the torso, dressed". */
    public static String injury(BodyStatusSnapshot body, BodyStatusSnapshot.Injury injury) {
        String place = place(injury.regionId());
        boolean treated = injury.treatment() > 0.0F;
        StringBuilder text = new StringBuilder();
        switch (injury.kind()) {
            case CUT:
                String cut = injury.severity() >= 0.5F ? "Deep cut" : injury.severity() < 0.2F ? "Graze" : "Cut";
                text.append(injury.count() > 1 ? cut + "s" : cut)
                    .append(" on the ")
                    .append(place);
                if (treated) text.append(", dressed");
                else if (injury.bleeding() > 0.05F) text.append(", bleeding");
                break;
            case BURN:
                boolean several = injury.count() > 1;
                text.append(injury.severity() >= 0.5F ? several ? "Bad burns" : "Bad burn" : several ? "Burns" : "Burn")
                    .append(" on the ")
                    .append(place);
                if (treated) text.append(", dressed");
                break;
            case STRUCTURAL:
            default:
                BodyStatusSnapshot.Region region = body.region(injury.regionId());
                boolean broken = injury.severity() >= 0.5F || (region != null && region.fractured());
                text.append(broken ? "Broken " : "Bruised ")
                    .append(place);
                if (treated) text.append(", splinted");
                break;
        }
        return text.toString();
    }

    /** How strongly an exposure is affecting you: trace, mild, moderate, severe. */
    public static String level(BodyStatusSnapshot.Exposure exposure) {
        double penalty = exposure.conditionPenalty();
        if (penalty < 0.01) return "trace";
        if (penalty < 0.05) return "mild";
        if (penalty < 0.15) return "moderate";
        return "severe";
    }

    /** e.g. "Smoke in your lungs", "Swallowed lead", "Lead on your skin". */
    public static String exposure(BodyStatusSnapshot.Exposure exposure, String materialName) {
        String material = exposure.material();
        if (material.endsWith(":combustion_smoke")) return "Smoke in your lungs";
        String name = materialName.toLowerCase(Locale.ROOT);
        switch (exposure.route()) {
            case INHALATION:
                return "Breathed-in " + name;
            case INGESTION:
                return "Swallowed " + name;
            case CONTACT:
                return capitalise(name) + " on your skin";
            case WOUND:
            default:
                return capitalise(name) + " in a wound";
        }
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
