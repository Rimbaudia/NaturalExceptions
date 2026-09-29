package naturalexceptions.minecraft.block;

import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;

/**
 * A pale-blue flower with long trailing petals that grows wild only on sky islands. Its petals catch the wind:
 * a Glider Cloak needs two, and steeped in water they make Windbloom Tea, which softens falls.
 */
public final class BlockWindbloom extends BlockBush {

    public BlockWindbloom() {
        super(Material.plants);
        setBlockName("windbloom");
        setBlockTextureName("naturalexceptions:windbloom");
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setBlockBounds(0.3F, 0.0F, 0.3F, 0.7F, 0.8F, 0.7F);
    }
}
