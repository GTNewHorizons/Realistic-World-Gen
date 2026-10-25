package rwg.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;

import rwg.config.ConfigRWG;
import rwg.util.NoiseImplementation;

public class RwgWorldSavedData extends WorldSavedData {

    private static RwgWorldSavedData INSTANCE = null;
    private static final String DATA_NAME = "rwg_data";
    private NoiseImplementation noiseImplementation = rwg.util.NoiseImplementation.UNKNOWN;

    public static final int BIOME_LIST_UNKNOWN = 0;
    public static final int BIOME_LIST_LEGACY = 1;
    public static final int BIOME_LIST_FIXED = 2;
    private int biomeListVersion = BIOME_LIST_UNKNOWN;

    public RwgWorldSavedData() {
        super(DATA_NAME);
    }

    public RwgWorldSavedData(String s) {
        super(s);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound) {
        String noiseImplementation = nbtTagCompound.getString("noiseImplementation");

        try {
            this.noiseImplementation = NoiseImplementation.valueOf(noiseImplementation);
        } catch (IllegalArgumentException e) {
            System.out.println("RWG ERROR LOADING DATA - NOISE IS: " + this.noiseImplementation);
        }

        this.biomeListVersion = nbtTagCompound.getInteger("biomeListVersion");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound) {
        String noiseString = noiseImplementation.toString();

        try {
            nbtTagCompound.setString("noiseImplementation", noiseString);
        } catch (Exception e) {
            System.out.println("RWG ERROR SAVING DATA - NOISE IS: " + this.noiseImplementation);
        }

        nbtTagCompound.setInteger("biomeListVersion", biomeListVersion);
    }

    static void loadInstance() {
        if (INSTANCE == null) {
            WorldServer world = DimensionManager.getWorld(0);
            if (world == null) return;

            MapStorage storage = world.mapStorage;
            if (storage == null) return;

            INSTANCE = (RwgWorldSavedData) storage.loadData(RwgWorldSavedData.class, DATA_NAME);
            if (INSTANCE == null) {
                INSTANCE = new RwgWorldSavedData();
                storage.setData(DATA_NAME, INSTANCE);
            }
        }
        INSTANCE.markDirty();
    }

    public static void setNoiseImplementation(NoiseImplementation ni) {
        loadInstance();

        INSTANCE.noiseImplementation = ni;
    }

    public static NoiseImplementation getNoiseImplementation() {
        loadInstance();
        if (INSTANCE == null) {
            return NoiseImplementation.UNKNOWN;
        }

        return INSTANCE.noiseImplementation;
    }

    /**
     * Must be called with the world under construction: worldInfo is not initialized yet for a brand new world, and
     * DimensionManager does not know about it yet.
     */
    public static int getBiomeListVersion(World world) {
        if (world.isRemote) {
            // Integrated server already decided; a remote server's choice is unknown to the client.
            WorldServer server = DimensionManager.getWorld(0);
            if (server != null && server.mapStorage != null) {
                RwgWorldSavedData data = (RwgWorldSavedData) server.mapStorage
                        .loadData(RwgWorldSavedData.class, DATA_NAME);
                if (data != null && data.biomeListVersion != BIOME_LIST_UNKNOWN) {
                    return data.biomeListVersion;
                }
            }
            return ConfigRWG.fixedBiomeListsForNewWorlds ? BIOME_LIST_FIXED : BIOME_LIST_LEGACY;
        }

        MapStorage storage = world.mapStorage;
        if (storage == null) {
            return BIOME_LIST_LEGACY;
        }

        RwgWorldSavedData data = loadOrCreate(storage);
        if (data.biomeListVersion == BIOME_LIST_UNKNOWN) {
            boolean isNewWorld = !world.getWorldInfo().isInitialized();
            data.biomeListVersion = isNewWorld && ConfigRWG.fixedBiomeListsForNewWorlds ? BIOME_LIST_FIXED
                    : BIOME_LIST_LEGACY;
            data.markDirty();
            System.out.println(
                    "RWG: " + (isNewWorld ? "new" : "existing")
                            + " world, using biome list version "
                            + data.biomeListVersion);
        }

        return data.biomeListVersion;
    }

    /**
     * Only takes effect on the next world load, since the chunk manager reads it once on construction.
     */
    public static boolean setBiomeListVersion(int version) {
        WorldServer world = DimensionManager.getWorld(0);
        if (world == null || world.mapStorage == null) return false;

        RwgWorldSavedData data = loadOrCreate(world.mapStorage);
        data.biomeListVersion = version;
        data.markDirty();
        return true;
    }

    private static RwgWorldSavedData loadOrCreate(MapStorage storage) {
        RwgWorldSavedData data = (RwgWorldSavedData) storage.loadData(RwgWorldSavedData.class, DATA_NAME);
        if (data == null) {
            data = new RwgWorldSavedData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }
}
