package modules.PAD.PADHerderAPI;

import java.io.Serializable;

public class Leader implements Serializable {
    private String name;
    private String desc;

    public Leader(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
