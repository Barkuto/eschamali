package modules.PAD.PADHerderAPI;

public enum Type {
    EVOMAT(0, "Evo Material"),
    BALANCED(1, "Balanced"),
    PHYSICAL(2, "Physical"),
    HEALER(3, "Healer"),
    DRAGON(4, "Dragon"),
    GOD(5, "God"),
    ATTACKER(6, "Attacker"),
    DEVIL(7, "Devil"),
    MACHINE(8, "Machine"),
    AWOKENMAT(12, "Awoken Skill Material"),
    ENHANCEMAT(14, "Enhance Material"),
    REDEEM(15, "Redeemable"),
    NONE(99, "");

    private int id;
    private String name;

    Type(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }
}
