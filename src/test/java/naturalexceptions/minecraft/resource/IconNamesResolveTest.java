package naturalexceptions.minecraft.resource;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Every icon name used in code must resolve to a real texture, or the game shows the missing-texture pattern. */
final class IconNamesResolveTest {

    private static final Path SOURCES = Paths.get("src/main/java/naturalexceptions");
    private static final Path TEXTURES = Paths.get("src/main/resources/assets/naturalexceptions/textures");
    private static final Pattern ICON = Pattern.compile("\"(naturalexceptions|minecraft):([a-z0-9_/]+)\"");
    /** Vanilla 1.7.10 icons this mod legitimately borrows. */
    private static final Set<String> VANILLA = new HashSet<>(Arrays.asList("water_still"));

    @Test
    void everyIconNameInCodeHasATexture() throws Exception {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString()
                .endsWith(".java"))::iterator) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (!line.contains("registerIcon(") && !line.contains("TextureName(")) continue;
                    Matcher matcher = ICON.matcher(line);
                    while (matcher.find()) {
                        String domain = matcher.group(1);
                        String name = matcher.group(2);
                        if ("minecraft".equals(domain)) {
                            if (!VANILLA.contains(name)) problems.add(file.getFileName() + ": minecraft:" + name);
                        } else if (!Files.isRegularFile(TEXTURES.resolve("items/" + name + ".png"))
                            && !Files.isRegularFile(TEXTURES.resolve("blocks/" + name + ".png"))) {
                                problems.add(file.getFileName() + ": naturalexceptions:" + name);
                            }
                    }
                }
            }
        }
        // Icon names passed through constructors (e.g. knowledge books) are checked on the registered items.
        naturalexceptions.content.BuiltinContent.register();
        naturalexceptions.minecraft.registry.NEItems.register();
        naturalexceptions.minecraft.registry.NEBlocks.register();
        java.lang.reflect.Field iconString = net.minecraft.item.Item.class.getDeclaredField("iconString");
        iconString.setAccessible(true);
        for (java.lang.reflect.Field field : naturalexceptions.minecraft.registry.NEItems.class.getFields()) {
            Object value = field.get(null);
            if (!(value instanceof net.minecraft.item.Item)) continue;
            String name = (String) iconString.get(value);
            if (name == null) continue;
            check(name, "NEItems." + field.getName(), problems);
        }
        assertTrue(problems.isEmpty(), "Unresolved icon names: " + problems);
    }

    private static void check(String icon, String where, List<String> problems) {
        String[] parts = icon.split(":", 2);
        if (parts.length != 2) return;
        if ("minecraft".equals(parts[0])) {
            if (!VANILLA.contains(parts[1])) problems.add(where + ": " + icon);
        } else if ("naturalexceptions".equals(parts[0])
            && !Files.isRegularFile(TEXTURES.resolve("items/" + parts[1] + ".png"))
            && !Files.isRegularFile(TEXTURES.resolve("blocks/" + parts[1] + ".png"))) {
                problems.add(where + ": " + icon);
            }
    }
}
