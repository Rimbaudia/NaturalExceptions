package naturalexceptions.minecraft.worldgen;

import java.util.Random;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * What the people of a dead citadel left in their houses: last notes, letters and ledgers, written books found among
 * their things. Together they tell what happened: the winds that held the city up failed, the crops with them, and
 * then a grey cough came; the Castellan shut the gates, and its machines still keep them; the dead were carried to
 * the Sanctum, and in the end the living lay down beside them.
 */
public final class TownNotes {

    private TownNotes() {}

    /** {title, author, page, page...}. */
    static final String[][] NOTES = { { "Baker's Reckoning", "Hale, baker",
        "Flour short again. The barges from the low isles have not come since the wind dropped.\n\nI baked half loaves and still the line ran to the well square. Wenna says the mill sails have not turned in nine days." },
        { "Note, Folded Small", "Unsigned",
            "The Castellan has shut the gates. No one in, no one out, it says, until the sickness passes.\n\nThe machines stand in the gate passage and will not hear us. My sister is outside." },
        { "Watch Roster", "Sergeant Corvin",
            "Third bell, the gate. Fourth bell, the wall-walk.\n\nAldo. Bren. Tamsin. Orrin.\n\nStruck: Aldo. Bren. Tamsin.",
            "No one is left to stand the watch but the Automatons. They do not tire. I envy them." },
        { "Prayer Card", "The Sanctum",
            "Keep them, Holy Ground, who we carry to you.\n\nKeep their names when we cannot.\n\nLet the wind come back to the sails and the rain to the gardens, and let us lie down beside them when it is our time." },
        { "Letter Never Sent", "Ilse",
            "Dear Mother,\n\nDo not come. I know you mean to. The fever takes the young first, and you are not young, but you are stubborn.",
            "The machines would not open the gate for you anyway.\n\nI love you. The bells for the dead ring every hour now." },
        { "Physician's Daybook", "Doctor Marrow",
            "Eleventh day of the calm. Grey cough in the lower town; fever by the second day, and by the fifth the lungs fill.\n\nWillow bark eases nothing.",
            "The Castellan asks for my count every evening. I have stopped giving it." },
        { "Child's Slate", "Mira, aged seven",
            "Mama says we are going to the Sanctum to say goodbye to Papa. I am taking my wooden bird.\n\nThe Automaton at our corner used to wave. It does not wave now." },
        { "Smith's Order Book", "Garth, smith",
            "Owed: twelve halberd heads for the Castellan's machines. Forty hinges. A cradle for the Tallows, paid in candles.\n\nOre in the furnace. Fire banked. Back after the procession." },
        { "Market Notice", "By order of the Castellan",
            "The market is closed.\n\nGoods are to be left where they lie.\n\nGather at the Sanctum at the evening bell." },
        { "Gardener's Almanac", "Pell",
            "No rain since the calm began. Sowed again anyway.\n\nThe seed keeps; it is only waiting.\n\nWhoever finds this: plant it. Something should grow here." },
        { "Steward's Log", "Osric, steward",
            "The Castellan asked me today what it should guard when there is no one left.\n\nI told it: guard the dead.\n\nIt bowed its crowned head, and I think it understood." },
        { "Sermon for the Calm", "Mother Aveline",
            "The wind is the breath of the world, and it has gone out of us a while.\n\nCarry your dead to the Sanctum, and do not weep where they can hear. They are not gone. They are gathered." } };

    /** One of the notes, as a written book. */
    public static ItemStack any(Random random) {
        return book(NOTES[random.nextInt(NOTES.length)]);
    }

    static ItemStack book(String[] note) {
        ItemStack book = new ItemStack(Items.written_book);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", note[0]);
        tag.setString("author", note[1]);
        NBTTagList pages = new NBTTagList();
        for (int i = 2; i < note.length; i++) pages.appendTag(new NBTTagString(note[i]));
        tag.setTag("pages", pages);
        book.setTagCompound(tag);
        return book;
    }
}
