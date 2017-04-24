package modules.Yugioh;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 4/8/2017.
 */
public class YGOModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "Yugioh";
    private YGOListener yl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        yl = new YGOListener();
        client.getDispatcher().registerListener(yl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(yl);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAuthor() {
        return "Barkuto";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getMinimumDiscord4JVersion() {
        return "2.7.0";
    }

    @Override
    public ArrayList<String> commands() {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(YGOListener.prefix);
        cmds.add("`info`:  Searches for a card and shows info about it **USAGE**: info <query>");
        cmds.add("`update`:  Updates card database **USAGE**: update");
        return cmds;
    }
}
