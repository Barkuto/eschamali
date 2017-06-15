package modules.JoinLeave;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/17/2016.
 */
public class JoinLeaveModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "JoinLeave";
    private JoinLeaveListener jll;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        jll = new JoinLeaveListener();
        client.getDispatcher().registerListener(jll);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(jll);
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
        return "2.5.3";
    }

    @Override
    public ArrayList<String> commands() {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add("__**DESC**__: Keeps tracks of people who leave/join.");
        return cmds;
    }
}
