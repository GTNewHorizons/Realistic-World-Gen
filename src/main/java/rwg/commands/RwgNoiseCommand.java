package rwg.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import rwg.config.ConfigRWG;

public class RwgNoiseCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "rwg_noise";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rwg_noise default|opensimplex|perlin";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

        if (args.length < 1) {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }

        ConfigRWG.setNoiseFunction(args[0]);
    }
}
