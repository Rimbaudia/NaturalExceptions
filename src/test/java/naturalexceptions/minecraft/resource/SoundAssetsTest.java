package naturalexceptions.minecraft.resource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Every sound event in sounds.json points at an OGG file that exists. */
final class SoundAssetsTest {

    @Test
    void everySoundEventHasItsFile() throws Exception {
        File root = new File("src/main/resources/assets/naturalexceptions");
        String json = new String(Files.readAllBytes(new File(root, "sounds.json").toPath()), StandardCharsets.UTF_8);
        Matcher sounds = Pattern.compile("\"sounds\"\\s*:\\s*\\[\\s*\"([^\"]+)\"")
            .matcher(json);
        int count = 0;
        while (sounds.find()) {
            File ogg = new File(root, "sounds/" + sounds.group(1) + ".ogg");
            assertTrue(ogg.isFile() && ogg.length() > 1000, "missing or empty: " + ogg);
            count++;
        }
        assertFalse(count == 0, "no sounds found");
    }
}
