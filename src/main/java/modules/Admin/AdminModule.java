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
        cmds.add("`kick`: Kicks the specified user(s), requires KICK permission. **USAGE**: kick @user1 @user2");
        cmds.add("`ban`: Bans the specified user(s), requires BAN permission. **USAGE**: ban @user1 @user2");
        cmds.add("`prune`: Prunes the specified users last X messages, requires MANAGE MESSAGES permission. **USAGE**: prune @user <# msgs to delete>");
        cmds.add("`mute`: Mutes the person from chatting in the server, requires MANAGE MESSAGES permission. **USAGE**: mute @user");
        cmds.add("`lock`: Locks current channel from chatting, requires MANAGE MESSAGES permission. **USAGE**: lock");
        cmds.add("`unlock`: Unlocks current channel from chatting, requires MANAGE MESSAGES permission. **USAGE**: unlock");
        cmds.add("`warn`: Warns user, requires BAN or KICK permission. **USAGE**: warn @user");
        cmds.add("`warnings`: Shows how many warnings you have. **USAGE**: warnings");
        cmds.add("`warnings`: Shows how many warnings another use has, requires BAN or KICK permission. **USAGE**: warnings @user");
        return cmds;
    }
}
