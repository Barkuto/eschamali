package modules.PAD.PADHerderAPI;

/**
 * Created by Iggie on 10/13/2016.
 */
public enum Type {
    UNKNOWN(-1, "??", "??"),
    EVOMAT(0, "Evo Material", null), BALANCED(1, "Balanced", null), PHYSICAL(2, "Physical", null), HEALER(3, "Healer", null),
    DRAGON(4, "Dragon", null), GOD(5, "God", null), ATTACKER(6, "Attacker", null), DEVIL(7, "Devil", null), MACHINE(8, "Machine", null),
    AWOKENSKILLMAT(12, "Awoken Skill Material", null), PROTECTED(13, "Protected", null), ENHANCEMAT(14, "Enhance Material", null);

    private int id;
    private String name;
    private String emote;

    Type(int id, String name, String emote) {
        this.id = id;
        this.name = name;
        this.emote = emote;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmote() {
        return emote != null ? emote : name;
    }

    public static Type getType(int id) {
        for (Type t : Type.values()) {
            if (t.getID() == id) {
                return t;
            }
        }
        return UNKNOWN;
    }

    public static Type getType(String name) {
        for (Type t : Type.values()) {
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return UNKNOWN;
    }
}
