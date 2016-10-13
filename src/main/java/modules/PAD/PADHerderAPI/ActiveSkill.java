package modules.PAD.PADHerderAPI;

import com.google.gson.JsonObject;

/**
 * Created by Iggie on 10/13/2016.
 */
public class ActiveSkill {
    private String name;
    private String effect;
    private int min_cooldown;
    private int max_cooldown;

    public ActiveSkill(JsonObject json) {
        name = json.get("name").toString().replace("\"", "");
        effect = json.get("effect").toString().replace("\"", "");
        min_cooldown = json.get("min_cooldown").getAsInt();
        max_cooldown = json.get("max_cooldown").getAsInt();
    }

    public String getName() {
        return name;
    }

    public String getEffect() {
        return effect;
    }

    public int getMinCD() {
        return min_cooldown;
    }

    public int getMaxCD() {
        return max_cooldown;
    }

    public String toString() {
        String s = "";
        s += "Name: " + name + "\n";
        s += "Effect: (" + max_cooldown + "->" + min_cooldown + ") " + effect + "\n";
        return s;
    }
}
