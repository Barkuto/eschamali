package modules.Roles;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RolesModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "Roles";
    private RolesListener rl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        rl = new RolesListener();
        client.getDispatcher().registerListener(rl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(rl);
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
        return "2.6.0";
    }

    @Override
    public ArrayList<String> commands() {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(RolesListener.prefix);
        cmds.add("__**DESC**__: Manages user roles.");
        cmds.add("`autorole`: Sets the autorole for the server, requires MANAGE SERVER permission. **USAGE**: autorole <role>");
        cmds.add("`removeautorole`: Removes the current autorole, requires MANAGE SERVER permission. **USAGE**: removeautorole");
        cmds.add("`ar`: Adds a role to a user, requires MANAGE ROLES permission. **USAGE**: ar @user <role>");
        cmds.add("`rr`: Removes a role from a user, requires MANAGE ROLES permission. **USAGE**: rr @user <role>");
        cmds.add("`iam`: Adds a self-assignable role to yourself. **USAGE**: iam <role>");
        cmds.add("`iamn`: Removes a self assignable role from yourself. **USAGE**: iamn <role>");
        cmds.add("`inrole`: Lists people in a certain role. **USAGE**: inrole <role>");
        cmds.add("`myroles`: Lists your own roles. **USAGE**: myroles");
        cmds.add("`roles`: Lists the servers roles. **USAGE**: roles");
        cmds.add("`rolesof`: Lists roles of a user. **USAGE**: rolesof @user");
        cmds.add("`lsar`: Lists all self assignable roles, requires MANAGE ROLES permission. **USAGE**: lsar");
        cmds.add("`asar`: Allows a role to be self-assignable, requires MANAGE ROLES permission. **USAGE**: asar <role>");
        cmds.add("`amsar`: Adds multiple self assignable roles, requires MANAGE ROLES permission. **USAGE**: amsar <role1>;<role2>;<role3>");
        cmds.add("`rsar`: Removes a role from being self assignable, requires MANAGE ROLES permission. **USAGE**: rsar <role>");
        return cmds;
    }
}
