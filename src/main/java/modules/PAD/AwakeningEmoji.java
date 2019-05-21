package modules.PAD;

import sx.blah.discord.handle.obj.IEmoji;
import sx.blah.discord.handle.obj.IGuild;

import java.util.ArrayList;
import java.util.HashMap;

public enum AwakeningEmoji {
    UNKNOWN(0, "unknown"),
    HP(1, "HP"),
    ATK(2, "ATK"),
    RCV(3, "RCV"),
    FIRERES(4, "FireRes"),
    WATERRES(5, "WaterRes"),
    WOODRES(6, "WoodRes"),
    LIGHTRES(7, "LightRes"),
    DARKRES(8, "DarkRes"),
    AUTORCV(9, "AutoRCV"),
    BINDRES(10, "BindRes"),
    BLINDRES(11, "BlindRes"),
    JAMRES(12, "JamRes"),
    POIRES(13, "PoiRes"),
    FIREOE(14, "FireOE"),
    WATEROE(15, "WaterOE"),
    WOODOE(16, "WoodOE"),
    LIGHTOE(17, "LightOE"),
    DARKOE(18, "DarkOE"),
    HEARTOE(29, "HeartOE"),
    TIMEEXTEND(19, "TE"),
    BINDRCV(20, "BindRCV"),
    SKILLBOOST(21, "SB"),
    TPA(27, "TPA"),
    SBR(28, "SBR"),
    FIREROW(22, "FireRow"),
    WATERROW(23, "WaterRow"),
    WOODROW(24, "WoodRow"),
    LIGHTROW(25, "LightRow"),
    DARKROW(26, "DarkRow"),
    COOP(30, "CoopBoost"),
    DRAGONKILLER(31, "DragonKill"),
    GODKILLER(32, "GodKill"),
    DEVILKILLER(33, "DevilKill"),
    MACHINEKILLER(34, "MachineKill"),
    BALANCEDKILLER(35, "BalanceKill"),
    ATTACKERKILLER(36, "AttackerKill"),
    PHYSICALKILLER(37, "PhysKill"),
    HEALERKILLER(38, "HealerKill"),
    EVOMATKILLER(39, "EvoKill"),
    AWOKENMATKILLER(40, "AwokenKill"),
    ENHANCEKILLER(41, "EnhanceKill"),
    VENDORKILLER(42, "RedeemKill"),
    SEVENC(43, "7c"),
    GUARDBREAK(44, "GuardBreak"),
    FOLLOWUPATTACK(45, "FUA"),
    TEAMHP(46, "TeamHP"),
    TEAMRCV(47, "TeamRCV"),
    VOIDPEN(48, "VoidPen"),
    ASSIST(49, "Assist"),
    SFUA(50, "SFUA"),
    CHARGE(51, "Charge"),
    BINDRESPLUS(52, "BindResPlus"),
    TEPLUS(53, "TEPlus"),
    CLOUDRES(54, "CloudRes"),
    RIBBONRES(55, "RibbonRes"),
    SBPLUS(56, "SBPlus"),
    HP80BOOST(57, "80Boost"),
    HP50BOOST(58, "50Boost"),
    LSHIELD(59, "LShield"),
    LUNLOCK(60, "LUnlock"),
    TENC(61, "10c"),

    COMBOORB(62, "ComboOrb"),
    VOICE(63, "Voice"),
    DUNGEONBOOST(64, "DungeonBoost"),

    MINUSHP(65, "MinusHP"),
    MINUSATK(66, "MinusATK"),
    MINUSRCV(67, "MinusRCV"),
    ;

    private int id;
    private String emote;
    private static HashMap<Integer, IEmoji> awakeningEmojis;

    AwakeningEmoji(int id, String emote) {
        this.id = id;
        this.emote = emote;
    }

    public static void loadEmojis(ArrayList<IGuild> servers) {
        awakeningEmojis = new HashMap<>();
        for (int i = 0; i < servers.size(); i++) {
            IGuild g = servers.get(i);
            AwakeningEmoji[] vals = AwakeningEmoji.values();
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
        if (awakeningEmojis.containsKey(id))
            return awakeningEmojis.get(id);
        else return awakeningEmojis.get(-1);
    }
}
