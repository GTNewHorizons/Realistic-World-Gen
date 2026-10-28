package rwg2.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

/** Changes the executing player's walking and flying speeds by the same multiplier. */
public class SpeedCommand extends CommandBase {

    private static final float BASE_WALK_SPEED = 0.1f;
    private static final float BASE_FLY_SPEED = 0.05f;

    @Override
    public String getCommandName() {
        return "speed";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/speed <multiplier 0-10>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        double multiplier = parseDoubleBounded(sender, args[0], 0.0, 10.0);
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        // The public setters are client-only in 1.7.10. Round-trip through NBT so this remains safe on a dedicated
        // server.
        NBTTagCompound playerData = new NBTTagCompound();
        player.capabilities.writeCapabilitiesToNBT(playerData);
        NBTTagCompound abilities = playerData.getCompoundTag("abilities");
        abilities.setFloat("walkSpeed", (float) multiplier * BASE_WALK_SPEED);
        abilities.setFloat("flySpeed", (float) multiplier * BASE_FLY_SPEED);
        player.capabilities.readCapabilitiesFromNBT(playerData);
        player.sendPlayerAbilities();

        sender.addChatMessage(new ChatComponentText("Walk and fly speed set to " + multiplier + "x."));
    }
}
