package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.minecraft.biology.BiologicalEvents;

/** Stone baked by the lava pockets of the deep: dimly glowing and hot enough to burn whoever stands on it. */
public final class BlockMagmaRock extends Block {

    /** Surface temperature and heat delivered to a foot standing on it. */
    static final double SURFACE_KELVIN = 820.0;
    static final double CONTACT_JOULES_PER_SECOND = 30.0;
    private static final ResourceId LEFT_LEG = ResourceId.of("naturalexceptions", "left_leg");

    public BlockMagmaRock() {
        super(Material.rock);
        setBlockName("magmaRock");
        setBlockTextureName("naturalexceptions:magma_rock");
        setHardness(2.5F);
        setResistance(12.0F);
        setLightLevel(0.3F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote || !(entity instanceof EntityLivingBase) || entity.isImmuneToFire()) return;
        BiologicalEvents.applySustainedThermalContact(
            (EntityLivingBase) entity,
            LEFT_LEG,
            SURFACE_KELVIN,
            CONTACT_JOULES_PER_SECOND);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (random.nextInt(4) != 0 || !world.isAirBlock(x, y + 1, z)) return;
        world.spawnParticle("smoke", x + random.nextDouble(), y + 1.02, z + random.nextDouble(), 0.0, 0.03, 0.0);
    }
}
