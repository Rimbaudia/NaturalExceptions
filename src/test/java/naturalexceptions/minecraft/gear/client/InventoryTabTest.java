package naturalexceptions.minecraft.gear.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class InventoryTabTest {

    @Test
    void screenTabsLeftTogglesRightNoOverlap() {
        int left = 339;
        int top = 157;
        int width = 176;
        assertEquals(left, InventoryTab.INVENTORY.x(left, width));
        assertEquals(left + width - InventoryTab.WIDTH, InventoryTab.ITEMS.x(left, width));
        InventoryTab[] tabs = InventoryTab.values();
        for (int i = 0; i < tabs.length; i++) {
            int a = tabs[i].x(left, width);
            assertTrue(a >= left && a + InventoryTab.WIDTH <= left + width, tabs[i] + " within the screen's width");
            for (int j = i + 1; j < tabs.length; j++) {
                int b = tabs[j].x(left, width);
                assertTrue(a + InventoryTab.WIDTH <= b || b + InventoryTab.WIDTH <= a, tabs[i] + " vs " + tabs[j]);
            }
            assertEquals(tabs[i], InventoryTab.at(a + 14, top - 10, left, top, width));
        }
        assertTrue(!InventoryTab.GEAR.isToggle() && InventoryTab.BODY.isToggle());
        assertNull(InventoryTab.at(left + 90, top - 10, left, top, width), "gap between the groups");
        assertNull(InventoryTab.at(left + 14, top + 2, left, top, width), "the screen itself is not a tab");
    }
}
