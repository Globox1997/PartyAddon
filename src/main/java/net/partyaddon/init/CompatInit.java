package net.partyaddon.init;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.partyaddon.access.compat.WorldMapAccess;
import xaero.map.gui.GuiMap;

public class CompatInit {

    public static void syncGroupToMap(MinecraftClient client, List<UUID> groupPlayerUUIDs, List<BlockPos> groupPlayerBlockPoses, List<Float> groupPlayerYaws) {
//        if (client.currentScreen != null && client.currentScreen instanceof GuiMap) {
//            ((WorldMapAccess) client.currentScreen).setGroupPlayers(groupPlayerUUIDs, groupPlayerBlockPoses, groupPlayerYaws);
//        }
    }

}
