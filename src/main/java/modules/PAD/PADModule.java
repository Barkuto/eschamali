package modules.PAD;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/25/2016.
 */
public class PADModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "PAD";
    private PADListener pl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        pl = new PADListener();
        client.getDispatcher().registerListener(pl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(pl);
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
        return "2.5.4";
    }

    @Override
    public ArrayList<String> commands() {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(PADListener.prefix);
//        cmds.add("`join`: Makes the bot join your voice channel. **USAGE**: join");
        return cmds;
    }
}
