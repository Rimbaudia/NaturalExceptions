package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.minecraft.fabrication.TileEntityForgingAnvil;

/**
 * Blacksmith Hammer used for hot-working and cold-working metal components and blanks on the forging anvil.
 *
 * <p>
 * Supports 4 explicit, physically meaningful shaping faces:
 * <ul>
 * <li><b>FLATTEN (Flat Face)</b>: Compresses stock thickness, spreading blanks into wide plates.</li>
 * <li><b>DRAW_OUT (Peen Face)</b>: Elongates stock along length into rods or rounded handles.</li>
 * <li><b>BEVEL (Cross Peen)</b>: Tapers plate edges into blades or rod ends into pick points.</li>
 * <li><b>PLANISH (Finishing Face)</b>: Levels surfaces and refines component manufacturing state to finished.</li>
 * </ul>
 */
public final class ItemBlacksmithHammer extends Item {

    public static final String KEY_OPERATION = "HammerOp";

    public enum HammerOperation {

        FLATTEN("Flattening", "Compresses stock thickness into wide plates"),
        DRAW_OUT("Drawing Out", "Stretches stock lengthwise into rods"),
        BEVEL("Beveling / Tapering", "Shapes cutting edges and pointed tool heads"),
        PLANISH("Planishing", "Smooths and levels forged surfaces to finished state");

        private final String displayName;
        private final String description;

        HammerOperation(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }

        public HammerOperation next() {
            HammerOperation[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static HammerOperation fromId(String id) {
            if (id == null) return FLATTEN;
            for (HammerOperation op : values()) {
                if (op.name()
                    .equalsIgnoreCase(id.trim())) {
                    return op;
                }
            }
            return FLATTEN;
        }
    }

    public ItemBlacksmithHammer() {
        setMaxStackSize(1);
        setMaxDamage(250);
        setUnlocalizedName("blacksmithHammer");
        setTextureName("naturalexceptions:blacksmith_hammer");
        setFull3D();
    }

    public static HammerOperation getOperation(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(KEY_OPERATION)) {
            return HammerOperation.FLATTEN;
        }
        return HammerOperation.fromId(
            stack.getTagCompound()
                .getString(KEY_OPERATION));
    }

    public static void setOperation(ItemStack stack, HammerOperation op) {
        if (stack == null) return;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setString(KEY_OPERATION, (op != null ? op : HammerOperation.FLATTEN).name());
    }

    public static HammerOperation cycleOperation(ItemStack stack) {
        HammerOperation current = getOperation(stack);
        HammerOperation next = current.next();
        setOperation(stack, next);
        return next;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            HammerOperation next = cycleOperation(stack);
            if (!world.isRemote) {
                world.playSoundEffect(player.posX, player.posY, player.posZ, "random.click", 0.8F, 1.6F);
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7b[Blacksmith Hammer] Active Face: \u00a7f" + next.displayName()
                            + " \u00a77("
                            + next.description()
                            + ")"));
            }
        }
        return stack;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityForgingAnvil) {
            TileEntityForgingAnvil anvil = (TileEntityForgingAnvil) te;
            return anvil.hammerStrike(player, stack);
        }
        return false;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add("\u00a76Strikes and shapes workpieces on the forging anvil.");
        list.add("\u00a77Advances workpiece towards anvil's target geometry.");
        list.add("\u00a78Sneak-click anvil to cycle target (Pick Head, Blade, etc.)");

        if (stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(KEY_OPERATION)) {
            HammerOperation op = getOperation(stack);
            list.add("  \u00a7bManual Face Override: \u00a7f" + op.displayName());
            list.add("  \u00a77" + op.description());
            list.add("\u00a78(Shift + Right-Click air to cycle manual face)");
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(KEY_OPERATION)) {
            HammerOperation op = getOperation(stack);
            return "Blacksmith Hammer [" + op.displayName() + "]";
        }
        return "Blacksmith Hammer";
    }
}
