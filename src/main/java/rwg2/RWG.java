package rwg2;

import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import rwg2.biomes.base.BaseBiomes;
import rwg2.commands.RwgBugInfoCommand;
import rwg2.commands.RwgNoiseCommand;
import rwg2.commands.SpeedCommand;
import rwg2.config.ConfigRWG;
import rwg2.data.RailcraftWorldgenFilter;
import rwg2.data.VillageMaterials;
import rwg2.handlers.LoginHandler;
import rwg2.support.Support;
import rwg2.world.WorldTypeRealistic;

@Mod(
        modid = "rwg2",
        name = "RealisticWorldGen",
        version = Tags.VERSION,
        dependencies = "required-after:endlessids",
        acceptableRemoteVersions = "*")
public class RWG {

    @Instance("rwg2")
    public static RWG instance;

    public static final WorldTypeRealistic worldtype = new WorldTypeRealistic("RWG2", false);
    public static final WorldTypeRealistic continentWorldtype = new WorldTypeRealistic("RWG2_CONTINENTS", true);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;

        ConfigRWG.init(event);
        BaseBiomes.load();

        MinecraftForge.TERRAIN_GEN_BUS.register(new VillageMaterials());
        MinecraftForge.TERRAIN_GEN_BUS.register(new RailcraftWorldgenFilter());

        FMLCommonHandler.instance().bus().register(new LoginHandler());
        // MinecraftForge.TERRAIN_GEN_BUS.register(new TreeReplacement());
    }

    @EventHandler
    public void Init(FMLInitializationEvent event) {}

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        BaseBiomes.validateRegistrations();
        Support.init();
    }

    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        ICommandManager command = server.getCommandManager();
        ServerCommandManager manager = (ServerCommandManager) command;

        manager.registerCommand(new RwgNoiseCommand());
        manager.registerCommand(new RwgBugInfoCommand());
        manager.registerCommand(new SpeedCommand());
    }
}
