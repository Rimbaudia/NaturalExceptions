package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Stormglass in a skystone frame: the light of the sky dungeons, flameless and full. */
public final class BlockStormglassLantern extends Block {

    public BlockStormglassLantern() {
        super(Material.glass);
        setBlockName("stormglassLantern");
        setBlockTextureName("naturalexceptions:stormglass_lantern");
        setHardness(0.8F);
        setLightLevel(1.0F);
        setStepSound(soundTypeGlass);
    }
}
