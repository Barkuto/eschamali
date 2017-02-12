package modules.PAD.PADHerderAPI;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by Iggie on 10/11/2016.
 */
public class Monster {
    private int id;
    private int pdx_id;
    private int us_id;
    private String name;
    private String name_jp;//escaped unicode
    private int rarity;
    private int team_cost;
    private int monster_points;
    private String element;
    private String element2;
    private String type;
    private String type2;
    private String type3;
    private ActiveSkill active_skill;
    private LeaderSkill leader_skill;
    private AwokenSkill[] awoken_skills;

    private int hp_min;
    private int hp_max;
    private double hp_scale;
    private int atk_min;
    private int atk_max;
    private double atk_scale;
    private int rcv_min;
    private int rcv_max;
    private double rcv_scale;

    private String image40_href;//escaped unicode
    private String image40_size;
    private String image60_href;//escaped unicode
    private String image60_size;

    private int max_level;
    private long xp_curve;

    private int version;
    private boolean jp_only;
    private double feed_xp;

    public Monster(JsonObject json) {
        try {
            id = json.get("pdx_id").getAsInt();
        } catch (NullPointerException e) {
            id = json.get("id").getAsInt();
        }
        name = json.get("name").toString().replace("\"", "");
        name_jp = json.get("name_jp").toString().replace("\"", "");
        rarity = json.get("rarity").getAsInt();
        team_cost = json.get("team_cost").getAsInt();
        monster_points = json.get("monster_points").getAsInt();
        element = json.get("element").toString();
        element2 = json.get("element2").toString();
        type = json.get("type").toString();
        type2 = json.get("type2").toString();
        type3 = json.get("type3").toString();
        active_skill = PADHerderAPI.getActive(json.get("active_skill").toString().replace("\"", ""));
        leader_skill = PADHerderAPI.getLeader(json.get("leader_skill").toString().replace("\"", ""));
        JsonArray array = json.get("awoken_skills").getAsJsonArray();
        awoken_skills = new AwokenSkill[array.size()];
        for (int i = 0; i < array.size(); i++) {
            awoken_skills[i] = PADHerderAPI.getAwokenSkill(array.get(i).getAsInt());
        }
        hp_min = json.get("hp_min").getAsInt();
        hp_max = json.get("hp_max").getAsInt();
        hp_scale = json.get("hp_scale").getAsDouble();
        atk_min = json.get("atk_min").getAsInt();
        atk_max = json.get("atk_max").getAsInt();
        atk_scale = json.get("atk_scale").getAsDouble();
        rcv_min = json.get("rcv_min").getAsInt();
        rcv_max = json.get("rcv_max").getAsInt();
        rcv_scale = json.get("rcv_scale").getAsDouble();

        image40_href = json.get("image40_href").toString();
        image40_size = json.get("image40_size").toString();
        image60_href = json.get("image60_href").toString();
        image60_size = json.get("image60_size").toString();

        max_level = json.get("max_level").getAsInt();
        xp_curve = json.get("xp_curve").getAsInt();

        version = json.get("version").getAsInt();
        jp_only = json.get("jp_only").getAsBoolean();
        feed_xp = json.get("feed_xp").getAsDouble();
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

    public String getElement() {
        return element;
    }

    public String getElement2() {
        return element2;
    }

    public String getType() {
        return type;
    }

    public String getType2() {
        return type2;
    }

    public String getType3() {
        return type3;
    }

    public ActiveSkill getActive_skill() {
        return active_skill;
    }

    public LeaderSkill getLeader_skill() {
        return leader_skill;
    }

    public AwokenSkill[] getAwoken_skills() {
        return awoken_skills;
    }

    public int getHp_min() {
        return hp_min;
    }

    public int getHp_max() {
        return hp_max;
    }

    public double getHp_scale() {
        return hp_scale;
    }

    public int getAtk_min() {
        return atk_min;
    }

    public int getAtk_max() {
        return atk_max;
    }

    public double getAtk_scale() {
        return atk_scale;
    }

    public int getRcv_min() {
        return rcv_min;
    }

    public int getRcv_max() {
        return rcv_max;
    }

    public double getRcv_scale() {
        return rcv_scale;
    }

    public String getImage40_href() {
        return image40_href;
    }

    public String getImage40_size() {
        return image40_size;
    }

    public String getImage60_href() {
        return image60_href;
    }

    public String getImage60_size() {
        return image60_size;
    }

    public int getMax_level() {
        return max_level;
    }

    public long getXp_curve() {
        return xp_curve;
    }

    public int getVersion() {
        return version;
    }

    public boolean isJp_only() {
        return jp_only;
    }

    public double getFeed_xp() {
        return feed_xp;
    }

    public String toString() {
        String s = "";
        s += "id: " + id + "\n";
        s += "name: " + name + "\n";
        return s;
    }
}
