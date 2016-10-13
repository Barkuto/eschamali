package modules.PAD.PADHerderAPI;

/**
 * Created by Iggie on 10/13/2016.
 */
public enum Attribute {
    FIRE("Fire", null), WATER("Water", null), WOOD("Wood", null), LIGHT("Light", null), DARK("Dark", null);

    private String name;
    private String emote;

    Attribute(String name, String emote) {
        this.name = name;
        this.emote = emote;
    }

    public String getName() {
        return name;
    }

    public String getEmote() {
        return emote;
    }
}
