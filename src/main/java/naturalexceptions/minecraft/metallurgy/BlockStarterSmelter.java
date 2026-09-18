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
import naturalexceptions.core.metallurgy.MaterialQuantity;

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

        // Sneak interaction: extraction
        if (player.isSneaking()) {
            // Attempt to pour liquid metal first
            ItemStack poured = smelter.extractLiquidMetal(MaterialQuantity.ofGrams(100.0));
            if (poured != null) {
                if (!player.inventory.addItemStackToInventory(poured)) {
                    player.dropPlayerItemWithRandomChoice(poured, false);
                }
                player.addChatMessage(new ChatComponentText("\u00a7aExtracted 100g liquid metal sample."));
                return true;
            }

            // Otherwise skim slag
            ItemStack skimmed = smelter.extractSlag(MaterialQuantity.ofGrams(50.0));
            if (skimmed != null) {
                if (!player.inventory.addItemStackToInventory(skimmed)) {
                    player.dropPlayerItemWithRandomChoice(skimmed, false);
                }
                player.addChatMessage(new ChatComponentText("\u00a7eSkimmed 50g slag from surface."));
                return true;
            }

            player.addChatMessage(new ChatComponentText("\u00a77No extractable liquid metal or slag present."));
            return true;
        }

        // Insert fuel
        if (held != null && smelter.insertFuel(held)) {
            player.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "\u00a76Fuel added. Active burn: %d ticks.",
                        smelter.vesselState()
                            .fuelState()
                            .remainingTicks())));
            return true;
        }

        // Insert feed / ore
        if (held != null && smelter.insertFeed(held)) {
            player.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "\u00a76Feed added. Total mass: %s across %d layer(s).",
                        smelter.vesselState()
                            .totalMass(),
                        smelter.vesselState()
                            .layers()
                            .size())));
            return true;
        }

        // Inspect coarse status
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
}
