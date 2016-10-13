package modules.PAD.PADHerderAPI;

import com.google.gson.JsonObject;

/**
 * Created by Iggie on 10/13/2016.
 */
public class LeaderSkill {
    private String data;
    private String effect;
    private String name;

    public LeaderSkill(JsonObject json) {
        try {
            data = json.get("data").toString();
        } catch (NullPointerException e) {
            data = null;
        }
        effect = json.get("effect").toString().replace("\"", "");
        name = json.get("name").toString().replace("\"", "");
    }

    public String getData() {
        return data;
    }

    public String getEffect() {
        return effect;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        String s = "";
        s += "name: " + name + "\n";
        s += "effect: " + effect + "\n";
        s += "data: " + data != null ? data : null + "\n";
        return s;
    }
}
