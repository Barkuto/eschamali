package modules.Channels;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/20/2016.
 */
public class ChannelsModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "Channels";
    private ChannelsListener cl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        cl = new ChannelsListener();
        client.getDispatcher().registerListener(cl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(cl);
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
        cmds.add(ChannelsListener.prefix);
        cmds.add("__**DESC**__: Manages what channels the bot can talk in.");
        cmds.add("`atc`: Adds a channel to allow the bot to talk in. **USAGE**: atc #channel");
        cmds.add("`dtc`: Removes a channel the bot can talk in. **USAGE**: dtc #channel");
        cmds.add("`rtc`: Resets the channels the bot can talk in. **USAGE**: rtc");
        cmds.add("`tc`: Lists the channels the bot can talk in. **USAGE**: tc");
        return cmds;
    }
}
