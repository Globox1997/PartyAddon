package net.partyaddon.init;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.partyaddon.config.PartyAddonConfig;

public class ConfigInit {

    public static PartyAddonConfig CONFIG = new PartyAddonConfig();

    public static void init() {
        AutoConfig.register(PartyAddonConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(PartyAddonConfig.class).getConfig();
    }
}
