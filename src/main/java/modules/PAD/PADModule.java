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
    public static String credentials;
    private PADListener pl;

    public PADModule(String credentials) {
        super();
        this.credentials = credentials;
    }

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
        cmds.add("`monster`:  Searches for a monster, and links its PADx page. **USAGE**: monster <query>");
        cmds.add("`info`:  Displays a monsters PADx info. **USAGE**: info <query>");
        cmds.add("`dungeon`:  Searches for a dungeon, and links its PADx page. **USAGE**: dungeon <query>");
        cmds.add("`guerilla`:  Displays the guerilla dungeons for the day. **USAGE**: guerilla <timezone> <group>");
        cmds.add("`pic`:  Displays a monsters full art from PADx. **USAGE**: pic <query>");
        cmds.add("`guerillaall`: Displays guerillas for all timezones and groups. **USAGE**: guerillaall");
        cmds.add("`addguerillachannel`: Adds a channel for auto guerilla posting. Requires some MANAGE_ perm **USAGE**: addguerillachannel #channel");
        cmds.add("`deleteguerillachannel`: Deletes a channel from auto guerilla posting. Requires some MANAGE_ perm **USAGE**: deleteguerillachannel #channel");
        cmds.add("`guerillachannels`: Shows what channels have automatic guerilla posting. Requires some MANAGE_ perm **USAGE**: guerillachannels");
        cmds.add("`buttoncalc`: Calc dmg for buttoning things. **USAGE**: &buttoncalc <base atk> <atk plusses> <atk lnts> <atk+ lnts> <coop: Y/N> <inherit base atk> <nuke amt>");
        return cmds;
    }
}
