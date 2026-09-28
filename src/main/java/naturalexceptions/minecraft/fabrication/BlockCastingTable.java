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
        if (world != null && world.isRemote) {
            return true;
        }

        TileEntity te = world != null ? world.getTileEntity(x, y, z) : null;
        if (!(te instanceof TileEntityCastingTable)) {
            if (world != null) {
                te = createNewTileEntity(world, world.getBlockMetadata(x, y, z));
                world.setTileEntity(x, y, z, te);
                te.markDirty();
                world.markBlockForUpdate(x, y, z);
            }
        }
        return handleInteraction((TileEntityCastingTable) te, player, world, x, y, z);
    }

    public boolean handleInteraction(TileEntityCastingTable table, EntityPlayer player, World world, int x, int y,
        int z) {
        if (table == null || player == null) {
            return false;
        }
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
                if (world != null) {
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.stone", 1.0F, 1.0F);
                }
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

        // 1b. Pouring from Foundry Ladle directly into mold on table
        if (held != null && held.getItem() instanceof naturalexceptions.minecraft.item.ItemFoundryLadle) {
            if (ItemCastingMold.isFilled(tableMold)) {
                player.addChatMessage(new ChatComponentText("\u00a7eMold on Casting Table is already full of metal!"));
                return true;
            }
            if (naturalexceptions.minecraft.item.ItemFoundryLadle.pourIntoCastingTable(held, table)) {
                player.inventoryContainer.detectAndSendChanges();
                player.addChatMessage(
                    new ChatComponentText("\u00a7aPoured molten metal from Foundry Ladle into casting mold."));
                return true;
            } else {
                if (naturalexceptions.minecraft.item.ItemFoundryLadle.isSolidified(held)) {
                    player.addChatMessage(
                        new ChatComponentText("\u00a7cMetal in ladle has solidified! Remelt it in a smelter to pour."));
                } else {
                    player.addChatMessage(
                        new ChatComponentText("\u00a7cFoundry Ladle is empty or metal is incompatible."));
                }
                return true;
            }
        }

        // 1c. Blacksmith Tongs interaction: safely extract solidified component into tongs
        if (held != null && held.getItem() instanceof naturalexceptions.minecraft.item.ItemBlacksmithTongs) {
            if (ItemCastingMold.isFilled(tableMold) && ItemCastingMold.isSolidified(tableMold)) {
                if (!naturalexceptions.minecraft.item.ItemBlacksmithTongs.hasWorkpiece(held)) {
                    ItemStack component = ItemCastingMold.extractComponent(tableMold);
                    if (component != null) {
                        naturalexceptions.minecraft.item.ItemBlacksmithTongs.setWorkpiece(held, component);
                        player.inventoryContainer.detectAndSendChanges();
                        table.markDirty();
                        if (world != null) {
                            world.markBlockForUpdate(x, y, z);
                            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 1.0F, 1.2F);
                        }
                        player.addChatMessage(
                            new ChatComponentText(
                                "\u00a7a[Blacksmith Tongs] Safely grabbed hot \u00a7f" + component.getDisplayName()
                                    + " \u00a7afrom casting mold!"));
                        return true;
                    }
                } else {
                    player.addChatMessage(
                        new ChatComponentText("\u00a7e[Blacksmith Tongs] Tongs are already holding a workpiece!"));
                    return true;
                }
            }
        }

        // 2. Sneak right-click retrieves the hosted mold from the casting table (empty, solidified, or malformed)
        if (player.isSneaking()) {
            if (!player.inventory.addItemStackToInventory(tableMold)) {
                player.dropPlayerItemWithRandomChoice(tableMold, false);
            }
            player.inventoryContainer.detectAndSendChanges();
            if (world != null) {
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.stone", 1.0F, 0.8F);
            }
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7eRetrieved \u00a7f" + tableMold.getDisplayName() + " \u00a7efrom Casting Table."));
            table.setMold(null);
            return true;
        }

        // 3. Table has mold: Solidified and Filled -> Extract cast component
        if (ItemCastingMold.isFilled(tableMold) && ItemCastingMold.isSolidified(tableMold)) {
            boolean recovered = ItemCastingMold.isRecovered(tableMold);
            ItemStack component = ItemCastingMold.extractComponent(tableMold);
            if (component != null) {
                ActiveThermalState thermal = naturalexceptions.minecraft.item.ItemGenericComponent
                    .getThermalState(component);
                if (thermal != null && thermal.temperatureKelvin() > 373.15) {
                    player.attackEntityFrom(net.minecraft.util.DamageSource.onFire, 1.0F);
                    player.addChatMessage(
                        new ChatComponentText(
                            "\u00a7c[Casting Table] Ouch! The cast component is scalding hot! Use blacksmith tongs to handle it."));
                }
                if (!player.inventory.addItemStackToInventory(component)) {
                    player.dropPlayerItemWithRandomChoice(component, false);
                }
                player.inventoryContainer.detectAndSendChanges();
                if (world != null) {
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 1.0F, 1.2F);
                    world.markBlockForUpdate(x, y, z);
                }
                String msg = "\u00a7aExtracted \u00a7f" + component.getDisplayName() + " \u00a7afrom Casting Mold!";
                if (recovered) {
                    msg += " \u00a77(Recovered from legacy data)";
                }
                player.addChatMessage(new ChatComponentText(msg));
                table.markDirty();
                return true;
            } else if (ItemCastingMold.isScrapable(tableMold)) {
                ItemCastingMold.clearContainedPayload(tableMold);
                table.markDirty();
                if (world != null) {
                    world.markBlockForUpdate(x, y, z);
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.gravel", 1.0F, 0.8F);
                }
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7e[Casting Table] Scraped corrupted residues from mold cavity. Mold reclaimed on table."));
                return true;
            } else {
                String diagnostic = ItemCastingMold.getPayloadDiagnostic(tableMold);
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7c[Casting Table] "
                            + (diagnostic != null ? diagnostic : "Mold contents could not be extracted.")
                            + " \u00a77(Sneak right-click to retrieve mold)"));
                return true;
            }
        }

        // 4. Table has mold: Still Molten -> Display status or warn
        if (ItemCastingMold.isFilled(tableMold)) {
            ActiveThermalState thermal = ItemCastingMold.getThermalState(tableMold);
            double tempK = thermal != null ? thermal.temperatureKelvin() : 1400.0;
            boolean molten = thermal != null
                && thermal.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID;
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7e[Casting Table] The metal is "
                        + (molten ? "still molten" : "set, and " + naturalexceptions.minecraft.item.HeatWords.describe(tempK))
                        + ". "
                        + (molten ? "Let it cool before taking it out." : "Tongs can lift it off the table.")));
            return true;
        }

        player.addChatMessage(
            new ChatComponentText("\u00a7e[Casting Table] Empty mold ready; pour through an aligned Smelter Outlet."));
        return true;
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
