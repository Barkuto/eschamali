package modules.Profiles;

/**
 * Created by Iggie on 5/23/2017.
 */

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 4/8/2017.
 */
public class ProfilesModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "Profiles";
    private ProfilesListener pl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        pl = new ProfilesListener();
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
        return "2.7.0";
    }

    @Override
    public ArrayList<String> commands() {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(ProfilesListener.prefix);
        cmds.add("Custom profiles have the following fields: Name, Nickname, Picture, Bio, Color, Footer, FooterIcon, and Fields.");
        cmds.add("To __set__ a field use the command `profilesetFIELDNAMEHERE ARGUMENTS GO HERE`");
        cmds.add("To __reset__ a field use the command `profileresetFIELDNAMEHERE`");
        cmds.add("To __reset__ your profile, use the command `profilereset`");
        cmds.add("To add Fields, use `profileaddfield FIELDNAMEHERE;CONTENT HERE`");
        cmds.add("To remove Fields, use `profileremovefield FIELDNAMEHERE`");
        cmds.add("To view your profile use the `profile` command.");
        return cmds;
    }
}
