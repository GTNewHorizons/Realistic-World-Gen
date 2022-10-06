package rwg.commands;

import java.util.Locale;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import rwg.util.NoiseGeneratorWrapper;
import rwg.util.NoiseImplementation;
import rwg.world.RwgWorldSavedData;

public class RwgNoiseCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "rwg_noise";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rwg_noise get|opensimplex|perlin";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

        if (args.length < 1) {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }

        NoiseImplementation current = RwgWorldSavedData.getNoiseImplementation();

        if (args[0].equals("get")) {
            sender.addChatMessage(new ChatComponentText("Current noise type is " + current));
            return;
        }

        NoiseImplementation noiseImplementation = NoiseImplementation.valueOf(args[0].toUpperCase(Locale.ROOT));
        boolean wasDynamic = true;
        switch (current) {
            case UNKNOWN:
            case DYNAMICPERLIN:
            case DYNAMICOPENSIMPLEX:
                wasDynamic = true;
                break;
            case PERLIN:
            case OPENSIMPLEX:
                wasDynamic = false;
                break;
        }

        if (current != noiseImplementation) {
            switch (noiseImplementation) {
                case UNKNOWN:
                    NoiseGeneratorWrapper.useOpenSimplex = false;
                    sender.addChatMessage(
                            new ChatComponentText("Set noise type to UNKNOWN, which will default to Perlin."));
                    break;
                case DYNAMICPERLIN:
                    NoiseGeneratorWrapper.useOpenSimplex = false;
                    sender.addChatMessage(new ChatComponentText("Set noise type to DYNAMICPERLIN."));
                    break;
                case PERLIN:
                    NoiseGeneratorWrapper.useOpenSimplex = false;
                    sender.addChatMessage(new ChatComponentText("Set noise type to PERLIN."));
                    break;
                case DYNAMICOPENSIMPLEX:
                    NoiseGeneratorWrapper.useOpenSimplex = true;
                    sender.addChatMessage(new ChatComponentText("Set noise type to DYNAMICOPENSIMPLEX."));
                    break;
                case OPENSIMPLEX:
                    NoiseGeneratorWrapper.useOpenSimplex = true;
                    sender.addChatMessage(new ChatComponentText("Set noise type to OPENSIMPLEX."));
                    break;
            }
            if (wasDynamic) {
                sender.addChatMessage(new ChatComponentText(
                        "All new chunks will instantly be generated with the selected algorithm."));
            } else {
                sender.addChatMessage(
                        new ChatComponentText(
                                "World must be reloaded (server restarted or single player relog) for changes to take effect."));
            }
        } else {
            sender.addChatMessage(new ChatComponentText("Noise type unchanged. "));
        }

        RwgWorldSavedData.setNoiseImplementation(noiseImplementation);
    }
}
