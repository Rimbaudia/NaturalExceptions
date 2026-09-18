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
        return "/ne_smelter <structure|help>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Accessible to all players
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && "structure".equalsIgnoreCase(args[0])) {
            sender.addChatMessage(
                new ChatComponentText("\u00a76=== Natural Exceptions: 3x3x3 Starter Smelter Structure ==="));
            sender.addChatMessage(
                new ChatComponentText("\u00a7eRequired: 24 Refractory Bricks + 1 Starter Smelter Controller"));
            sender.addChatMessage(
                new ChatComponentText(
                    "\u00a7bLayer 1 (Hearth Base, Y-1): \u00a773x3 solid Refractory Bricks (9 blocks)"));
            sender.addChatMessage(
                new ChatComponentText(
                    "\u00a7bLayer 2 (Crucible Chamber, Y=0): \u00a77Controller at front-center, 7 Refractory Bricks, 1 center air cavity"));
            sender.addChatMessage(
                new ChatComponentText(
                    "\u00a7bLayer 3 (Upper Rim/Flue, Y+1): \u00a778 Refractory Bricks perimeter ring, 1 center chimney air"));
            sender.addChatMessage(
                new ChatComponentText("\u00a7aValidation: Right-click controller to check structure validity."));
            return;
        }

        sender.addChatMessage(new ChatComponentText("\u00a76Natural Exceptions Smelter Commands:"));
        sender.addChatMessage(
            new ChatComponentText("  \u00a7e/ne_smelter structure \u00a77- View layer-by-layer 3x3x3 building guide"));
        sender.addChatMessage(new ChatComponentText("  \u00a7e/ne_smelter help      \u00a77- View command usage"));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "structure", "help");
        }
        return null;
    }
}
