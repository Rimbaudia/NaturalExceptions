package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Where things stand out on a citadel's island. Each landmark asks for a footprint within a band of bearings and
 * distances from the citadel's middle; the first spot there that the island will take (on it, level enough) and that
 * keeps clear of everything already placed is reserved for it. All in citadel offsets (x east, z south); bearings in
 * degrees, 0 east, 90 south, 180 west, 270 north.
 */
final class CitadelSites {

    /** How it judges ground: the height a rectangle can be levelled at, or -1 if it will not take it. */
    interface Ground {

        int at(int x0, int z0, int x1, int z1);
    }

    /** A reserved rectangle, inclusive, with the ground it was levelled at (-1 where that does not apply). */
    static final class Site {

        final String name;
        final int x0, z0, x1, z1, ground;

        Site(String name, int x0, int z0, int x1, int z1, int ground) {
            this.name = name;
            this.x0 = x0;
            this.z0 = z0;
            this.x1 = x1;
            this.z1 = z1;
            this.ground = ground;
        }

        boolean overlaps(int ax0, int az0, int ax1, int az1, int gap) {
            return ax0 <= x1 + gap && ax1 >= x0 - gap && az0 <= z1 + gap && az1 >= z0 - gap;
        }
    }

    /** The clear ground kept between any two sites. */
    static final int GAP = 6;

    private final Ground ground;
    final List<Site> sites = new ArrayList<>();

    CitadelSites(Ground ground) {
        this.ground = ground;
    }

    /** Reserves a rectangle outright (the town, the road): nothing else may come within {@link #GAP} of it. */
    Site reserve(String name, int x0, int z0, int x1, int z1) {
        Site site = new Site(name, Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1), -1);
        sites.add(site);
        return site;
    }

    /** Whether a rectangle keeps clear of every site. */
    boolean clear(int x0, int z0, int x1, int z1) {
        for (Site s : sites) if (s.overlaps(x0, z0, x1, z1, GAP)) return false;
        return true;
    }

    /**
     * A {@code w} by {@code d} site for {@code name}, its middle on a bearing between {@code from} and {@code to}
     * degrees and between {@code near} and {@code far} blocks out; tried from the far end first when {@code rim}, else
     * from the near end. Null if nowhere in the band will take it.
     */
    Site fit(String name, int w, int d, double from, double to, int near, int far, boolean rim) {
        int steps = Math.max(1, (int) Math.ceil((to - from) / 6.0));
        for (int k = 0; k <= far - near; k += 4) {
            int r = rim ? far - k : near + k;
            for (int i = 0; i <= steps; i++) {
                // Out from the middle of the band first, so a landmark sits where it was meant to when it can.
                int j = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2);
                double bearing = Math.toRadians((from + to) / 2 + j * (to - from) / steps);
                if (bearing < Math.toRadians(from) - 1e-9 || bearing > Math.toRadians(to) + 1e-9) continue;
                int mx = (int) Math.round(Math.cos(bearing) * r), mz = (int) Math.round(Math.sin(bearing) * r);
                int x0 = mx - w / 2, z0 = mz - d / 2, x1 = x0 + w - 1, z1 = z0 + d - 1;
                if (!clear(x0, z0, x1, z1)) continue;
                int g = ground.at(x0, z0, x1, z1);
                if (g < 0) continue;
                Site site = new Site(name, x0, z0, x1, z1, g);
                sites.add(site);
                return site;
            }
        }
        return null;
    }
}
