package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * Skystone: the raw rock of the great sky islands that hold the dungeons. Quarried, it is dressed into Skystone
 * Bricks and the rest (at a crafting table, or one for one at a Mason's Bench); unlike the bricks, the rock itself
 * breaks like any stone.
 */
public final class BlockSkystoneRock extends Block {

    public BlockSkystoneRock() {
        super(Material.rock);
        setBlockName("skystone");
        setBlockTextureName("naturalexceptions:skystone_rock");
        setHardness(1.8F);
        setResistance(10.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 0);
    }
}
