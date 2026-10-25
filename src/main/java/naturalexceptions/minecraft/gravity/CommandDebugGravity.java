package naturalexceptions.minecraft.gravity;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/**
 * Developer runtime diagnostic command to toggle gravity physics logging.
 *
 * <p>
 * Usage:
 * <ul>
 * <li>{@code /ne_gravity_debug on} - Enables per-tick debug logging for player physics.</li>
 * <li>{@code /ne_gravity_debug off} - Disables per-tick debug logging.</li>
 * <li>{@code /ne_gravity_debug} - Toggles logging state.</li>
 * </ul>
 */
public final class CommandDebugGravity extends CommandBase {

    @Override
    public String getCommandName() {
        return "ne_gravity_debug";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // Operators only: it floods the log
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ne_gravity_debug [on|off]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        NEGravityService service = NEGravityService.global();
        if (args.length > 0) {
            if ("on".equalsIgnoreCase(args[0]) || "true".equalsIgnoreCase(args[0])) {
                service.setDebugLogging(true);
                sender.addChatMessage(new ChatComponentText("§a[Natural Exceptions] Gravity debug logging enabled."));
                return;
            } else if ("off".equalsIgnoreCase(args[0]) || "false".equalsIgnoreCase(args[0])) {
                service.setDebugLogging(false);
                sender.addChatMessage(new ChatComponentText("§e[Natural Exceptions] Gravity debug logging disabled."));
                return;
            }
        }
        boolean newState = !service.isDebugLogging();
        service.setDebugLogging(newState);
        sender.addChatMessage(
            new ChatComponentText(
                "§b[Natural Exceptions] Gravity debug logging is now " + (newState ? "§aON" : "§cOFF")));
    }
}
