package naturalexceptions.minecraft.metallurgy;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.metallurgy.AggregateLayer;

/**
 * Block hosting the Starter Smelter vessel.
 */
public final class BlockStarterSmelter extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon iconSide;
    @SideOnly(Side.CLIENT)
    private IIcon iconTop;
    @SideOnly(Side.CLIENT)
    private IIcon iconBottom;
    @SideOnly(Side.CLIENT)
    private IIcon iconFront;
    @SideOnly(Side.CLIENT)
    private IIcon iconFrontLit;

    public BlockStarterSmelter() {
        super(Material.rock);
        setHardness(2.5F);
        setResistance(10.0F);
        setStepSound(soundTypeStone);
        setBlockName("starterSmelter");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        iconSide = reg.registerIcon("naturalexceptions:starter_smelter_side");
        iconTop = reg.registerIcon("naturalexceptions:starter_smelter_top");
        iconBottom = reg.registerIcon("naturalexceptions:starter_smelter_base");
        iconFront = reg.registerIcon("naturalexceptions:starter_smelter_front");
        iconFrontLit = reg.registerIcon("naturalexceptions:starter_smelter_front_lit");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 1) return iconTop;
        if (side == 0) return iconBottom;
        int frontSide = (meta == 0) ? 3 : meta;
        if (side == frontSide) {
            return iconFront;
        }
        return iconSide;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 1) return iconTop;
        if (side == 0) return iconBottom;
        int meta = world.getBlockMetadata(x, y, z);
        int frontSide = (meta == 0) ? 3 : meta;
        if (side == frontSide) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityStarterSmelter) {
                if (((TileEntityStarterSmelter) te).vesselState()
                    .fuelState()
                    .isBurning()) {
                    return iconFrontLit;
                }
            }
            return iconFront;
        }
        return iconSide;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        int l = MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        if (l == 0) world.setBlockMetadataWithNotify(x, y, z, 2, 2); // North
        if (l == 1) world.setBlockMetadataWithNotify(x, y, z, 5, 2); // East
        if (l == 2) world.setBlockMetadataWithNotify(x, y, z, 3, 2); // South
        if (l == 3) world.setBlockMetadataWithNotify(x, y, z, 4, 2); // West
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityStarterSmelter();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityStarterSmelter)) {
            return false;
        }
        TileEntityStarterSmelter smelter = (TileEntityStarterSmelter) te;
        ItemStack held = player.getHeldItem();

        // 1. Direct outlet interaction: pour directly into held empty casting mold
        if (held != null && held.getItem() instanceof naturalexceptions.minecraft.item.ItemCastingMold) {
            if (!naturalexceptions.minecraft.item.ItemCastingMold.isFilled(held)) {
                ItemStack singleMold = held.stackSize == 1 ? held : held.splitStack(1);
                if (smelter.pourIntoMold(singleMold)) {
                    if (singleMold != held) {
                        if (!player.inventory.addItemStackToInventory(singleMold)) {
                            player.dropPlayerItemWithRandomChoice(singleMold, false);
                        }
                    }
                    player.inventoryContainer.detectAndSendChanges();
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
                    player.addChatMessage(
                        new ChatComponentText("\u00a7aPoured molten metal into " + singleMold.getDisplayName()));
                    return true;
                } else {
                    if (singleMold != held) {
                        held.stackSize++;
                    }
                    double maxTemp = 293.15;
                    for (naturalexceptions.core.metallurgy.AggregateLayer l : smelter.vesselState()
                        .layers()) {
                        if (l.temperatureKelvin() > maxTemp) {
                            maxTemp = l.temperatureKelvin();
                        }
                    }
                    player.addChatMessage(
                        new ChatComponentText(
                            String.format(
                                "\u00a7cNo accessible liquid metal in smelter to pour (Crucible: %.0f K, need >= 1358 K).",
                                maxTemp)));
                    return true;
                }
            }
        }

        // 2. Direct fuel feeding from hand (sneak right-click feeds up to 16, normal feeds 1)
        if (held != null && net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(held) > 0) {
            int burnTimePerItem = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(held);
            int countToFeed = player.isSneaking() ? Math.min(held.stackSize, 16) : 1;
            int totalBurnAdded = burnTimePerItem * countToFeed;

            smelter.vesselState()
                .addFuel(totalBurnAdded);

            if (held.getItem()
                .hasContainerItem(held)) {
                ItemStack container = held.getItem()
                    .getContainerItem(held);
                held.stackSize--;
                if (held.stackSize <= 0) {
                    player.setCurrentItemOrArmor(0, container);
                } else if (container != null && !player.inventory.addItemStackToInventory(container)) {
                    player.dropPlayerItemWithRandomChoice(container, false);
                }
            } else {
                held.stackSize -= countToFeed;
                if (held.stackSize <= 0) {
                    player.setCurrentItemOrArmor(0, null);
                }
            }
            player.inventoryContainer.detectAndSendChanges();
            smelter.markDirty();
            world.markBlockForUpdate(x, y, z);
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "fire.ignite", 1.0F, 1.0F);

            int remaining = smelter.vesselState()
                .fuelState()
                .remainingTicks();
            player.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "\u00a76[Starter Smelter] Fed %dx fuel to fire. (Fire duration: %d ticks / %.0fs)",
                        countToFeed,
                        remaining,
                        remaining / 20.0)));
            return true;
        }

        // 3. Direct feed charge insertion from hand
        if (held != null && TileEntityStarterSmelter.isSmeltableFeed(held)) {
            ItemStack singleCharge = held.stackSize == 1 ? held : held.splitStack(1);
            if (smelter.insertFeed(singleCharge)) {
                if (singleCharge != held && singleCharge.stackSize > 0) {
                    held.stackSize += singleCharge.stackSize;
                }
                player.inventoryContainer.detectAndSendChanges();
                smelter.markDirty();
                world.markBlockForUpdate(x, y, z);
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.stone", 1.0F, 1.0F);
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7a[Starter Smelter] Added 1x " + held.getDisplayName() + " to crucible charge."));
                return true;
            }
        }

        // 2. Developer/debug inspect: sneak right-click outputs coarse status to chat
        if (player.isSneaking()) {
            smelter.validateStructure();
            if (!smelter.isStructureValid()) {
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7c[Starter Smelter] Structure Incomplete! Requires solid hearth base. (Use /ne_smelter structure)"));
                return true;
            }

            player.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "\u00a7e[Smelter Status] Structure: \u00a7aValid\u00a7e | Layers: %d | Mass: %s | Fuel: %s",
                        smelter.vesselState()
                            .layers()
                            .size(),
                        smelter.vesselState()
                            .totalMass(),
                        smelter.vesselState()
                            .fuelState()
                            .isBurning() ? "\u00a7aBurning" : "\u00a77Cold")));

            for (int i = 0; i < smelter.vesselState()
                .layers()
                .size(); i++) {
                AggregateLayer l = smelter.vesselState()
                    .layers()
                    .get(i);
                player.addChatMessage(
                    new ChatComponentText(
                        String.format(
                            "  \u00a77[%d] %s: %s at %.1f K (%s)",
                            i,
                            l.phaseType(),
                            l.mass(),
                            l.temperatureKelvin(),
                            l.physicalPhase())));
            }
            return true;
        }

        // 3. Normal player activation: Open Starter Smelter GUI
        player.openGui(
            naturalexceptions.NaturalExceptions.instance,
            naturalexceptions.minecraft.gui.NEGuiHandler.GUI_STARTER_SMELTER,
            world,
            x,
            y,
            z);
        return true;
    }
}
