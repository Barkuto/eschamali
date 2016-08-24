package modules.Games;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/17/2016.
 */
public class GamesModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "Games";
    private GamesListener gl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        gl = new GamesListener();
        client.getDispatcher().registerListener(gl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(gl);
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
        cmds.add(GamesListener.prefix);
        cmds.add("__**DESC**__: Some fun commands.");
        cmds.add("`8ball`: Asks the magic 8 ball a question. **USAGE**: 8ball \"question\"");
        cmds.add("`choose`: Chooses one of the given choices. **USAGE**: choose option1;option2;option3");
        cmds.add("`rps`: Plays Rock, Paper, Scissors with the both. **USAGE**: rps rock|paper|scissor");
        return cmds;
    }
}
