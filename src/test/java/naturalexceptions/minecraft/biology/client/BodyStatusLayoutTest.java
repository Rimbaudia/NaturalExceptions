package naturalexceptions.minecraft.biology.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.minecraft.knowledge.client.InventoryOverlayLayout;

final class BodyStatusLayoutTest {

    @Test
    void sitsLeftOfTheInventoryWithoutCoveringIt() {
        // 854x480 scaled screen, vanilla 176x166 inventory centred.
        BodyStatusLayout layout = BodyStatusLayout.calculate(854, 480, 339, 157);
        assertEquals(BodyStatusLayout.PANEL_WIDTH, layout.panelWidth());
        assertTrue(layout.panelX() + layout.panelWidth() <= 339, "ends before the inventory");
        assertTrue(layout.panelX() >= 0);
        assertFalse(layout.overlaysScreenEdge());
        assertEquals(339, layout.tabX(), "tab on the inventory's top-left corner");
        assertTrue(layout.tabY() < 157);
        assertTrue(layout.listTop() < layout.listBottom());
    }

    @Test
    void shrinksThenOverlaysOnNarrowScreens() {
        BodyStatusLayout snug = BodyStatusLayout.calculate(430, 256, 127, 45);
        assertTrue(snug.panelWidth() < BodyStatusLayout.PANEL_WIDTH);
        assertTrue(snug.panelX() + snug.panelWidth() <= 127);
        assertFalse(snug.overlaysScreenEdge());

        BodyStatusLayout tight = BodyStatusLayout.calculate(260, 240, 42, 37);
        assertTrue(tight.overlaysScreenEdge());
        assertEquals(0, tight.reservedLeftWidth(42), "nothing to reserve when it overlays anyway");
    }

    @Test
    void figureHitTestsEachRegion() {
        BodyStatusLayout layout = BodyStatusLayout.calculate(854, 480, 339, 157);
        for (String region : BodyStatusLayout.dollRegions()) {
            int[] r = layout.regionRect(region);
            assertEquals(region, layout.regionAt(r[0] + r[2] / 2, r[1] + r[3] / 2));
        }
        assertNull(layout.regionAt(layout.dollX() - 1, layout.dollY() - 1));
        int[] head = layout.regionRect("head");
        int[] torso = layout.regionRect("torso");
        assertEquals(head[1] + head[3], torso[1], "head sits on the torso");
    }

    @Test
    void harmColourRunsGreenToRed() {
        int sound = BodyStatusLayout.harmColour(0.0F);
        int ruined = BodyStatusLayout.harmColour(1.0F);
        assertTrue(((sound >> 8) & 0xFF) > ((sound >> 16) & 0xFF), "sound is green");
        assertTrue(((ruined >> 16) & 0xFF) > ((ruined >> 8) & 0xFF) * 2, "ruined is red");
        assertEquals(0xFF000000, BodyStatusLayout.harmColour(0.7F) & 0xFF000000, "opaque");
    }

    @Test
    void itemBrowserKeepsOutOfTheOpenBodyPanel() {
        // Right side too narrow, left side wide: the browser would take the left, unless the Body panel holds it.
        InventoryOverlayLayout.Layout free = InventoryOverlayLayout.calculate(560, 300, 300, 60, 176, 166, 0);
        assertEquals(InventoryOverlayLayout.Side.LEFT, free.side());
        BodyStatusLayout body = BodyStatusLayout.calculate(560, 300, 300, 60);
        InventoryOverlayLayout.Layout shared = InventoryOverlayLayout
            .calculate(560, 300, 300, 60, 176, 166, body.reservedLeftWidth(300));
        assertTrue(
            shared.side() == InventoryOverlayLayout.Side.COMPACT
                || shared.panelX() + shared.panelWidth() <= body.panelX(),
            "no overlap");
    }
}
