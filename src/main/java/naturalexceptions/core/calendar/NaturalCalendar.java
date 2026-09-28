package naturalexceptions.core.calendar;

/** Deterministic Natural Exceptions time derived from elapsed world ticks. */
public final class NaturalCalendar {

    public static final long TICKS_PER_DAY = 24_000L;
    public static final int DAYS_PER_SEASON = 24;
    public static final int DAYS_PER_YEAR = 96;
    private static final long TICKS_PER_SEASON = TICKS_PER_DAY * DAYS_PER_SEASON;
    private static final long TICKS_PER_YEAR = TICKS_PER_DAY * DAYS_PER_YEAR;

    private NaturalCalendar() { }

    /** Maps durable elapsed world time to an immutable calendar position. Negative time is invalid. */
    public static Position at(long totalWorldTicks) {
        if (totalWorldTicks < 0L) {
            throw new IllegalArgumentException("totalWorldTicks must be non-negative");
        }
        long absoluteDay = totalWorldTicks / TICKS_PER_DAY;
        long dayOfYear = absoluteDay % DAYS_PER_YEAR;
        long tickOfYear = totalWorldTicks % TICKS_PER_YEAR;
        long seasonIndex = dayOfYear / DAYS_PER_SEASON;
        double seasonProgress = (tickOfYear % TICKS_PER_SEASON) / (double) TICKS_PER_SEASON;
        return new Position(totalWorldTicks, absoluteDay, absoluteDay / DAYS_PER_YEAR, dayOfYear,
            Season.values()[(int) seasonIndex], seasonProgress);
    }

    public enum Season {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }

    public static final class Position {
        private final long totalWorldTicks;
        private final long absoluteDay;
        private final long year;
        private final long dayOfYear;
        private final Season season;
        private final double seasonProgress;

        private Position(long totalWorldTicks, long absoluteDay, long year, long dayOfYear, Season season,
            double seasonProgress) {
            this.totalWorldTicks = totalWorldTicks;
            this.absoluteDay = absoluteDay;
            this.year = year;
            this.dayOfYear = dayOfYear;
            this.season = season;
            this.seasonProgress = seasonProgress;
        }

        public long totalWorldTicks() { return totalWorldTicks; }
        public long absoluteDay() { return absoluteDay; }
        /** Zero-based year index. */
        public long year() { return year; }
        /** Zero-based day index within the 96-day year. */
        public long dayOfYear() { return dayOfYear; }
        public Season season() { return season; }
        /** Progress through the current season, including the partial current day. */
        public double seasonProgress() { return seasonProgress; }
    }
}
