package modules.PAD.PADHerderAPI;

import com.google.gson.JsonObject;

/**
 * Created by Iggie on 10/13/2016.
 */
public class AwokenSkill {
    private String desc;
    private int id;
    private String name;

    public AwokenSkill(JsonObject json) {
        desc = json.get("desc").toString().replace("\"", "");
        id = json.get("id").getAsInt();
        name = json.get("name").toString().replace("\"", "");
    }

    public String getDesc() {
        return desc;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        String s = "";
        s += "id: " + id + "\n";
        s += "name: " + name + "\n";
        s += "desc: " + desc + "\n";
        return s;
    }
}
