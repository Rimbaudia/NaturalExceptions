package naturalexceptions.core.worldgen.dungeon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The authored rooms, read once from {@code assets/naturalexceptions/dungeon/rooms/} (listed in index.txt). */
public final class TemplateLibrary {

    private static final String DIR = "/assets/naturalexceptions/dungeon/rooms/";
    private static volatile List<RoomTemplate> all;

    private TemplateLibrary() {}

    public static List<RoomTemplate> all() {
        List<RoomTemplate> loaded = all;
        if (loaded == null) {
            synchronized (TemplateLibrary.class) {
                if (all == null) all = Collections.unmodifiableList(load());
                loaded = all;
            }
        }
        return loaded;
    }

    public static RoomTemplate named(String name) {
        for (RoomTemplate template : all()) if (template.name.equals(name)) return template;
        throw new IllegalArgumentException("no room template " + name);
    }

    private static List<RoomTemplate> load() {
        List<RoomTemplate> templates = new ArrayList<>();
        for (String name : lines("index.txt")) {
            name = name.trim();
            if (name.isEmpty() || name.startsWith(";")) continue;
            templates.add(RoomTemplate.parse(name, lines(name + ".room")));
        }
        return templates;
    }

    private static List<String> lines(String file) {
        try (InputStream in = TemplateLibrary.class.getResourceAsStream(DIR + file)) {
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
