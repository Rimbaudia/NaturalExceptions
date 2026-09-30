package naturalexceptions.minecraft.furniture;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * A cutting bench's screen: the piece to cut on the left, and to its right one slot for each thing it can be cut
 * into. Taking one of those cuts it (shift-click cuts all you have into it).
 */
public final class ContainerCutting extends Container {

    /** The choices shown, four across and three down. */
    public static final int CHOICES = 12, ACROSS = 4;
    public static final int INPUT_X = 26, INPUT_Y = 35, CHOICE_X = 80, CHOICE_Y = 17;
    /** Where the input and the fuel sit on a bench that needs a fire. */
    public static final int FIRED_INPUT_Y = 17, FUEL_Y = 53;

    private final TileEntityCuttingBench bench;
    private final CuttingRecipes.Kind kind;
    private final Choices choices = new Choices();

    public ContainerCutting(IInventory player, TileEntityCuttingBench bench) {
        this.bench = bench;
        this.kind = bench.kind();
        addSlotToContainer(new Slot(bench, 0, INPUT_X, kind.fired() ? FIRED_INPUT_Y : INPUT_Y) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return CuttingRecipes.accepts(kind, stack);
            }
        });
        // The fuel slot, for a bench with a fire; kept off the screen on the others.
        addSlotToContainer(new Slot(bench, TileEntityCuttingBench.FUEL, kind.fired() ? INPUT_X : -1000, FUEL_Y) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return bench.isItemValidForSlot(TileEntityCuttingBench.FUEL, stack);
            }
        });
        for (int i = 0; i < CHOICES; i++)
            addSlotToContainer(new Choice(choices, i, CHOICE_X + (i % ACROSS) * 18, CHOICE_Y + (i / ACROSS) * 18));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            addSlotToContainer(new Slot(player, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(player, c, 8 + c * 18, 142));
    }

    /** What the piece on the bench can be cut into now. */
    List<CuttingRecipes.Recipe> recipes() {
        if (!bench.hot()) return java.util.Collections.emptyList();
        return CuttingRecipes.forInput(kind, bench.getStackInSlot(0));
    }

    /** Cuts one: takes the recipe's input off the bench. */
    void cut(CuttingRecipes.Recipe recipe, EntityPlayer player) {
        bench.decrStackSize(0, recipe.count());
        bench.burn();
        if (!player.worldObj.isRemote) player.worldObj.playSoundEffect(
            bench.xCoord + 0.5,
            bench.yCoord + 0.5,
            bench.zCoord + 0.5,
            BlockCuttingBench.SOUNDS[kind.ordinal()],
            1.0F,
            0.8F + player.worldObj.rand.nextFloat() * 0.4F);
    }

    private int sentHeat = -1;

    /** Tells the screen how much fire a bench that needs one has left. */
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!kind.fired() || bench.heat() == sentHeat) return;
        sentHeat = bench.heat();
        for (Object crafter : crafters)
            ((net.minecraft.inventory.ICrafting) crafter).sendProgressBarUpdate(this, 0, sentHeat);
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) bench.setHeat(value);
    }

    /** Whether the bench can work now (for a fired one: a fire, or fuel to light it). */
    public boolean hot() {
        return bench.hot();
    }

    public boolean fired() {
        return kind.fired();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return bench.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        int own = 2 + CHOICES;
        if (slot instanceof Choice) {
            List<CuttingRecipes.Recipe> list = recipes();
            int i = ((Choice) slot).getSlotIndex();
            if (i >= list.size()) return null;
            CuttingRecipes.Recipe recipe = list.get(i);
            ItemStack made = recipe.output();
            ItemStack copy = made.copy();
            if (!mergeItemStack(made, own, inventorySlots.size(), true)) return null;
            if (made.stackSize > 0) player.dropPlayerItemWithRandomChoice(made, false);
            cut(recipe, player);
            return copy;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index <= 1) {
            if (!mergeItemStack(stack, own, inventorySlots.size(), true)) return null;
        } else if (CuttingRecipes.accepts(kind, stack)) {
            if (!mergeItemStack(stack, 0, 1, false)) return null;
        } else if (bench.isItemValidForSlot(TileEntityCuttingBench.FUEL, stack)) {
            if (!mergeItemStack(stack, 1, 2, false)) return null;
        } else return null;
        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();
        return copy;
    }

    /** The choices, read afresh from the piece on the bench whenever they are looked at. */
    private final class Choices extends net.minecraft.inventory.InventoryBasic {

        Choices() {
            super("choices", false, CHOICES);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            List<CuttingRecipes.Recipe> list = recipes();
            return slot < list.size() ? list.get(slot)
                .output() : null;
        }

        @Override
        public ItemStack decrStackSize(int slot, int count) {
            return getStackInSlot(slot);
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {}
    }

    /** A choice: nothing goes in; taking it cuts the piece. */
    private final class Choice extends Slot {

        Choice(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
            List<CuttingRecipes.Recipe> list = recipes();
            if (getSlotIndex() < list.size()) cut(list.get(getSlotIndex()), player);
            super.onPickupFromSlot(player, stack);
        }
    }
}
