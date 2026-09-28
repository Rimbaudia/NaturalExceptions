package naturalexceptions.minecraft.event;

import java.util.Locale;

import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.calendar.StrangeSeason;

/**
 * Restrained, player-facing wording for the calendar and world events. It owns no state; it describes what a
 * person on the ground could notice, never internal identifiers.
 */
public final class P8Presentation {

    private P8Presentation() {}

    public static String seasonName(NaturalCalendar.Season season) {
        String name = season.name()
            .toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** e.g. "Day 5 of Spring, year 1". */
    public static String dateLine(NaturalCalendar.Position position) {
        long dayOfSeason = position.dayOfYear() % NaturalCalendar.DAYS_PER_SEASON + 1;
        return "Day " + dayOfSeason + " of " + seasonName(position.season()) + ", year " + (position.year() + 1);
    }

    /** One line about how far into the season it is, in ordinary words. */
    public static String seasonProgressLine(NaturalCalendar.Position position) {
        double progress = position.seasonProgress();
        String part = progress < 0.25 ? "Early" : progress < 0.75 ? "Mid" : "Late";
        return part + " " + seasonName(position.season()).toLowerCase(Locale.ROOT) + ".";
    }

    public static String seasonBeginsMessage(NaturalCalendar.Season season) {
        switch (season) {
            case SPRING:
                return "The days lengthen. Spring has begun.";
            case SUMMER:
                return "The air grows heavy and warm. Summer has begun.";
            case AUTUMN:
                return "A chill edges the evenings. Autumn has begun.";
            default:
                return "Frost settles in the low places. Winter has begun.";
        }
    }

    public static String strangeSeasonLine(StrangeSeason season) {
        if (season == null || season == StrangeSeason.NONE) return null;
        return "Ash drifts on the wind, and the season feels wrong.";
    }

    public static String bloodNightBeginsMessage() {
        return "\u00a7cThe moon rises the colour of rust. Still water stirs, and things move in the dark.";
    }

    public static String bloodNightEndsMessage() {
        return "\u00a77The red moon has passed. The land is quieter, though not quite as it was.";
    }

    public static String bloodNightAlmanacLine() {
        return "A Blood Night is upon the land: stay near light, and watch the water.";
    }

    /** Eight-point compass direction from (dx, dz) in Minecraft coordinates (north is -z). */
    public static String direction(double dx, double dz) {
        if (dx == 0.0 && dz == 0.0) return "here";
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        if (degrees < 0) degrees += 360.0;
        String[] names = { "north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west" };
        return names[(int) Math.round(degrees / 45.0) % 8];
    }

    /** Coarse distance in words; a person judging a falling light cannot know it to the block. */
    public static String distance(double blocks) {
        if (blocks < 24) return "very close";
        if (blocks < 64) return "close by";
        if (blocks < 160) return "a short walk away";
        if (blocks < 400) return "some distance away";
        return "far away";
    }

    /** Heard/seen from afar as the impact happens. */
    public static String distantImpactMessage(double dx, double dz) {
        double blocks = Math.sqrt(dx * dx + dz * dz);
        return "\u00a7bA streak of light falls to the " + direction(dx, dz)
            + ", "
            + distance(blocks)
            + ". Something may be worth finding there.";
    }

    /** Almanac line pointing at the nearest unrecovered site. */
    public static String nearestSiteLine(double dx, double dz) {
        double blocks = Math.sqrt(dx * dx + dz * dz);
        return "A fallen star still lies to the " + direction(dx, dz) + ", " + distance(blocks) + ".";
    }
}
