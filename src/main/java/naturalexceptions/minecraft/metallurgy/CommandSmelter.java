package naturalexceptions.minecraft.metallurgy;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public final class CommandSmelter extends CommandBase {

    @Override
    public String getCommandName() {
        return "ne_smelter";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ne_smelter <structure|mold|visual|help>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Accessible to all players
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && "structure".equalsIgnoreCase(args[0])) {
            naturalexceptions.minecraft.multiblock.StarterSmelterDefinition def = naturalexceptions.minecraft.multiblock.StarterSmelterDefinition.INSTANCE;
            sender.addChatMessage(new ChatComponentText("\u00a76=== Natural Exceptions: Variable Starter Smelter ==="));
            sender.addChatMessage(
                new ChatComponentText(
                    "\u00a7eStatus: \u00a7c" + def.maturity()
                        .label() + " \u00a77(" + def.maturityNotice() + ")"));
            StringBuilder reqMsg = new StringBuilder("\u00a7eMinimum shell: ");
            boolean first = true;
            for (java.util.Map.Entry<String, Integer> entry : def.getRequiredCountsByName()
                .entrySet()) {
                if (!first) reqMsg.append(" + ");
                reqMsg.append(entry.getValue())
                    .append(" ")
                    .append(entry.getKey());
                first = false;
            }
            sender.addChatMessage(new ChatComponentText(reqMsg.toString()));
            sender.addChatMessage(
                new ChatComponentText(
                    "\u00a7bBounds: outer width/depth 3-7 blocks (odd); cavity height 1-3 blocks. Minimum: 3x3 footprint and one cavity layer."));
            sender.addChatMessage(
                new ChatComponentText(
                    "\u00a7bBuild: solid refractory floor, refractory perimeter walls, clear rectangular cavity, controller at front-center wall."));
            sender.addChatMessage(
                new ChatComponentText(
                    "\u00a7bTop: refractory rim around an open cavity/exhaust. One cavity block provides 25 L usable volume."));
            sender.addChatMessage(
                new ChatComponentText(
                    "\u00a7bTuyere: horizontal wall port, nozzle points inward. Outlet: lower horizontal wall port, spout points outward."));
            sender.addChatMessage(
                new ChatComponentText("\u00a7aValidation: Right-click controller to check structure validity."));
            return;
        }

        if (args.length > 0 && ("mold".equalsIgnoreCase(args[0]) || "visual".equalsIgnoreCase(args[0]))) {
            if (sender instanceof net.minecraft.entity.player.EntityPlayer) {
                net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) sender;
                net.minecraft.item.ItemStack held = player.getHeldItem();
                if (held != null && held.getItem() instanceof naturalexceptions.minecraft.item.ItemCastingMold) {
                    sender.addChatMessage(new ChatComponentText("\u00a76=== Casting Mold Visual Diagnostics ==="));
                    naturalexceptions.core.fabrication.GeometryId geom = naturalexceptions.minecraft.item.ItemCastingMold
                        .getGeometry(held);
                    boolean filled = naturalexceptions.minecraft.item.ItemCastingMold.isFilled(held);
                    sender.addChatMessage(
                        new ChatComponentText("  \u00a77Geometry: \u00a7f" + (geom != null ? geom.id() : "blank")));
                    sender.addChatMessage(new ChatComponentText("  \u00a77Filled: \u00a7f" + filled));
                    if (filled) {
                        naturalexceptions.core.material.MaterialRef ref = naturalexceptions.minecraft.item.ItemCastingMold
                            .getContainedMaterialRef(held);
                        naturalexceptions.core.material.CanonicalMaterialKey key = naturalexceptions.minecraft.item.ItemCastingMold
                            .getContainedStateKey(held);
                        naturalexceptions.core.thermal.ActiveThermalState thermal = naturalexceptions.minecraft.item.ItemCastingMold
                            .getThermalState(held);
                        boolean solid = naturalexceptions.minecraft.item.ItemCastingMold.isSolidified(held);
                        int color = naturalexceptions.minecraft.item.ItemCastingMold.getTintColor(held, 1);
                        sender.addChatMessage(new ChatComponentText("  \u00a77Material: \u00a7f" + ref));
                        sender.addChatMessage(
                            new ChatComponentText(
                                "  \u00a77State Key: \u00a7f" + (key != null ? key.canonicalString() : "null")));
                        sender.addChatMessage(new ChatComponentText("  \u00a77Solidified: \u00a7f" + solid));
                        sender.addChatMessage(
                            new ChatComponentText(
                                String.format(
                                    "  \u00a77Temperature: \u00a7f%.1f K (Melt: %.1f K)",
                                    thermal != null ? thermal.temperatureKelvin() : 0.0,
                                    thermal != null ? thermal.properties()
                                        .meltingPointKelvin() : 0.0)));
                        sender.addChatMessage(
                            new ChatComponentText(
                                String.format("  \u00a77Pass 1 Tint: \u00a76#%06X", color & 0xFFFFFF)));
                    }
                    String diag = naturalexceptions.minecraft.item.ItemCastingMold.getPayloadDiagnostic(held);
                    if (diag != null) {
                        sender.addChatMessage(new ChatComponentText("  \u00a7eDiagnostic: \u00a7c" + diag));
                    } else {
                        sender.addChatMessage(new ChatComponentText("  \u00a7aPayload Status: Clean"));
                    }
                    return;
                } else {
                    sender.addChatMessage(
                        new ChatComponentText("\u00a7cPlease hold a Casting Mold to inspect visual diagnostics."));
                    return;
                }
            } else {
                sender.addChatMessage(
                    new ChatComponentText("\u00a7cThis command must be run by a player holding a mold."));
                return;
            }
        }

        sender.addChatMessage(new ChatComponentText("\u00a76Natural Exceptions Smelter Commands:"));
        sender.addChatMessage(
            new ChatComponentText(
                "  \u00a7e/ne_smelter structure \u00a77- View variable vessel bounds and minimum layout"));
        sender.addChatMessage(
            new ChatComponentText("  \u00a7e/ne_smelter mold      \u00a77- Inspect held mold visual & thermal state"));
        sender.addChatMessage(new ChatComponentText("  \u00a7e/ne_smelter help      \u00a77- View command usage"));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "structure", "mold", "visual", "help");
        }
        return null;
    }
}
