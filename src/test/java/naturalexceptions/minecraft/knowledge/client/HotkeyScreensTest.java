package naturalexceptions.minecraft.knowledge.client;

import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;

import org.junit.jupiter.api.Test;

/** Regression: typing "k" in chat opened the Knowledge Browser. */
final class HotkeyScreensTest {

    @Test
    void chatAndOtherTextScreensNeverTakeHotkeys() {
        assertFalse(NEKnowledgeKeyHandler.acceptsHotkeys(new GuiChat()), "chat");
        assertFalse(NEKnowledgeKeyHandler.acceptsHotkeys(new GuiChat("/")), "command line");
        assertFalse(NEKnowledgeKeyHandler.acceptsHotkeys(new GuiScreen()), "any other screen");
        assertFalse(NEKnowledgeKeyHandler.acceptsHotkeys(null), "no screen (world hotkeys are handled elsewhere)");
    }
}
