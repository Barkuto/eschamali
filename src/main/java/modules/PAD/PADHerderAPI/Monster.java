package modules.PAD.PADHerderAPI;

import java.io.Serializable;

public class Monster implements Serializable {
    private int id;
    private String name;
    private String name_jp;
    private int rarity;
    private int team_cost;
    private int monster_points;
    private Attribute att1;
    private Attribute att2;
    private Type type;
    private Type type2;
    private Type type3;
    private Active active;
    private Leader leader;
    private Awakening[] awakenings;
    private int hp_min;
    private int hp_max;
    private int atk_min;
    private int atk_max;
    private int rcv_min;
    private int rcv_max;
    private int max_level;
    private int xp_curve;
    private boolean jp_only;
    private double weighted;

    public Monster(int id, String name, String name_jp, int rarity, int team_cost, int monster_points, Attribute att1, Attribute att2,
                   Type type, Type type2, Type type3, Active active, Leader leader, Awakening[] awakenings,
                   int hp_min, int hp_max, int atk_min, int atk_max, int rcv_min, int rcv_max, int max_level, int xp_curve, boolean jp_only) {
        this.id = id;
        this.name = name;
        this.name_jp = name_jp;
        this.rarity = rarity;
        this.team_cost = team_cost;
        this.monster_points = monster_points;
        this.att1 = att1;
        this.att2 = att2;
        this.type = type;
        this.type2 = type2;
        this.type3 = type3;
        this.active = active;
        this.leader = leader;
        this.awakenings = awakenings;
        this.hp_min = hp_min;
        this.hp_max = hp_max;
        this.atk_min = atk_min;
        this.atk_max = atk_max;
        this.rcv_min = rcv_min;
        this.rcv_max = rcv_max;
        this.max_level = max_level;
        this.xp_curve = xp_curve;
        this.jp_only = jp_only;
        this.weighted = (hp_max / 10.0) + (atk_max / 5.0) + (rcv_max / 3.0);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getName_jp() {
        return name_jp;
    }

    public int getRarity() {
        return rarity;
    }

    public int getTeam_cost() {
        return team_cost;
    }

    public int getMonster_points() {
        return monster_points;
    }

    public Attribute getAtt1() {
        return att1;
    }

    public Attribute getAtt2() {
        return att2;
    }

    public Type getType() {
        return type;
    }

    public Type getType2() {
        return type2;
    }

    public Type getType3() {
        return type3;
    }

    public Active getActive() {
        return active;
    }

    public Leader getLeader() {
        return leader;
    }

    public Awakening[] getAwakenings() {
        return awakenings;
    }

    public int getHp_min() {
        return hp_min;
    }

    public int getHp_max() {
        return hp_max;
    }

    public int getAtk_min() {
        return atk_min;
    }

    public int getAtk_max() {
        return atk_max;
    }

    public int getRcv_min() {
        return rcv_min;
    }

    public int getRcv_max() {
        return rcv_max;
    }

    public int getMax_level() {
        return max_level;
    }

    public int getXp_curve() {
        return xp_curve;
    }

    public boolean isJp_only() {
        return jp_only;
    }

    public double getWeighted() {
        return weighted;
    }
}
