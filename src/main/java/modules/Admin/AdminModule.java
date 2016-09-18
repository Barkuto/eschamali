package modules.Admin;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 9/15/2016.
 */
public class AdminModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "Admin";
    private AdminListener al;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        al = new AdminListener();
        client.getDispatcher().registerListener(al);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(al);
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
        return "2.5.2";
    }

    @Override
    public ArrayList<String> commands() {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(AdminListener.prefix);
        cmds.add("__**DESC**__: Administration commands");
        return cmds;
    }
}
