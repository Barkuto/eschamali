package modules.PAD.PADHerderAPI;

/**
 * Created by Iggie on 9/30/2016.
 */
public enum Awakening {
    UNKNOWN("??", "??", "Unkown"), HP("Enhanced HP", "+HP", "HP"), ATK("Enhanced Attack", "+ATK", "ATK"), RCV("Enhanced Heal", "+RCV", "RCV"),

    AUTORCV("Auto-Recover", "Auto-RCV", "AutoRCV"), TIMEEXTEND("Extend Time", "TE", "TE"), BINDRCV("Recover Bind", "BindRCV", "BindRCV"),
    SKILLBOOST("Skill Boost", "SB", "SB"), TPA("Two-Pronged Attack", "TPA", "TPA"), SBR("Resistance-Skill Lock", "SBR", "SBR"),

    FIRERES("Reduce Fire Damage", "FireRes", "FireRes"), WATERRES("Reduce Water Damage", "WaterRes", "WaterRes"), WOODRES("Reduce Wood Damage", "WoodRes", "WoodRes"),
    LIGHTRES("Reduce Light Damage", "LightRes", "LightRes"), DARKRES("Reduce Dark Damage", "DarkRes", "DarkRes"),

    BINDRES("Resistance-Bind", "BindRes", "BindRes"), BLINDRES("Resistance-Dark", "BlindRes", "BlindRes"), JAMRES("Resistance-Jammers", "JamRes", "JamRes"),
    POIRES("Resistance-Poison", "PoiRes", "PoiRes"),

    FIREOE("Enhanced Fire Orbs", "FireOE", "FireOE"), WATEROE("Enhanced Water Orbs", "WaterOE", "WaterOE"), WOODOE("Enhanced Wood Orbs", "WoodOE", "WoodOE"),
    LIGHTOE("Enhanced Light Orbs", "LightOE", "LightOE"), DARKOE("Enhanced Dark Orbs", "DarkOE", "DarkOE"), HEARTOE("Enhanced Heart Orbs", "HeartOE", "HeartOE"),

    FIREROW("Enhanced Fire Att.", "FireRow", "FireRow"), WATERROW("Enhanced Water Att.", "WaterRow", "WaterRow"), WOODROW("Enhanced Wood Att.", "WoodRow", "WoodRow"),
    LIGHTROW("Enhanced Light Att.", "LightRow", "LightRow"), DARKROW("Enhanced Dark Att.", "DarkRow", "DarkRow"),

    DRAGONKILLER("Dragon Killer", "DragonKill", "DragonKill"), GODKILLER("God Killer", "GodKill", "GodKill"), DEVILKILLER("Devil Killer", "DevilKill", "DevilKill"),
    MACHINEKILLER("Machine Killer", "MachineKill", "MachineKill"), ATTACKERKILLER("Attacker Killer", "AttackerKill", "AttackerKill"), PHYSICALKILLER("Physical Killer", "PhysicalKill", "PhysKill"),
    HEALERKILLER("Healer Killer", "HealerKill", "HealerKill"), BALANCEDKILLER("Balanced Killer", "BalancedKill", "BalanceKill"), AWAKENKILLER("Awaken Material Killer", "AwakenKill", "AwokenKill"),
    ENHANCEKILLER("Enhance Material Killer", "EnhanceKill", "EnhanceKill"), VENDORKILLER("Vendor Material Killer", "VendorKill", "VendorKill"), EVOKILLER("Evolve Killer", "EvoKill", "EvoKill"),

    COOP("Multi Boost", "CoopBoost", "CoopBoost"), COMBOBOOST("Enhanced Combo", "7cBoost", "7c"), GAURDBREAK("Guard Break", "DefBreak", "GuardBreak"),

    FOLLOWUPATTACK("Follow Up Attack", "FUA", "FUA"),

    HPPLUS("Team HP+", "HP+", "TeamHP"), RCVPLUS("Team RCV+", "RCV+", "TeamRCV"), VOIDPEN("Void Penetration", "VoidPen", "VoidPen");


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
