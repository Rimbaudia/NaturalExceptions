package naturalexceptions.minecraft.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import naturalexceptions.minecraft.knowledge.client.TextLayoutHelper;

class GuiTextLayoutTest {

    @Test
    void testTextWrappingRespectsPixelWidth() {
        String longText = "Heat-resistant refractory masonry engineered to contain extreme thermal crucibles "
            + "exceeding 1600 \u00b0C without melting or cracking. They are the fundamental structural block "
            + "required to construct the Starter Smelter crucible, as well as the Casting Table, Smelter Outlet, "
            + "Tuyere, and Assembly Table.";

        int maxPixelWidth = 200;
        TextLayoutHelper.IWidthMeasurer measurer = s -> s.length() * 6; // 6px per char

        List<String> lines = TextLayoutHelper.wrapWithMeasurer(longText, maxPixelWidth, measurer);
        assertFalse(lines.isEmpty(), "Should produce wrapped lines");
        assertTrue(lines.size() > 1, "Long text must wrap into multiple lines");

        for (String line : lines) {
            int width = measurer.getWidth(line);
            assertTrue(
                width <= maxPixelWidth,
                "Line must never exceed maximum pixel width (" + width + " > " + maxPixelWidth + "): '" + line + "'");
        }
    }

    @Test
    void testParagraphSpacingPreserved() {
        String text = "First paragraph content.\n\nSecond paragraph content.";
        TextLayoutHelper.IWidthMeasurer measurer = s -> s.length() * 6;

        List<String> lines = TextLayoutHelper.wrapWithMeasurer(text, 250, measurer);
        assertTrue(lines.contains(""), "Explicit paragraph break must produce empty line delimiter");
        assertEquals("First paragraph content.", lines.get(0));
        assertEquals("Second paragraph content.", lines.get(2));
    }

    @Test
    void testOverlongUnbrokenWordStillFitsPixelWidth() {
        TextLayoutHelper.IWidthMeasurer measurer = s -> s.length() * 5;
        List<String> lines = TextLayoutHelper.wrapWithMeasurer("extraordinarilylongunbrokenword", 30, measurer);
        assertTrue(lines.size() > 1);
        for (String line : lines) assertTrue(measurer.getWidth(line) <= 30, line);
    }

    @Test
    void testScrollRangeCalculations() {
        // Content shorter than viewport
        assertEquals(0, TextLayoutHelper.calculateMaxScroll(100, 150));

        // Content taller than viewport
        assertEquals(150, TextLayoutHelper.calculateMaxScroll(300, 150));

        // Scroll clamping
        assertEquals(0, TextLayoutHelper.clampScroll(-50, 150));
        assertEquals(100, TextLayoutHelper.clampScroll(100, 150));
        assertEquals(150, TextLayoutHelper.clampScroll(200, 150));
    }
}
