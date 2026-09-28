package naturalexceptions.minecraft.knowledge.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

/**
 * Layout and scrolling helper for the Knowledge Browser GUI, ensuring strict
 * boundary compliance and zero horizontal text overflow.
 */
public final class TextLayoutHelper {

    private TextLayoutHelper() {}

    /**
     * Interface for string pixel-width measurement, enabling headless unit testing without OpenGL.
     */
    @FunctionalInterface
    public interface IWidthMeasurer {

        int getWidth(String str);
    }

    /**
     * Wraps text strictly within the specified pixel width using the FontRenderer.
     */
    public static List<String> wrap(FontRenderer fontRenderer, String text, int maxPixelWidth) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        if (fontRenderer != null) {
            return fontRenderer.listFormattedStringToWidth(text, maxPixelWidth);
        }
        // Fallback for headless testing: approximate 6 pixels per character
        return wrapWithMeasurer(text, maxPixelWidth, s -> s.length() * 6);
    }

    /**
     * Wraps text strictly within the specified pixel width using a custom width measurer.
     * Preserves explicit newlines and paragraph breaks.
     */
    public static List<String> wrapWithMeasurer(String text, int maxPixelWidth, IWidthMeasurer measurer) {
        if (text == null || text.isEmpty() || maxPixelWidth <= 0 || measurer == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.trim()
                .isEmpty()) {
                result.add("");
                continue;
            }

            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (measurer.getWidth(word) > maxPixelWidth) {
                    if (currentLine.length() > 0) {
                        result.add(currentLine.toString());
                        currentLine.setLength(0);
                    }
                    StringBuilder piece = new StringBuilder();
                    for (int offset = 0; offset < word.length();) {
                        int codePoint = word.codePointAt(offset);
                        String character = new String(Character.toChars(codePoint));
                        String candidate = piece.toString() + character;
                        if (piece.length() > 0 && measurer.getWidth(candidate) > maxPixelWidth) {
                            result.add(piece.toString());
                            piece.setLength(0);
                        }
                        piece.append(character);
                        offset += Character.charCount(codePoint);
                    }
                    if (piece.length() > 0) result.add(piece.toString());
                    continue;
                }
                if (currentLine.length() == 0) {
                    currentLine.append(word);
                } else {
                    String candidate = currentLine + " " + word;
                    if (measurer.getWidth(candidate) <= maxPixelWidth) {
                        currentLine.append(" ")
                            .append(word);
                    } else {
                        result.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    }
                }
            }

            if (currentLine.length() > 0) {
                result.add(currentLine.toString());
            }
        }

        return result;
    }

    /**
     * Calculates the maximum vertical scroll offset.
     */
    public static int calculateMaxScroll(int totalContentHeight, int viewportHeight) {
        return Math.max(0, totalContentHeight - viewportHeight);
    }

    /**
     * Clamps scroll offset within valid bounds [0, maxScroll].
     */
    public static int clampScroll(int currentScroll, int maxScroll) {
        return Math.max(0, Math.min(currentScroll, Math.max(0, maxScroll)));
    }
}
