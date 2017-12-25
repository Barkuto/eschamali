package modules.PAD.PADHerderAPI;

import java.io.Serializable;

public class Active implements Serializable {
    private String name;
    private String effect;
    private int minCD;
    private int maxCD;

    public Active(String name, String effect, int minCD, int maxCD) {
        this.name = name;
        this.effect = effect;
        this.minCD = minCD;
        this.maxCD = maxCD;
    }

    public String getName() {
        return name;
    }

    public String getEffect() {
        return effect;
    }

    public int getMinCD() {
        return minCD;
    }

    public int getMaxCD() {
        return maxCD;
    }
}