package naturalexceptions.minecraft.item;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.DerivedCapability;
import naturalexceptions.core.assembly.TargetMaterialProfile;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.material.MaterialAppearance;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Generic Minecraft item carrier representing an arbitrary physical tool assembly.
 */
public final class ItemGenericAssembly extends Item {

    @SideOnly(Side.CLIENT)
    private IIcon iconHandle;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandlePickaxe;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandleAxe;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandleShovel;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandleHoe;
    @SideOnly(Side.CLIENT)
    private IIcon iconHandleSword;
    @SideOnly(Side.CLIENT)
    private EnumMap<GeometryId, IIcon> woodenHandleIcons;
    @SideOnly(Side.CLIENT)
    private IIcon iconPickHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconAxeHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconShovelHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconHoeHead;
    @SideOnly(Side.CLIENT)
    private IIcon iconBlade;

    public ItemGenericAssembly() {
        setUnlocalizedName("genericAssembly");
        setTextureName("naturalexceptions:tool_handle");
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        this.itemIcon = reg.registerIcon("naturalexceptions:tool_handle");
        this.iconHandle = this.itemIcon;
        this.iconHandlePickaxe = reg.registerIcon("naturalexceptions:tool_handle_pickaxe");
        this.iconHandleAxe = reg.registerIcon("naturalexceptions:tool_handle_axe");
        this.iconHandleShovel = reg.registerIcon("naturalexceptions:tool_handle_shovel");
        this.iconHandleHoe = reg.registerIcon("naturalexceptions:tool_handle_hoe");
        this.iconHandleSword = reg.registerIcon("naturalexceptions:tool_handle_sword");
        this.woodenHandleIcons = new EnumMap<>(GeometryId.class);
        this.woodenHandleIcons
            .put(GeometryId.PICK_HEAD, reg.registerIcon("naturalexceptions:tool_handle_pickaxe_wood"));
        this.woodenHandleIcons.put(GeometryId.AXE_HEAD, reg.registerIcon("naturalexceptions:tool_handle_axe_wood"));
        this.woodenHandleIcons
            .put(GeometryId.SHOVEL_HEAD, reg.registerIcon("naturalexceptions:tool_handle_shovel_wood"));
        this.woodenHandleIcons.put(GeometryId.HOE_HEAD, reg.registerIcon("naturalexceptions:tool_handle_hoe_wood"));
        this.woodenHandleIcons.put(GeometryId.BLADE, reg.registerIcon("naturalexceptions:tool_handle_sword_wood"));
        this.iconPickHead = reg.registerIcon("naturalexceptions:tool_pickaxe_head");
        this.iconAxeHead = reg.registerIcon("naturalexceptions:tool_axe_head");
        this.iconShovelHead = reg.registerIcon("naturalexceptions:tool_shovel_head");
        this.iconHoeHead = reg.registerIcon("naturalexceptions:tool_hoe_head");
        this.iconBlade = reg.registerIcon("naturalexceptions:tool_blade");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    public int getRenderPasses(int metadata) {
        return 2;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int pass) {
        AssemblyGraph assembly = getAssembly(stack);
        GeometryId geom = (assembly != null && assembly.head() != null
            && assembly.head()
                .geometryId() != null) ? assembly.head()
                    .geometryId() : BlankGeometry.get(stack);
        if (geom == null) geom = GeometryId.PICK_HEAD;

        if (pass == 0) {
            if (hasWoodenHandle(assembly) && BlankGeometry.get(stack) == null && woodenHandleIcons != null) {
                IIcon woodenHandle = woodenHandleIcons.get(geom);
                if (woodenHandle != null) return woodenHandle;
            }
            switch (geom) {
                case AXE_HEAD:
                    return iconHandleAxe != null ? iconHandleAxe : (iconHandle != null ? iconHandle : itemIcon);
                case SHOVEL_HEAD:
                    return iconHandleShovel != null ? iconHandleShovel : (iconHandle != null ? iconHandle : itemIcon);
                case HOE_HEAD:
                    return iconHandleHoe != null ? iconHandleHoe : (iconHandle != null ? iconHandle : itemIcon);
                case BLADE:
                    return iconHandleSword != null ? iconHandleSword : (iconHandle != null ? iconHandle : itemIcon);
                case PICK_HEAD:
                default:
                    return iconHandlePickaxe != null ? iconHandlePickaxe : (iconHandle != null ? iconHandle : itemIcon);
            }
        }

        switch (geom) {
            case PICK_HEAD:
                return iconPickHead != null ? iconPickHead : itemIcon;
            case AXE_HEAD:
                return iconAxeHead != null ? iconAxeHead : itemIcon;
            case SHOVEL_HEAD:
                return iconShovelHead != null ? iconShovelHead : itemIcon;
            case HOE_HEAD:
                return iconHoeHead != null ? iconHoeHead : itemIcon;
            case BLADE:
                return iconBlade != null ? iconBlade : itemIcon;
            default:
                return iconPickHead != null ? iconPickHead : itemIcon;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        return getIcon(stack, 1);
    }

    public static ItemStack createStack(AssemblyGraph assembly) {
        Item item = NEItems.genericAssembly != null ? NEItems.genericAssembly : new ItemGenericAssembly();
        ItemStack stack = new ItemStack(item);
        NBTTagCompound nbt = new NBTTagCompound();
        AssemblyNbtCodec.writeToNbt(assembly, nbt);
        stack.setTagCompound(nbt);
        return stack;
    }

    public static AssemblyGraph getAssembly(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        try {
            return AssemblyNbtCodec.readFromNbt(stack.getTagCompound());
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveAssembly(ItemStack stack, AssemblyGraph assembly) {
        if (stack == null) return;
        NBTTagCompound nbt = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        AssemblyNbtCodec.writeToNbt(assembly, nbt);
        stack.setTagCompound(nbt);
    }

    public static DerivedCapability getCapability(AssemblyGraph assembly) {
        if (assembly == null) {
            return null;
        }
        MaterialProperties headProps = null;
        if (assembly.head() != null && assembly.head()
            .materialRef() != null) {
            naturalexceptions.core.material.MaterialDefinition def = naturalexceptions.content.BuiltinContent
                .materials()
                .getByRef(
                    assembly.head()
                        .materialRef());
            if (def != null) {
                headProps = def.properties();
            }
        }
        if (headProps == null) {
            MaterialPropertyResolver resolver = new MaterialPropertyResolver();
            headProps = resolver.resolve(
                assembly.head()
                    .materialRef(),
                assembly.head()
                    .stateKey()
                    .composition())
                .properties();
        }
        MaterialProperties handleProps = BuiltinMaterials.WOOD_PROPERTIES;
        if (assembly.handle() != null && !BuiltinMaterials.WOOD.ref()
            .equals(
                assembly.handle()
                    .materialRef())) {
            naturalexceptions.core.material.MaterialDefinition def = naturalexceptions.content.BuiltinContent
                .materials()
                .getByRef(
                    assembly.handle()
                        .materialRef());
            if (def != null) {
                handleProps = def.properties();
            } else {
                MaterialPropertyResolver resolver = new MaterialPropertyResolver();
                handleProps = resolver.resolve(
                    assembly.handle()
                        .materialRef(),
                    assembly.handle()
                        .stateKey()
                        .composition())
                    .properties();
            }
        }
        return new DerivedCapability(assembly, headProps, handleProps);
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.isBroken() || assembly.head() == null) {
            return 1.0F;
        }

        DerivedCapability capability = getCapability(assembly);
        if (capability == null) {
            return 1.0F;
        }
        GeometryId geom = assembly.head()
            .geometryId();
        net.minecraft.block.material.Material mat = block != null ? block.getMaterial() : null;

        boolean effective = false;
        if (geom == GeometryId.PICK_HEAD) {
            effective = (mat == net.minecraft.block.material.Material.rock
                || mat == net.minecraft.block.material.Material.iron
                || mat == net.minecraft.block.material.Material.anvil);
        } else if (geom == GeometryId.AXE_HEAD) {
            effective = (mat == net.minecraft.block.material.Material.wood
                || mat == net.minecraft.block.material.Material.plants
                || mat == net.minecraft.block.material.Material.vine);
        } else if (geom == GeometryId.SHOVEL_HEAD) {
            effective = (mat == net.minecraft.block.material.Material.ground
                || mat == net.minecraft.block.material.Material.grass
                || mat == net.minecraft.block.material.Material.sand
                || mat == net.minecraft.block.material.Material.snow
                || mat == net.minecraft.block.material.Material.craftedSnow
                || mat == net.minecraft.block.material.Material.clay);
        } else if (geom == GeometryId.HOE_HEAD) {
            effective = (mat == net.minecraft.block.material.Material.plants
                || mat == net.minecraft.block.material.Material.leaves
                || mat == net.minecraft.block.material.Material.vine);
        } else if (geom == GeometryId.BLADE) {
            effective = (mat == net.minecraft.block.material.Material.web);
        }

        TargetMaterialProfile target = mapBlockToTarget(block);
        if (effective) {
            return (float) capability.miningSpeed(target);
        }
        return 1.0F;
    }

    @Override
    public boolean canHarvestBlock(Block block, ItemStack stack) {
        if (block == null) {
            return false;
        }
        if (block.getMaterial() != null && block.getMaterial()
            .isToolNotRequired()) {
            return true;
        }
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.isBroken() || assembly.head() == null) {
            return false;
        }
        DerivedCapability capability = getCapability(assembly);
        if (capability == null) {
            return false;
        }
        GeometryId geom = assembly.head()
            .geometryId();
        net.minecraft.block.material.Material mat = block.getMaterial();

        if (geom == GeometryId.PICK_HEAD) {
            if (mat == net.minecraft.block.material.Material.rock || mat == net.minecraft.block.material.Material.iron
                || mat == net.minecraft.block.material.Material.anvil) {
                TargetMaterialProfile target = mapBlockToTarget(block);
                return capability.penetration() >= target.hardness() * 0.6;
            }
        } else if (geom == GeometryId.AXE_HEAD) {
            if (mat == net.minecraft.block.material.Material.wood || mat == net.minecraft.block.material.Material.plants
                || mat == net.minecraft.block.material.Material.vine) {
                return true;
            }
        } else if (geom == GeometryId.SHOVEL_HEAD) {
            if (mat == net.minecraft.block.material.Material.ground
                || mat == net.minecraft.block.material.Material.grass
                || mat == net.minecraft.block.material.Material.sand
                || mat == net.minecraft.block.material.Material.snow
                || mat == net.minecraft.block.material.Material.craftedSnow
                || mat == net.minecraft.block.material.Material.clay) {
                return true;
            }
        } else if (geom == GeometryId.HOE_HEAD) {
            if (mat == net.minecraft.block.material.Material.plants
                || mat == net.minecraft.block.material.Material.leaves
                || mat == net.minecraft.block.material.Material.vine) {
                return true;
            }
        }
        TargetMaterialProfile target = mapBlockToTarget(block);
        return capability.penetration() >= target.hardness() * 0.6;
    }

    @Override
    public boolean func_150897_b(Block block) {
        if (block == null) return false;
        return block.getMaterial() != null && block.getMaterial()
            .isToolNotRequired();
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.isBroken() || assembly.head() == null) {
            return -1;
        }
        GeometryId geom = assembly.head()
            .geometryId();
        DerivedCapability cap = getCapability(assembly);
        if (cap == null) return -1;
        double p = cap.penetration();

        int level;
        if (p >= 6.0) level = 3; // Diamond-equivalent
        else if (p >= 4.0) level = 2; // Iron-equivalent
        else if (p >= 2.0) level = 1; // Stone-equivalent
        else level = 0; // Wood/Gold equivalent

        if ("pickaxe".equals(toolClass) && geom == GeometryId.PICK_HEAD) {
            return level;
        }
        if ("axe".equals(toolClass) && geom == GeometryId.AXE_HEAD) {
            return level;
        }
        if ("shovel".equals(toolClass) && geom == GeometryId.SHOVEL_HEAD) {
            return level;
        }
        if ("hoe".equals(toolClass) && geom == GeometryId.HOE_HEAD) {
            return level;
        }
        if ("sword".equals(toolClass) && geom == GeometryId.BLADE) {
            return 1;
        }
        return -1;
    }

    @Override
    public Set<String> getToolClasses(ItemStack stack) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.head() == null) {
            return Collections.emptySet();
        }
        GeometryId geom = assembly.head()
            .geometryId();
        if (geom == GeometryId.PICK_HEAD) {
            return Collections.singleton("pickaxe");
        }
        if (geom == GeometryId.AXE_HEAD) {
            return Collections.singleton("axe");
        }
        if (geom == GeometryId.SHOVEL_HEAD) {
            return Collections.singleton("shovel");
        }
        if (geom == GeometryId.HOE_HEAD) {
            return Collections.singleton("hoe");
        }
        if (geom == GeometryId.BLADE) {
            return Collections.singleton("sword");
        }
        return Collections.emptySet();
    }

    private static boolean hasWoodenHandle(AssemblyGraph assembly) {
        return assembly == null || assembly.handle() == null
            || BuiltinMaterials.WOOD.ref()
                .equals(
                    assembly.handle()
                        .materialRef());
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        AssemblyGraph assembly = getAssembly(stack);
        if (pass == 0) {
            if (hasWoodenHandle(assembly)) return 0xFFFFFF;
            if (assembly != null && assembly.handle() != null) {
                return MaterialAppearance.getSolidColor(
                    assembly.handle()
                        .materialRef(),
                    assembly.handle()
                        .stateKey());
            }
            return 0x8B5A2B;
        }
        if (assembly != null && assembly.head() != null) {
            MaterialRef ref = assembly.head()
                .materialRef();
            if (ref != null && ("iron".equalsIgnoreCase(
                ref.authoredId() != null ? ref.authoredId()
                    .path() : "")
                || "fe".equalsIgnoreCase(
                    ref.authoredId() != null ? ref.authoredId()
                        .path() : ""))) {
                return 0xFFFFFF;
            }
            return MaterialAppearance.getSolidColor(
                assembly.head()
                    .materialRef(),
                assembly.head()
                    .stateKey());
        }
        return 0xFFFFFF;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z,
        EntityLivingBase player) {
        if (world != null && world.isRemote) {
            return true;
        }

        if (block != null && block.getBlockHardness(world, x, y, z) == 0.0F) {
            return true;
        }

        AssemblyGraph assembly = getAssembly(stack);
        if (assembly != null && !assembly.isBroken()) {
            DerivedCapability capability = getCapability(assembly);
            if (capability != null) {
                TargetMaterialProfile target = mapBlockToTarget(block);
                capability.interactWith(target);
                saveAssembly(stack, assembly);
                checkToolBreak(stack, world, player);
            }
        }

        return true;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (MaterialNamingTarget.openIfSneaking(stack, world, player)) {
            return true;
        }
        return super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (MaterialNamingTarget.openIfSneaking(stack, world, player)) {
            return true;
        }
        if (!player.canPlayerEdit(x, y, z, side, stack)) {
            return false;
        }
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.isBroken() || assembly.head() == null) {
            return false;
        }
        if (assembly.head()
            .geometryId() == GeometryId.HOE_HEAD) {
            if (side != 0 && world.getBlock(x, y + 1, z)
                .isAir(world, x, y + 1, z)) {
                Block block = world.getBlock(x, y, z);
                if (block == Blocks.grass || block == Blocks.dirt) {
                    Block farmland = Blocks.farmland;
                    world.playSoundEffect(
                        x + 0.5F,
                        y + 0.5F,
                        z + 0.5F,
                        farmland.stepSound.getStepResourcePath(),
                        (farmland.stepSound.getVolume() + 1.0F) / 2.0F,
                        farmland.stepSound.getPitch() * 0.8F);
                    if (world.isRemote) {
                        return true;
                    }
                    world.setBlock(x, y, z, farmland);
                    DerivedCapability capability = getCapability(assembly);
                    if (capability != null) {
                        capability.interactWith(TargetMaterialProfile.dirt());
                        saveAssembly(stack, assembly);
                        checkToolBreak(stack, world, player);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static TargetMaterialProfile mapBlockToTarget(Block block) {
        if (block == null) {
            return TargetMaterialProfile.stone();
        }
        if (block == NEBlocks.copperOre) {
            return TargetMaterialProfile.copperOre();
        }
        if (block.getMaterial() != null) {
            net.minecraft.block.material.Material mat = block.getMaterial();
            if (mat == net.minecraft.block.material.Material.wood) {
                return TargetMaterialProfile.wood();
            }
            if (mat == net.minecraft.block.material.Material.ground
                || mat == net.minecraft.block.material.Material.grass
                || mat == net.minecraft.block.material.Material.sand
                || mat == net.minecraft.block.material.Material.snow
                || mat == net.minecraft.block.material.Material.craftedSnow
                || mat == net.minecraft.block.material.Material.clay) {
                return TargetMaterialProfile.dirt();
            }
        }
        return TargetMaterialProfile.stone();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null) {
            list.add("\u00a77Empty assembly");
            return;
        }

        String headMat = naturalexceptions.minecraft.knowledge.KnowledgePresentation.materialName(
            player,
            assembly.head()
                .materialRef(),
            assembly.head()
                .stateKey());
        String handleMat = naturalexceptions.minecraft.knowledge.KnowledgePresentation.materialName(
            player,
            assembly.handle()
                .materialRef(),
            assembly.handle()
                .stateKey());

        list.add("  \u00a77Head: \u00a7f" + headMat);
        list.add("  \u00a77Handle: \u00a7f" + handleMat);
        if (MaterialNamingTarget.canOpenNaming(stack)) {
            list.add("\u00a78Sneak-right-click to name generated material");
        }

        if (assembly.isBroken()) {
            list.add("  \u00a77Condition: \u00a7cBroken");
        } else {
            double wear = assembly.head()
                .damage()
                .wear();
            double deform = assembly.head()
                .damage()
                .deformation();
            double cracks = assembly.head()
                .damage()
                .cracking();
            String cond;
            if (cracks > 0.3 || wear > 0.8) {
                cond = "\u00a7cDamaged";
            } else if (wear > 0.4 || deform > 0.3) {
                cond = "\u00a76Worn";
            } else if (wear > 0.05) {
                cond = "\u00a7eGood";
            } else {
                cond = "\u00a7aPristine";
            }
            list.add("  \u00a77Condition: " + cond);
        }

        boolean showShift = false;
        try {
            if (org.lwjgl.input.Keyboard.isCreated()) {
                showShift = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                    || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
            }
        } catch (Throwable ignored) {}

        if (advanced) {
            list.add("\u00a78--- Developer diagnostics ---");
            list.add("  \u00a78Total Mass: " + assembly.totalMass());
            if (!assembly.isBroken()) {
                double wear = assembly.head()
                    .damage()
                    .wear() * 100.0;
                double deform = assembly.head()
                    .damage()
                    .deformation() * 100.0;
                double cracks = assembly.head()
                    .damage()
                    .cracking() * 100.0;
                list.add(
                    String
                        .format("  \u00a78Strain: Wear %.1f%% | Deform %.1f%% | Cracks %.1f%%", wear, deform, cracks));
            }
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null) {
            GeometryId blank = BlankGeometry.get(stack);
            return blank == null ? "Tool Assembly" : "Blank " + formatGeometryName(blank);
        }
        return naturalexceptions.core.material.MaterialNameResolver.formatToolGeometry(
            assembly.head()
                .geometryId());
    }

    @Override
    public com.google.common.collect.Multimap getAttributeModifiers(ItemStack stack) {
        com.google.common.collect.Multimap multimap = com.google.common.collect.HashMultimap.create();
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly != null && !assembly.isBroken() && assembly.head() != null) {
            DerivedCapability capability = getCapability(assembly);
            if (capability != null) {
                GeometryId geom = assembly.head()
                    .geometryId();
                double baseDamage = 1.0;
                if (geom == GeometryId.BLADE) {
                    baseDamage = 4.0 + capability.penetration() * 0.8;
                } else if (geom == GeometryId.AXE_HEAD) {
                    baseDamage = 3.0 + capability.penetration() * 0.6 + capability.impact() * 0.3;
                } else if (geom == GeometryId.PICK_HEAD) {
                    baseDamage = 2.0 + capability.penetration() * 0.4;
                } else if (geom == GeometryId.SHOVEL_HEAD) {
                    baseDamage = 1.5 + capability.penetration() * 0.3;
                } else if (geom == GeometryId.HOE_HEAD) {
                    baseDamage = 1.0 + capability.penetration() * 0.2;
                }
                multimap.put(
                    net.minecraft.entity.SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
                    new net.minecraft.entity.ai.attributes.AttributeModifier(
                        field_111210_e,
                        "Weapon modifier",
                        baseDamage,
                        0));
            }
        }
        return multimap;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly != null && !assembly.isBroken()) {
            DerivedCapability capability = getCapability(assembly);
            if (capability != null) {
                TargetMaterialProfile targetMat = TargetMaterialProfile.stone();
                capability.interactWith(targetMat);
                saveAssembly(stack, assembly);
                World world = attacker != null ? attacker.worldObj : null;
                checkToolBreak(stack, world, attacker);
            }
        }
        return true;
    }

    public static boolean checkToolBreak(ItemStack stack, World world, EntityLivingBase entity) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null) {
            return false;
        }
        if (assembly.isBroken()) {
            if (assembly.head() != null && !assembly.head()
                .damage()
                .isFractured()) {
                assembly.head()
                    .damage()
                    .fracture();
            }
            saveAssembly(stack, assembly);

            if (entity != null) {
                entity.renderBrokenItemStack(stack);
            } else if (world != null) {
                world.playSoundEffect(0, 0, 0, "random.break", 0.8F, 0.8F);
            }

            // The tool falls apart; its parts (the fractured head included) are dropped, never deleted.
            if (entity != null && !entity.worldObj.isRemote) {
                ItemStack[] parts = naturalexceptions.minecraft.fabrication.ToolAssemblyCarrier.disassemble(stack);
                if (parts != null) {
                    for (ItemStack part : parts) entity.entityDropItem(part, 0.5F);
                }
            }
            stack.stackSize--;
            if (stack.stackSize < 0) {
                stack.stackSize = 0;
            }

            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (stack.stackSize <= 0 && player.getCurrentEquippedItem() == stack) {
                    player.destroyCurrentEquippedItem();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly != null && assembly.head() != null) {
            DerivedCapability cap = getCapability(assembly);
            if (cap != null) {
                double rate = cap.calculateWearRate(TargetMaterialProfile.stone());
                if (rate > 0.0) {
                    return (int) Math.round(1.0 / rate);
                }
            }
        }
        return 250;
    }

    @Override
    public int getDamage(ItemStack stack) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly != null && assembly.head() != null) {
            int max = getMaxDamage(stack);
            double wear = assembly.head()
                .damage()
                .wear();
            return (int) Math.min(max, Math.round(wear * max));
        }
        return 0;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.head() == null) {
            return false;
        }
        return assembly.head()
            .damage()
            .isDamaged();
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.head() == null) {
            return 0.0;
        }
        ComponentDamage d = assembly.head()
            .damage();
        return Math.min(1.0, Math.max(d.wear(), Math.max(d.deformation(), d.cracking())));
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        AssemblyGraph assembly = getAssembly(stack);
        if (assembly == null || assembly.head() == null) {
            return false;
        }
        return assembly.head()
            .damage()
            .isDamaged();
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, net.minecraft.creativetab.CreativeTabs tab, List list) {
        for (GeometryId geometry : new GeometryId[] { GeometryId.PICK_HEAD, GeometryId.AXE_HEAD, GeometryId.SHOVEL_HEAD,
            GeometryId.HOE_HEAD, GeometryId.BLADE }) {
            list.add(BlankGeometry.create(item, geometry));
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (MaterialNamingTarget.openIfSneaking(stack, world, player)) return stack;
        return super.onItemRightClick(stack, world, player);
    }

    private static String formatMaterialName(MaterialRef ref) {
        return naturalexceptions.core.material.MaterialNameResolver.resolveMaterialName(ref);
    }

    private static String formatGeometryName(naturalexceptions.core.fabrication.GeometryId id) {
        return naturalexceptions.core.material.MaterialNameResolver.formatToolGeometry(id);
    }
}
