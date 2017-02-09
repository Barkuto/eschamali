package modules.CustomCommands;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 9/18/2016.
 */
public class CustomCommandsModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "CustomCommands";
    private CustomCommandsListener ccl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        ccl = new CustomCommandsListener();
        client.getDispatcher().registerListener(ccl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(ccl);
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
        cmds.add(CustomCommandsListener.prefix);
        cmds.add("Commands require MANAGE SERVER permission.");
        cmds.add("`addcustomcommand`: Add a custom command. **Usage** addcustomcommand <command name> <text>");
        cmds.add("`editcustomcommand`: Edit an existing custom command. **Usage** editcustomcommand <command name> <text>");
        cmds.add("`deletecustomcommand`: Delete an existing custom command. **Usage** deletecustomcommand <command name>");
        cmds.add("`customcommands`: Shows all custom commands. **Usage** customcommands");
        return cmds;
    }
}
