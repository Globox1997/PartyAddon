package net.partyaddon.screen.widget;

import net.libz.api.InventoryTab;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.partyaddon.network.PartyAddonClientPacket;

public class PartyTab extends InventoryTab {

    public PartyTab(Component title, ResourceLocation texture, int preferedPos, Class<?>... screenClasses) {
        super(title, texture, preferedPos, screenClasses);
    }

    @Override
    public void onClick(Minecraft client) {
        PartyAddonClientPacket.writeC2SOpenPartyScreenPacket(client);
    }

}
