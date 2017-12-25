package modules.PAD.PADHerderAPI;

import sx.blah.discord.handle.obj.IEmoji;
import sx.blah.discord.handle.obj.IGuild;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Iggie on 9/30/2016.
 */
public enum Awakening {
    UNKNOWN(0, "??", "??", "Unknown"), HP(1, "Enhanced HP", "+HP", "HP"), ATK(2, "Enhanced Attack", "+ATK", "ATK"), RCV(3, "Enhanced Heal", "+RCV", "RCV"),

    AUTORCV(9, "Auto-Recover", "Auto-RCV", "AutoRCV"), TIMEEXTEND(19, "Extend Time", "TE", "TE"), BINDRCV(20, "Recover Bind", "BindRCV", "BindRCV"),
    SKILLBOOST(21, "Skill Boost", "SB", "SB"), TPA(27, "Two-Pronged Attack", "TPA", "TPA"), SBR(28, "Resistance-Skill Lock", "SBR", "SBR"),

    FIRERES(4, "Reduce Fire Damage", "FireRes", "FireRes"), WATERRES(5, "Reduce Water Damage", "WaterRes", "WaterRes"), WOODRES(6, "Reduce Wood Damage", "WoodRes", "WoodRes"),
    LIGHTRES(7, "Reduce Light Damage", "LightRes", "LightRes"), DARKRES(8, "Reduce Dark Damage", "DarkRes", "DarkRes"),

    BINDRES(10, "Resistance-Bind", "BindRes", "BindRes"), BLINDRES(11, "Resistance-Dark", "BlindRes", "BlindRes"), JAMRES(12, "Resistance-Jammers", "JamRes", "JamRes"),
    POIRES(13, "Resistance-Poison", "PoiRes", "PoiRes"),

    FIREOE(14, "Enhanced Fire Orbs", "FireOE", "FireOE"), WATEROE(15, "Enhanced Water Orbs", "WaterOE", "WaterOE"), WOODOE(16, "Enhanced Wood Orbs", "WoodOE", "WoodOE"),
    LIGHTOE(17, "Enhanced Light Orbs", "LightOE", "LightOE"), DARKOE(18, "Enhanced Dark Orbs", "DarkOE", "DarkOE"), HEARTOE(29, "Enhanced Heart Orbs", "HeartOE", "HeartOE"),

    FIREROW(22, "Enhanced Fire Att.", "FireRow", "FireRow"), WATERROW(23, "Enhanced Water Att.", "WaterRow", "WaterRow"), WOODROW(24, "Enhanced Wood Att.", "WoodRow", "WoodRow"),
    LIGHTROW(25, "Enhanced Light Att.", "LightRow", "LightRow"), DARKROW(26, "Enhanced Dark Att.", "DarkRow", "DarkRow"),

    DRAGONKILLER(31, "Dragon Killer", "DragonKill", "DragonKill"), GODKILLER(32, "God Killer", "GodKill", "GodKill"), DEVILKILLER(33, "Devil Killer", "DevilKill", "DevilKill"),
    MACHINEKILLER(34, "Machine Killer", "MachineKill", "MachineKill"), ATTACKERKILLER(35, "Attacker Killer", "AttackerKill", "AttackerKill"), PHYSICALKILLER(36, "Physical Killer", "PhysicalKill", "PhysKill"),
    HEALERKILLER(37, "Healer Killer", "HealerKill", "HealerKill"), BALANCEDKILLER(38, "Balanced Killer", "BalancedKill", "BalanceKill"), AWAKENKILLER(39, "Awaken Material Killer", "AwakenKill", "AwokenKill"),
    ENHANCEKILLER(40, "Enhance Material Killer", "EnhanceKill", "EnhanceKill"), VENDORKILLER(41, "Redeemable Material Killer", "RedeemKill", "RedeemKill"), EVOKILLER(42, "Evolve Killer", "EvoKill", "EvoKill"),

    COOP(30, "Multi Boost", "CoopBoost", "CoopBoost"), COMBOBOOST(43, "Enhanced Combo", "7cBoost", "7c"), GAURDBREAK(44, "Guard Break", "DefBreak", "GuardBreak"),

    FOLLOWUPATTACK(45, "Follow Up Attack", "FUA", "FUA"),

    HPPLUS(46, "Team HP+", "HP+", "TeamHP"), RCVPLUS(47, "Team RCV+", "RCV+", "TeamRCV"), VOIDPEN(48, "Void Penetration", "VoidPen", "VoidPen"),

    ASSIST(49, "Awoken Assist", "Assist", "Assist"), CHARGE(50, "Skill Charge", "Charge", "Charge"), SFUA(51, "Super Follow Up Attack", "SFUA", "SFUA");

    private int id;
    private String name;
    private String shortName;
    private String emote;

    private static HashMap<Integer, IEmoji> awakeningEmojis;
    private static HashMap<Integer, String> awakeningShortNames;

    Awakening(int id, String name, String shortName, String emote) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.emote = emote;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getEmote() {
        return emote;
    }

    public static void loadShortNames() {
        awakeningShortNames = new HashMap<>();
        for (Awakening a : values()) {
            awakeningShortNames.put(a.id, a.shortName);
        }
    }

    public static String getShortName(int id) {
        return awakeningShortNames.get(id);
    }

    public static void loadEmojis(ArrayList<IGuild> servers) {
        awakeningEmojis = new HashMap<>();
        for (int i = 0; i < servers.size(); i++) {
            IGuild g = servers.get(i);
            Awakening[] vals = Awakening.values();
            for (int j = 0; j < vals.length; j++) {
                IEmoji e = g.getEmojiByName(vals[j].emote);
                if (e != null)
                    awakeningEmojis.put(vals[j].id, e);
            }
            if (awakeningEmojis.size() >= values().length - 1)
                break;
        }
        if (awakeningEmojis.size() < values().length - 1)
            System.out.println("Some Awakening emojis were not loaded.");
    }

    public static IEmoji getEmoji(int id) {
        return awakeningEmojis.get(id);
    }
}
