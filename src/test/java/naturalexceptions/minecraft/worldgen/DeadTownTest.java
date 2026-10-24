package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A citadel has been dead an age: its market and pantries hold what went bad and what keeps (dry goods, seed), its
 * forge's furnace the work it was left at, and its houses its people's last notes.
 */
final class DeadTownTest {

    private static final Set<Item> FRESH = new HashSet<>(
        java.util.Arrays.asList(
            Items.bread,
            Items.apple,
            Items.carrot,
            Items.potato,
            Items.cookie,
            Items.pumpkin_pie,
            Items.fish,
            Items.cooked_fished,
            Items.baked_potato,
            Items.melon));

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static Set<Item> items(InventoryBasic inventory) {
        Set<Item> items = new HashSet<>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) if (inventory.getStackInSlot(i) != null) items.add(
            inventory.getStackInSlot(i)
                .getItem());
        return items;
    }

    @Test
    void nothingFreshIsLeftInTheMarketOrThePantries() {
        Set<Item> seen = new HashSet<>();
        for (long seed = 0; seed < 300; seed++) {
            for (int goods = 0; goods < 4; goods++) {
                InventoryBasic counter = new InventoryBasic("counter", false, 9);
                Wares.stockCounter(counter, goods, seed);
                seen.addAll(items(counter));
            }
            InventoryBasic pantry = new InventoryBasic("pantry", false, 9),
                crate = new InventoryBasic("crate", false, 27);
            Wares.stockPantry(pantry, seed);
            Wares.stockCrate(crate, seed);
            seen.addAll(items(pantry));
            seen.addAll(items(crate));
            seen.add(
                Wares.barrelStore(seed)
                    .getItem());
            InventoryBasic chest = new InventoryBasic("house", false, 27);
            SkyCacheLoot.fill(chest, seed, SkyCacheLoot.Tier.HOUSEHOLD);
            seen.addAll(items(chest));
        }
        for (Item fresh : FRESH) assertFalse(seen.contains(fresh), fresh.getUnlocalizedName() + " would have rotted");
        assertTrue(
            seen.contains(Items.poisonous_potato) && seen.contains(Items.wheat_seeds),
            "rot, and seed that keeps");
    }

    @Test
    void theForgeStillHoldsItsWork() {
        InventoryBasic furnace = new InventoryBasic("furnace", false, 3);
        Wares.leftMidWork(furnace, 42L);
        assertTrue(
            furnace.getStackInSlot(0)
                .getItem() == Item.getItemFromBlock(Blocks.iron_ore),
            "ore in it");
        assertTrue(furnace.getStackInSlot(1) == null, "its fire burnt out: no fuel, so it stays cold");
        assertTrue(
            furnace.getStackInSlot(2)
                .getItem() == Items.iron_ingot,
            "a bar or two done");
    }

    @Test
    void itsPeopleLeftNotesAWrittenBookCanHold() {
        for (String[] note : TownNotes.NOTES) {
            assertTrue(note[0].length() <= 32 && note.length > 2, note[0]);
            for (int i = 2; i < note.length; i++) assertTrue(note[i].length() <= 256, note[0] + ": a page holds it");
            ItemStack book = TownNotes.book(note);
            NBTTagList pages = book.getTagCompound()
                .getTagList("pages", 8);
            assertTrue(pages.tagCount() == note.length - 2);
        }
        int notes = 0;
        for (long seed = 0; seed < 400; seed++) {
            InventoryBasic chest = new InventoryBasic("house", false, 27);
            SkyCacheLoot.fill(chest, seed, SkyCacheLoot.Tier.HOUSEHOLD);
            if (items(chest).contains(Items.written_book)) notes++;
        }
        assertTrue(notes > 70 && notes < 170, "about three houses in ten keep a note: " + notes + " / 400");
    }
}
