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
        if (!world.isRemote && stack != null
            && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(TileEntityStarterSmelter.CONTENTS_TAG, 10)) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityStarterSmelter) {
                ((TileEntityStarterSmelter) te).readContentsTag(
                    stack.getTagCompound()
                        .getCompoundTag(TileEntityStarterSmelter.CONTENTS_TAG));
                world.markBlockForUpdate(x, y, z);
            }
        }
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

        // -1. Blacksmith Tongs interaction: heat held workpiece in smelter hearth
        if (held != null && held.getItem() instanceof naturalexceptions.minecraft.item.ItemBlacksmithTongs) {
            return held.getItem()
                .onItemUse(held, player, world, x, y, z, side, hitX, hitY, hitZ);
        }

        // 0. Direct ladle interaction: scoop molten metal or return metal to smelter
        if (held != null && held.getItem() instanceof naturalexceptions.minecraft.item.ItemFoundryLadle) {
            if (player.isSneaking() && naturalexceptions.minecraft.item.ItemFoundryLadle.hasMetal(held)) {
                // Return molten metal from ladle back to crucible
                naturalexceptions.core.metallurgy.MaterialQuantity mass = naturalexceptions.minecraft.item.ItemFoundryLadle
                    .getFilledMass(held);
                naturalexceptions.core.metallurgy.AggregateLayer drained = naturalexceptions.minecraft.item.ItemFoundryLadle
                    .drainMetal(held, mass);
                if (drained != null) {
                    smelter.vesselState()
                        .remelt(drained);
                    smelter.markDirty();
                    world.markBlockForUpdate(x, y, z);
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
                    player.addChatMessage(
                        new ChatComponentText("\u00a7aPoured " + mass + " of molten metal back into Starter Smelter."));
                    return true;
                }
            } else {
                // Scoop molten metal from smelter into ladle
                if (smelter.pourIntoLadle(held)) {
                    player.inventoryContainer.detectAndSendChanges();
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
                    naturalexceptions.core.metallurgy.MaterialQuantity mass = naturalexceptions.minecraft.item.ItemFoundryLadle
                        .getFilledMass(held);
                    player.addChatMessage(
                        new ChatComponentText("\u00a7aScooped molten metal into Foundry Ladle (" + mass + ")."));
                    return true;
                } else {
                    player.addChatMessage(
                        new ChatComponentText("\u00a7cNo accessible molten metal in smelter to scoop into ladle."));
                    return true;
                }
            }
        }

        // Direct fuel feeding from hand (sneak right-click feeds up to 16, normal feeds 1)
        if (held != null && TileEntityStarterSmelter.getFuelBurnTime(held) > 0) {
            int burnTimePerItem = TileEntityStarterSmelter.getFuelBurnTime(held);
            double flameTemp = TileEntityStarterSmelter.getFuelFlameTemp(held);
            String heatLabel = TileEntityStarterSmelter.getFuelHeatLabel(held);
            int countToFeed = player.isSneaking() ? Math.min(held.stackSize, 16) : 1;
            int totalBurnAdded = burnTimePerItem * countToFeed;

            smelter.vesselState()
                .addFuel(totalBurnAdded, flameTemp, heatLabel);

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
                        "\u00a76[Starter Smelter] Fed %dx fuel to fire (%s). (Fire duration: %d ticks / %.0fs)",
                        countToFeed,
                        heatLabel,
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
        } else if (held != null) {
            String rejection = naturalexceptions.core.material.VanillaResourceClassification.getRejectionReason(held);
            if (rejection != null) {
                player.addChatMessage(new ChatComponentText("\u00a7c[Starter Smelter] " + rejection));
                return true;
            }
        }

        // 2b. Hearth Workpiece bare-hand retrieval / scalding burn guard
        if (held == null && smelter.getHearthWorkpiece() != null && !player.isSneaking()) {
            ItemStack hw = smelter.getHearthWorkpiece();
            naturalexceptions.core.thermal.ActiveThermalState thermal = naturalexceptions.minecraft.item.ItemGenericComponent
                .getThermalState(hw);
            double tempK = thermal != null ? thermal.temperatureKelvin() : 293.15;
            if (tempK > 373.15) {
                player.attackEntityFrom(net.minecraft.util.DamageSource.onFire, 1.0F);
                world.playSoundEffect(player.posX, player.posY, player.posZ, "random.fizz", 0.7F, 1.2F);
                player.addChatMessage(
                    new ChatComponentText(
                        String.format(
                            "\u00a7c[Starter Smelter] Ouch! The metal in the hearth is scalding hot (%.0f K / %.0f \u00b0C)! Use blacksmith tongs to handle it.",
                            tempK,
                            tempK - 273.15)));
                return true;
            } else {
                smelter.setHearthWorkpiece(null);
                if (!player.inventory.addItemStackToInventory(hw)) {
                    player.dropPlayerItemWithRandomChoice(hw, false);
                }
                player.inventoryContainer.detectAndSendChanges();
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.stone", 1.0F, 0.8F);
                player.addChatMessage(
                    new ChatComponentText("\u00a7eRetrieved \u00a7f" + hw.getDisplayName() + " \u00a7efrom hearth."));
                return true;
            }
        }

        // 2c. Developer/debug inspect: sneak right-click outputs coarse status to chat
        if (player.isSneaking()) {
            if (smelter.getHearthWorkpiece() != null) {
                ItemStack hw = smelter.getHearthWorkpiece();
                naturalexceptions.core.thermal.ActiveThermalState thermal = naturalexceptions.minecraft.item.ItemGenericComponent
                    .getThermalState(hw);
                double tempK = thermal != null ? thermal.temperatureKelvin() : 293.15;
                player.addChatMessage(
                    new ChatComponentText(
                        String.format(
                            "\u00a7e[Hearth Workpiece] \u00a7f%s \u00a77(T: %.0f K / %.0f \u00b0C)",
                            hw.getDisplayName(),
                            tempK,
                            tempK - 273.15)));
            }

            smelter.validateStructure();
            if (!smelter.isStructureValid()) {
                String failure = smelter.getStructureValidationResult() != null
                    && smelter.getStructureValidationResult()
                        .failureReason() != null ? smelter.getStructureValidationResult()
                            .failureReason() : "Requires complete 3x3x3 refractory structure";
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7c[Starter Smelter] Structure incomplete or outdated — open Knowledge Browser for current layout. ("
                            + failure
                            + ")"));
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

    /**
     * The controller is always dropped from {@link #breakBlock} so that contents can travel with it; vanilla's
     * chance-based drop path (explosions) must not also drop, or lose, a copy.
     */
    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityStarterSmelter && player != null && player.capabilities.isCreativeMode) {
            ((TileEntityStarterSmelter) te).markCreativeRemoval();
        }
        super.onBlockHarvested(world, x, y, z, meta, player);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!world.isRemote && te instanceof TileEntityStarterSmelter) {
            ItemStack drop = createDrop((TileEntityStarterSmelter) te);
            if (drop != null) {
                net.minecraft.entity.item.EntityItem entityItem = new net.minecraft.entity.item.EntityItem(
                    world,
                    x + 0.5D,
                    y + 0.5D,
                    z + 0.5D,
                    drop);
                entityItem.delayBeforeCanPickup = 10;
                world.spawnEntityInWorld(entityItem);
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    /**
     * Builds the controller drop. A controller holding any fire, charge, melt, slag, hearth workpiece, or slot item
     * carries all of it (including thermal energy) in the stack; nothing is silently deleted. An empty controller
     * broken in creative mode drops nothing, matching vanilla creative removal.
     */
    public static ItemStack createDrop(TileEntityStarterSmelter smelter) {
        boolean contents = smelter.hasPreservableContents();
        if (!contents && smelter.isCreativeRemoval()) return null;
        ItemStack drop = new ItemStack(naturalexceptions.minecraft.registry.NEBlocks.starterSmelter);
        if (contents) {
            net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
            tag.setTag(TileEntityStarterSmelter.CONTENTS_TAG, smelter.writeContentsTag());
            drop.setTagCompound(tag);
        }
        return drop;
    }
}
