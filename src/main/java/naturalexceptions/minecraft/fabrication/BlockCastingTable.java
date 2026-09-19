package naturalexceptions.minecraft.fabrication;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

/**
 * Workstation block hosting metal casting molds, physical cooling, and pouring.
 */
public final class BlockCastingTable extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon iconTop;
    @SideOnly(Side.CLIENT)
    private IIcon iconSide;
    @SideOnly(Side.CLIENT)
    private IIcon iconBottom;

    public BlockCastingTable() {
        super(Material.rock);
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeStone);
        setBlockName("castingTable");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCastingTable();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        iconTop = reg.registerIcon("naturalexceptions:casting_table_top");
        iconSide = reg.registerIcon("naturalexceptions:casting_table_side");
        iconBottom = reg.registerIcon("naturalexceptions:starter_smelter_base");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 1) return iconTop;
        if (side == 0) return iconBottom;
        return iconSide;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityCastingTable)) {
            te = createNewTileEntity(world, world.getBlockMetadata(x, y, z));
            world.setTileEntity(x, y, z, te);
            te.markDirty();
            world.markBlockForUpdate(x, y, z);
        }
        TileEntityCastingTable table = (TileEntityCastingTable) te;
        ItemStack held = player.getHeldItem();
        ItemStack tableMold = table.getMold();

        // 1. Table has no mold -> player placing mold
        if (tableMold == null) {
            if (held != null && held.getItem() instanceof ItemCastingMold) {
                ItemStack placed = held.splitStack(1);
                if (held.stackSize <= 0) {
                    player.setCurrentItemOrArmor(0, null);
                }
                table.setMold(placed);
                player.inventoryContainer.detectAndSendChanges();
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.stone", 1.0F, 1.0F);
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7aPlaced \u00a7f" + placed.getDisplayName() + " \u00a7aon Casting Table."));
                return true;
            } else {
                player.addChatMessage(
                    new ChatComponentText("\u00a77Casting Table is empty. Place a Casting Mold on it."));
                return true;
            }
        }

        // 2. Table has mold: Solidified -> Extract cast component
        if (ItemCastingMold.isSolidified(tableMold)) {
            ItemStack component = ItemCastingMold.extractComponent(tableMold);
            if (component != null) {
                if (!player.inventory.addItemStackToInventory(component)) {
                    player.dropPlayerItemWithRandomChoice(component, false);
                }
                player.inventoryContainer.detectAndSendChanges();
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 1.0F, 1.2F);
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7aExtracted \u00a7f" + component.getDisplayName() + " \u00a7afrom Casting Mold!"));
                if (player.isSneaking()) {
                    // Sneak extraction also returns the empty mold
                    if (!player.inventory.addItemStackToInventory(tableMold)) {
                        player.dropPlayerItemWithRandomChoice(tableMold, false);
                    }
                    player.inventoryContainer.detectAndSendChanges();
                    table.setMold(null);
                } else {
                    table.markDirty();
                    world.markBlockForUpdate(x, y, z);
                }
                return true;
            }
        }

        // 3. Table has mold: Still Molten -> Display status or warn
        if (ItemCastingMold.isFilled(tableMold)) {
            ActiveThermalState thermal = ItemCastingMold.getThermalState(tableMold);
            double tempK = thermal != null ? thermal.temperatureKelvin() : 1400.0;
            double tempC = tempK - 273.15;
            player.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "\u00a7e[Casting Table] %s cooling: \u00a76%.1f \u00b0C (%.1f K) \u00a7c[MOLTEN]",
                        tableMold.getDisplayName(),
                        tempC,
                        tempK)));
            return true;
        }

        // 4. Table has mold: Empty
        // A: Sneak right-click retrieves the empty mold
        if (player.isSneaking()) {
            if (!player.inventory.addItemStackToInventory(tableMold)) {
                player.dropPlayerItemWithRandomChoice(tableMold, false);
            }
            player.inventoryContainer.detectAndSendChanges();
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.stone", 1.0F, 0.8F);
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7eRetrieved \u00a7f" + tableMold.getDisplayName() + " \u00a7efrom Casting Table."));
            table.setMold(null);
            return true;
        }

        // B: Normal right-click attempts pour from adjacent Starter Smelter
        TileEntityStarterSmelter adjacentSmelter = findAdjacentSmelter(world, x, y, z);
        if (adjacentSmelter != null) {
            if (adjacentSmelter.pourIntoMold(tableMold)) {
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7aPoured molten metal from adjacent Smelter into \u00a7f" + tableMold.getDisplayName()
                            + "\u00a7a!"));
                table.markDirty();
                world.markBlockForUpdate(x, y, z);
                return true;
            } else {
                double maxTemp = 293.15;
                for (naturalexceptions.core.metallurgy.AggregateLayer l : adjacentSmelter.vesselState()
                    .layers()) {
                    if (l.temperatureKelvin() > maxTemp) {
                        maxTemp = l.temperatureKelvin();
                    }
                }
                player.addChatMessage(
                    new ChatComponentText(
                        String.format(
                            "\u00a7e[Casting Table] Smelter has no liquid metal ready to pour (Crucible: %.0f K, need >= 1358 K). Sneak right-click to retrieve mold.",
                            maxTemp)));
                return true;
            }
        } else {
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7c[Casting Table] No adjacent Starter Smelter found. Place a Smelter next to or above this table, or sneak right-click to retrieve mold."));
            return true;
        }
    }

    private TileEntityStarterSmelter findAdjacentSmelter(World world, int x, int y, int z) {
        int[][] offsets = { { 0, 0, 1 }, { 0, 0, -1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, 1, 1 },
            { 0, 1, -1 }, { 1, 1, 0 }, { -1, 1, 0 }, { 0, -1, 0 }, { 0, -1, 1 }, { 0, -1, -1 }, { 1, -1, 0 },
            { -1, -1, 0 } };
        for (int[] off : offsets) {
            TileEntity te = world.getTileEntity(x + off[0], y + off[1], z + off[2]);
            if (te instanceof TileEntityStarterSmelter) {
                return (TileEntityStarterSmelter) te;
            }
        }
        return null;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityCastingTable) {
            ItemStack mold = ((TileEntityCastingTable) te).getMold();
            if (mold != null) {
                EntityItem entity = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, mold);
                world.spawnEntityInWorld(entity);
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
