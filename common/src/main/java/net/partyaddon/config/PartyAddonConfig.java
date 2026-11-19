package net.partyaddon.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "partyaddon")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class PartyAddonConfig implements ConfigData {

    public boolean distributeVanillaXP = true;
    @Comment("LevelZ compatibility")
    public boolean distributeLevelZXP = true;
    @Comment("Time in ticks")
    public int invitationTime = 1200;
    @Comment("Synced to clients, the party screen shows up to 7 members")
    public int groupSize = 7;
    @Comment("Bonus per group member, 0.05 = 5% per member")
    public float groupXpBonus = 0.05f;
    @Comment("Additional bonus when the group is full")
    public float fullGroupBonus = 0.5f;

    public boolean showGroupHud = true; // Client only
    public float hudScale = 1.0f; // Client only
    public int hudMaxWidth = 55; // Client only
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int hudOpacity = 75; // Client only
    public int hudPosX = 0; // Client only
    public int hudPosY = 0; // Client only
    public boolean hideOwnPlayer = false; // Client only

    @Override
    public void validatePostLoad() {
        this.invitationTime = Math.max(20, this.invitationTime);
        this.groupSize = Math.max(2, this.groupSize);
        this.groupXpBonus = Math.max(0.0f, this.groupXpBonus);
        this.fullGroupBonus = Math.max(0.0f, this.fullGroupBonus);
        this.hudScale = this.hudScale > 0.0f ? this.hudScale : 1.0f;
        this.hudMaxWidth = Math.max(9, this.hudMaxWidth);
        this.hudOpacity = Math.clamp(this.hudOpacity, 0, 100);
    }
}