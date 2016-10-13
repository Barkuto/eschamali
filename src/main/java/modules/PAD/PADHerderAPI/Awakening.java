package modules.PAD.PADHerderAPI;

/**
 * Created by Iggie on 9/30/2016.
 */
public enum Awakening {
    UNKNOWN("??", "??", "??"), HP("Enhanced HP", "+HP", null), ATK("Enhanced ATK", "+ATK", null), RCV("Enhanced RCV", "+RCV", null),

    AUTORCV("Auto-Recover", "Auto-RCV", null), TIMEEXTEND("Extend Time", "TE", null), BINDRCV("Recover Bind", "BindRCV", null),
    SKILLBOOST("Skill Boost", "SB", null), TPA("Two-Pronged Attack", "TPA", null), SBR("Resistance-Skill Lock", "SBR", null),

    FIRERES("Reduce Fire Damage", "FireRes", null), WATERRES("Reduce Water Damage", "WaterRes", null), WOODRES("Reduce Wood Damage", "WoodRes", null),
    LIGHTRES("Reduce Light Damage", "LightRes", null), DARKRES("Reduce Dark Damage", "DarkRes", null),

    BINDRES("Resistance-Bind", "BindRes", null), BLINDRES("Resistance-Dark", "BlindRes", null), JAMRES("Resistance-Jammers", "JamRes", null),
    POIRES("Resistance-Poison", "PoiRes", null),

    FIREOE("Enhanced Fire Orbs", "FireOE", null), WATEROE("Enhanced Water Orbs", "WaterOE", null), WOODOE("Enhanced Wood Orbs", "WoodOE", null),
    LIGHTOE("Enhanced Light Orbs", "LightOE", null), DARKOE("Enhanced Dark Orbs", "DarkOE", null),

    FIREROW("Enhanced Fire Att.", "FireRow", null), WATERROW("Enhanced Water Att.", "WaterRow", null), WOODROW("Enhanced Wood Att.", "WoodRow", null),
    LIGHTROW("Enhanced Light Att.", "LightRow", null), DARKROW("Enhanced Dark Att.", "DarkRow", null),

    DRAGONKILLER("Dragon Killer", "DragonKill", null), GODKILLER("God Killer", "GodKill", null), DEVILKILLER("Devil Killer", "DevilKill", null),
    MACHINEKILLER("Machine Killer", "MachineKill", null), AttackerKILLER("Attacker Killer", "AttackerKill", null), PHYSICALKILLER("Physical Killer", "PhysicalKill", null),
    HEALERKILLER("Healer Killer", "HealerKill", null), BALANCEDKILLER("Balanced Killer", "BalancedKill", null), AWAKENKILLER("Awaken Material Killer", "AwakenKill", null),
    ENHANCEKILLER("Enhance Material Killer", "EnhanceKill", null), VENDORKILLER("Vendor Material Killer", "VendorKill", null), EVOKILLER("Evolve Killer", "EvoKill", null),

    COOP("Multi Boost", "CoopBoost", null);

    private String name;
    private String shortName;
    private String emote;

    Awakening(String name, String shortName, String emote) {
        this.name = name;
        this.shortName = shortName;
        this.emote = emote;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getEmote() {
        return emote != null ? emote : shortName;
    }

    public static Awakening getAwakening(String name) {
        for (Awakening a : Awakening.values()) {
            if (a.getName().equalsIgnoreCase(name)) {
                return a;
            }
        }
        return UNKNOWN;
    }
}
