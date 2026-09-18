package naturalexceptions.minecraft.metallurgy;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/**
 * Block hosting the Starter Smelter vessel.
 */
public final class BlockStarterSmelter extends BlockContainer {

    public BlockStarterSmelter() {
        super(Material.rock);
        setHardness(2.5F);
        setResistance(10.0F);
        setStepSound(soundTypeStone);
        setBlockName("starterSmelter");
        setBlockTextureName("naturalexceptions:starter_smelter_side");
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
        player.addChatMessage(
            new ChatComponentText(
                String.format(
                    "\u00a7e[Smelter Status] Layers: %d | Mass: %s | Fuel: %s",
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
