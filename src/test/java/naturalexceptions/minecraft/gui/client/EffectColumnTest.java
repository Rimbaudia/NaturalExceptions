package naturalexceptions.minecraft.gui.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Potion effects stack down beside the inventory, wrapping into another column only when they run out of room. */
final class EffectColumnTest {

    @Test
    void effectsStackThenWrap() {
        assertEquals(0, EffectColumn.columns(0, 166), "no effects, no column");
        assertEquals(1, EffectColumn.columns(1, 166));
        // A 166-high inventory holds seven 20-px cells with 2-px gaps.
        assertEquals(1, EffectColumn.columns(7, 166));
        assertEquals(2, EffectColumn.columns(8, 166), "the eighth starts a second column");
        // Creative's screen is 136 high: six to a column.
        assertEquals(2, EffectColumn.columns(7, 136));
    }
}
