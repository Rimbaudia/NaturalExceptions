package naturalexceptions.minecraft.fabrication;

import net.minecraft.block.Block;
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
import naturalexceptions.minecraft.item.ItemBlacksmithHammer;
import naturalexceptions.minecraft.item.ItemBlacksmithTongs;
import naturalexceptions.minecraft.item.ItemGenericComponent;

/**
 * Forging Anvil block used for shaping and working metal components and blanks.
 */
public final class BlockForgingAnvil extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon iconTop;
    @SideOnly(Side.CLIENT)
    private IIcon iconSide;
    @SideOnly(Side.CLIENT)
    private IIcon iconBase;

    public BlockForgingAnvil() {
        super(Material.iron);
        setHardness(3.5F);
        setResistance(15.0F);
        setStepSound(Block.soundTypeAnvil);
        setBlockName("forgingAnvil");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        iconTop = reg.registerIcon("naturalexceptions:forging_anvil_top");
        iconSide = reg.registerIcon("naturalexceptions:forging_anvil_side");
        iconBase = reg.registerIcon("naturalexceptions:forging_anvil_base");
        this.blockIcon = iconTop;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 1) return iconTop;
        if (side == 0) return iconBase;
        return iconSide;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityForgingAnvil();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityForgingAnvil)) {
            return false;
        }
        TileEntityForgingAnvil anvil = (TileEntityForgingAnvil) te;
        ItemStack held = player.getHeldItem();
        ItemStack currentWorkpiece = anvil.getWorkpiece();

        // 1. Hammer strike
        if (held != null && held.getItem() instanceof ItemBlacksmithHammer) {
            boolean struck = anvil.hammerStrike(player, held);
            player.inventoryContainer.detectAndSendChanges();
            return struck;
        }

        // 2. Blacksmith Tongs interaction (place or retrieve workpiece)
        if (held != null && held.getItem() instanceof ItemBlacksmithTongs) {
            return anvil.tongsInteract(player, held);
        }

        // 0. Sneak right-click: cycle intended target geometry
        if (player.isSneaking()) {
            naturalexceptions.core.fabrication.GeometryId nextTarget = anvil.cycleTargetGeometry(player);
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7b[Forging Target] \u00a7f" + ItemGenericComponent.formatGeometryName(nextTarget)));
            return true;
        }

        // 4. Place workpiece onto empty anvil
        if (currentWorkpiece == null && held != null) {
            boolean valid = held.getItem() instanceof ItemGenericComponent;
            if (!valid) {
                naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry.ResolvedInput input = naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry
                    .resolve(held);
                valid = (input != null && input.kind()
                    == naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry.InputKind.METALLIC_PRODUCT);
            }
            if (valid) {
                ItemStack placed = held.splitStack(1);
                if (held.stackSize <= 0) {
                    player.setCurrentItemOrArmor(0, null);
                }
                anvil.setWorkpiece(placed);
                player.inventoryContainer.detectAndSendChanges();
                world.playSoundEffect(x + 0.5D, y + 0.8D, z + 0.5D, "step.stone", 1.0F, 1.0F);
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7aPlaced \u00a7f" + placed.getDisplayName() + " \u00a7aon forging anvil."));
                return true;
            }
        }

        // 5. Normal right-click with empty hand: retrieve workpiece from anvil
        if (currentWorkpiece != null && held == null) {
            ActiveThermalState thermal = ItemGenericComponent.getThermalState(currentWorkpiece);
            if (thermal != null && thermal.temperatureKelvin() > 373.15) {
                player.attackEntityFrom(net.minecraft.util.DamageSource.onFire, 1.0F);
                world.playSoundEffect(player.posX, player.posY, player.posZ, "random.fizz", 0.7F, 1.2F);
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7c[Forging Anvil] Ouch! The workpiece is scalding hot! Use blacksmith tongs to pick it up."));
                return true;
            }
            anvil.setWorkpiece(null);
            if (!player.inventory.addItemStackToInventory(currentWorkpiece)) {
                player.dropPlayerItemWithRandomChoice(currentWorkpiece, false);
            }
            player.inventoryContainer.detectAndSendChanges();
            world.playSoundEffect(x + 0.5D, y + 0.8D, z + 0.5D, "step.stone", 1.0F, 0.8F);
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7eRetrieved \u00a7f" + currentWorkpiece.getDisplayName() + " \u00a7efrom anvil."));
            return true;
        }

        player.addChatMessage(
            new ChatComponentText("\u00a77Forging anvil is empty. Place a component, blank, or ingot."));
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityForgingAnvil) {
            TileEntityForgingAnvil anvil = (TileEntityForgingAnvil) te;
            ItemStack wp = anvil.getWorkpiece();
            if (wp != null) {
                float rx = world.rand.nextFloat() * 0.8F + 0.1F;
                float ry = world.rand.nextFloat() * 0.8F + 0.1F;
                float rz = world.rand.nextFloat() * 0.8F + 0.1F;
                EntityItem entityItem = new EntityItem(world, x + rx, y + ry, z + rz, wp);
                world.spawnEntityInWorld(entityItem);
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
