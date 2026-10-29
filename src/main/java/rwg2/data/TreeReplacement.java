package rwg2.data;

import net.minecraft.init.Blocks;
import net.minecraftforge.event.terraingen.SaplingGrowTreeEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import rwg2.RWG;
import rwg2.deco.trees.DecoSavannah;
import rwg2.deco.trees.DecoSmallPine;

public class TreeReplacement {
    /*
     * Missing a check for air and its extremely buggy :(
     */

    @SubscribeEvent
    public boolean saplingGrowTree(SaplingGrowTreeEvent e) {
        if (e.world.getWorldInfo().getTerrainType() == RWG.worldtype) {
            int type = e.world.getBlockMetadata(e.x, e.y, e.z);
            if (type == 9) {
                e.world.setBlock(e.x, e.y, e.z, Blocks.air);
                (new DecoSmallPine(3 + e.rand.nextInt(3), 5 + e.rand.nextInt(5)))
                        .generate(e.world, e.rand, e.x, e.y, e.z);
                return true;
            }
            if (type == 12) {
                e.world.setBlock(e.x, e.y, e.z, Blocks.air);
                (new DecoSavannah(1)).generate(e.world, e.rand, e.x, e.y, e.z);
                return true;
            }
        }
        return false;
    }
}
