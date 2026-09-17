package naturalexceptions.minecraft.gravity;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.core.gravity.GravitySourceId;
import naturalexceptions.core.gravity.RadialGravitySource;
import naturalexceptions.core.gravity.Vector3d;

/**
 * In-game debug command to create a spherical radial gravity test body in the sky
 * and enable the radial gravity field for circumnavigation acceptance verification.
 *
 * <p>
 * Usage:
 * <ul>
 * <li>{@code /ne_gravity_body} - Generates a 9x9x9 stone sphere (R=4) above player with g=0.08 radial gravity.</li>
 * <li>{@code /ne_gravity_body <radius> <g>} - Custom radius and gravity strength.</li>
 * <li>{@code /ne_gravity_body reset} - Restores standard downward Minecraft gravity.</li>
 * </ul>
 */
public final class CommandDebugGravityBody extends CommandBase {

    @Override
    public String getCommandName() {
        return "ne_gravity_body";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Accessible to all players in creative / single-player dev
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ne_gravity_body [radius] [gravity] | /ne_gravity_body reset";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && "reset".equalsIgnoreCase(args[0])) {
            NEGravityService.global()
                .resetToDefault();
            sender.addChatMessage(
                new ChatComponentText("§a[Natural Exceptions] Gravity reset to standard Minecraft downward gravity."));
            return;
        }

        int radius = 4;
        double surfaceG = 0.08;

        if (args.length > 0) {
            try {
                radius = Math.max(2, Math.min(16, Integer.parseInt(args[0])));
            } catch (NumberFormatException ignored) {}
        }
        if (args.length > 1) {
            try {
                surfaceG = Math.max(0.01, Math.min(0.5, Double.parseDouble(args[1])));
            } catch (NumberFormatException ignored) {}
        }

        World world = sender.getEntityWorld();
        if (world == null) {
            sender.addChatMessage(new ChatComponentText("§c[Natural Exceptions] World not available."));
            return;
        }

        int cx, cy, cz;
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            cx = (int) Math.floor(player.posX);
            cy = Math.min(220, (int) Math.floor(player.posY) + radius + 5);
            cz = (int) Math.floor(player.posZ);
        } else {
            cx = 0;
            cy = 100;
            cz = 0;
        }

        // Generate spherical stone body and clear air cavity around it
        int clearRadius = radius + 4;
        for (int dx = -clearRadius; dx <= clearRadius; dx++) {
            for (int dy = -clearRadius; dy <= clearRadius; dy++) {
                for (int dz = -clearRadius; dz <= clearRadius; dz++) {
                    double distSq = dx * dx + dy * dy + dz * dz;
                    int bx = cx + dx;
                    int by = cy + dy;
                    int bz = cz + dz;
                    if (by < 1 || by > 254) continue;

                    if (distSq <= (radius + 0.5) * (radius + 0.5)) {
                        world.setBlock(bx, by, bz, Blocks.stone);
                    } else if (distSq <= (clearRadius + 0.5) * (clearRadius + 0.5)) {
                        world.setBlockToAir(bx, by, bz);
                    }
                }
            }
        }

        // Configure radial gravity field
        Vector3d center = new Vector3d(cx + 0.5, cy + 0.5, cz + 0.5);
        RadialGravitySource field = new RadialGravitySource(
            center,
            surfaceG,
            radius,
            radius + 20.0,
            GravitySourceId.of("naturalexceptions", "circumnavigation_body"));
        NEGravityService.global()
            .setActiveField(field);

        // Place player on top of the sphere
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            player.setPositionAndUpdate(cx + 0.5, cy + radius + 1.2, cz + 0.5);
            player.motionX = 0;
            player.motionY = 0;
            player.motionZ = 0;
        }

        sender.addChatMessage(
            new ChatComponentText(
                String.format(
                    "§a[Natural Exceptions] Created radial body at (%d, %d, %d), R=%d, g=%.3f",
                    cx,
                    cy,
                    cz,
                    radius,
                    surfaceG)));
        sender.addChatMessage(new ChatComponentText("§eCircumnavigation test instructions:"));
        sender.addChatMessage(new ChatComponentText("§71. Stand normally on the top (+Y face, local down = -Y)."));
        sender.addChatMessage(
            new ChatComponentText("§72. Walk forward over the edge onto side wall (+X face, local down = -X)."));
        sender.addChatMessage(new ChatComponentText("§73. Walk under the bottom (-Y face, local down = +Y ceiling)."));
        sender.addChatMessage(
            new ChatComponentText("§74. Walk up opposite wall (-X face) and return to the starting top region."));
        sender.addChatMessage(
            new ChatComponentText("§75. Run '/ne_gravity_body reset' when done to restore standard gravity."));
    }
}
