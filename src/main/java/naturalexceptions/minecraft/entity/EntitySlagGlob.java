package naturalexceptions.minecraft.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import naturalexceptions.minecraft.block.BlockMoltenMetal;
import naturalexceptions.minecraft.registry.NEBlocks;

/** A glob of hot slag lobbed by the Foundry Colossus: it burns what it hits, and splashes molten where it lands. */
public final class EntitySlagGlob extends EntityThrowable {

    public static final float DAMAGE = 5.0F;

    public EntitySlagGlob(World world) {
        super(world);
    }

    public EntitySlagGlob(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    @Override
    protected void onImpact(MovingObjectPosition hit) {
        if (worldObj.isRemote) return;
        if (hit.entityHit != null && hit.entityHit != getThrower()) {
            hit.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), DAMAGE);
            hit.entityHit.setFire(3);
        }
        int x = (int) Math.floor(posX), y = (int) Math.floor(posY), z = (int) Math.floor(posZ);
        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && hit.sideHit == 1) {
            x = hit.blockX;
            y = hit.blockY + 1;
            z = hit.blockZ;
        }
        if (NEBlocks.moltenMetal != null && BlockMoltenMetal.canLie(worldObj, x, y, z))
            worldObj.setBlock(x, y, z, NEBlocks.moltenMetal, 0, 3);
        worldObj.playSoundAtEntity(this, "random.fizz", 0.8F, 0.8F);
        setDead();
    }
}
