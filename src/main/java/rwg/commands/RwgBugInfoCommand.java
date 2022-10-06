package rwg.commands;

import static net.minecraft.util.EnumChatFormatting.GOLD;
import static net.minecraft.util.EnumChatFormatting.WHITE;

import java.util.Arrays;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class RwgBugInfoCommand extends CommandBase {
    static String[] BUG_INFO_MESSAGE = {
        "A bug in the RWG version shipped with GTNH 2.2.0.0 caused the world to be generated differently than previous versions.",
        "The bug has since been fixed, reverting future generation to pre-2.2.0.0 style.",
        "Unfortunately, what version was used when this world was created can't be automatically detected, so to allow players from both versions to continue playing on their world without making new ugly chunk borders, this version of RWG supports both but requires you to select which one to use when upgrading worlds.",
        GOLD + "/rwg_noise perlin" + WHITE + " is the version used in all versions except 2.2.0.0",
        GOLD + "/rwg_noise opensimplex" + WHITE + " is the version used in 2.2.0.0"
    };

    @Override
    public String getCommandName() {
        return "rwg_buginfo";
    }

    @Override
    public String getCommandUsage(ICommandSender iCommandSender) {
        return "/rwg_buginfo";
    }

    @Override
    public void processCommand(ICommandSender iCommandSender, String[] strings) {
        Arrays.stream(BUG_INFO_MESSAGE).map(ChatComponentText::new).forEach(iCommandSender::addChatMessage);
    }
}
