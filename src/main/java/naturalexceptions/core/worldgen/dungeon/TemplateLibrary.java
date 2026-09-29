package naturalexceptions.core.worldgen.dungeon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The authored rooms and buildings, read once from {@code assets/naturalexceptions/dungeon/rooms/} and
 * {@code .../buildings/} (each listed in its index.txt).
 */
public final class TemplateLibrary {

    private static final String ROOMS = "/assets/naturalexceptions/dungeon/rooms/";
    private static final String BUILDINGS = "/assets/naturalexceptions/dungeon/buildings/";
    private static volatile List<RoomTemplate> all;
    private static volatile List<RoomTemplate> buildings;

    private TemplateLibrary() {}

    public static List<RoomTemplate> all() {
        List<RoomTemplate> loaded = all;
        if (loaded == null) {
            synchronized (TemplateLibrary.class) {
                if (all == null) all = Collections.unmodifiableList(load(ROOMS));
                loaded = all;
            }
        }
        return loaded;
    }

    /** The citadel's buildings (the town's houses and works, and the grand keep). */
    public static List<RoomTemplate> buildings() {
        List<RoomTemplate> loaded = buildings;
        if (loaded == null) {
            synchronized (TemplateLibrary.class) {
                if (buildings == null) buildings = Collections.unmodifiableList(load(BUILDINGS));
                loaded = buildings;
            }
        }
        return loaded;
    }

    public static RoomTemplate building(String name) {
        for (RoomTemplate template : buildings()) if (template.name.equals(name)) return template;
        throw new IllegalArgumentException("no building " + name);
    }

    public static RoomTemplate named(String name) {
        for (RoomTemplate template : all()) if (template.name.equals(name)) return template;
        throw new IllegalArgumentException("no room template " + name);
    }

    private static List<RoomTemplate> load(String dir) {
        List<RoomTemplate> templates = new ArrayList<>();
        for (String name : lines(dir, "index.txt")) {
            name = name.trim();
            if (name.isEmpty() || name.startsWith(";")) continue;
            templates.add(RoomTemplate.parse(name, lines(dir, name + ".room")));
        }
        return templates;
    }

    private static List<String> lines(String dir, String file) {
        try (InputStream in = TemplateLibrary.class.getResourceAsStream(dir + file)) {
            if (in == null) throw new IllegalStateException("missing dungeon room file " + file);
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null;) lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read dungeon room file " + file, e);
        }
    }
}
