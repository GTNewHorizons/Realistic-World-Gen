package rwg.commands;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import rwg.world.RwgWorldSavedData;

public class RwgBiomeListsCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "rwg_biomelists";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rwg_biomelists get|legacy|fixed";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }

        WorldServer world = DimensionManager.getWorld(0);
        if (world == null) {
            throw new CommandException("rwg.biomelists.no_world");
        }

        int current = RwgWorldSavedData.getBiomeListVersion(world);
        int version;
        if (args[0].equals("get")) {
            sender.addChatMessage(new ChatComponentTranslation("rwg.biomelists.current", name(current)));
            return;
        } else if (args[0].equals("legacy")) {
            version = RwgWorldSavedData.BIOME_LIST_LEGACY;
        } else if (args[0].equals("fixed")) {
            version = RwgWorldSavedData.BIOME_LIST_FIXED;
        } else {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }

        if (version == current) {
            sender.addChatMessage(new ChatComponentTranslation("rwg.biomelists.unchanged", name(current)));
            return;
        }

        if (!RwgWorldSavedData.setBiomeListVersion(version)) {
            throw new CommandException("rwg.biomelists.no_world");
        }
        sender.addChatMessage(new ChatComponentTranslation("rwg.biomelists.was_set_to", name(version)));
        sender.addChatMessage(new ChatComponentTranslation("rwg.biomelists.chunk_borders"));
        sender.addChatMessage(new ChatComponentTranslation("rwg.noise.needs_restart"));
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "get", "legacy", "fixed") : null;
    }

    private static String name(int version) {
        return version == RwgWorldSavedData.BIOME_LIST_FIXED ? "fixed" : "legacy";
    }
}
