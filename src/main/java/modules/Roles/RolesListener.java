package modules.Roles;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.Permission;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.RoleBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RolesListener {
    public static String prefix = ".";
    private String ownerID = "85844964633747456";

    private String miscTableName = "rolesmisc";
    private String miscCol1 = "field";
    private String miscCol2 = "roles";
    private String[] miscCols = new String[]{miscCol1, miscCol2};

    private String rolesTableName = "roles";
    private String rolesCol1 = "role";
    private String rolesCol2 = "general";

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        Permission perms = PermissionsListener.getPermissionDB(event.getGuild());
        if (!perms.tableExists(miscTableName)) {
            perms.createTable(miscTableName, miscCols, new String[]{"string", "string"}, false);
            perms.addPerms(miscTableName, miscCol1, "autorole", miscCol2, "");
            perms.addPerms(miscTableName, miscCol1, "selfroles", miscCol2, "");
        }
        perms.close();
    }

    @EventSubscriber
    public void newUserJoin(UserJoinEvent event) {
        Permission perms = PermissionsListener.getPermissionDB(event.getGuild());
        if (PermissionsListener.isModuleOn(event.getGuild(), RolesModule.name)) {
            if (perms.tableExists(miscTableName)) {
                String role = perms.getPerms(miscTableName, miscCol1, "autorole", miscCol2);
                IRole r = event.getGuild().getRoleByID(role);
                if (r != null) {
                    try {
                        event.getUser().addRole(r);
                    } catch (MissingPermissionsException e) {
                        e.printStackTrace();
                    } catch (RateLimitException e) {
                        e.printStackTrace();
                    } catch (DiscordException e) {
                        e.printStackTrace();
                    }
                }
            }
            perms.close();
        }
    }

    @EventSubscriber
    public void messageReceived(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            String message = event.getMessage().getContent();
            IUser author = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, RolesModule.name) && PermissionsListener.canModuleInChannel(guild, RolesModule.name, channel)) {
                if (message.startsWith(prefix)) {
                    String[] args = message.split(" ");
                    args[0] = args[0].replace(prefix, "").trim();
                    String cmd = args[0];
                    String argsconcat;
                    try {
                        argsconcat = message.substring(cmd.length() + 2, message.length()).trim();
                    } catch (StringIndexOutOfBoundsException e) {
                        argsconcat = "";
                    }

                    Permission perms = PermissionsListener.getPermissionDB(guild);

                    if (cmd.equalsIgnoreCase("db")) {
                        if (userHasPerm(event.getMessage().getAuthor(), event.getMessage().getGuild(), Permissions.MANAGE_SERVER) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                            BufferedMessage.sendMessage(RolesModule.client, event, databaseString(guild));
                        }
                    } else if (cmd.equalsIgnoreCase("autorole")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_SERVER) || userHasPerm(author, guild, Permissions.MANAGE_ROLES) || author.getID().equals(ownerID)) {
                            if (args.length == 1) {
                                String output = "The current autorole is: ";
                                IRole role = guild.getRoleByID(perms.getPerms(miscTableName, miscCol1, "autorole", miscCol2));
                                if (role != null) {
                                    output += role.getName();
                                    BufferedMessage.sendMessage(RolesModule.client, event, output);
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "There is no auto role.");
                                }
                            } else {
                                IRole role = roleFromGuild(guild, argsconcat);
                                if (role != null) {
                                    perms.setPerms(miscTableName, miscCol1, "autorole", miscCol2, role.getID());
                                    BufferedMessage.sendMessage(RolesModule.client, event, "The auto-role is now \"" + role.getName() + "\"");
                                }
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("removeautorole")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_SERVER) || userHasPerm(author, guild, Permissions.MANAGE_ROLES) || author.getID().equals(ownerID)) {
                            perms.setPerms(miscTableName, miscCol1, "autorole", miscCol2, "");
                            BufferedMessage.sendMessage(RolesModule.client, event, "The auto-role has been removed.");
                        }
                    } else if (cmd.equalsIgnoreCase("ar")) {//Add role to person
                        if (args.length > 2) {
                            if (userHasPerm(author, guild, Permissions.MANAGE_ROLES) || author.getID().equals(ownerID) && args.length > 2) {
                                String user = args[1];
                                String role = "";
                                for (int i = 2; i < args.length; i++) {
                                    role += args[i] + " ";
                                }
                                role = role.trim();

                                IUser theUser = guild.getUserByID(parseUserID(user));
                                IRole theRole = roleFromGuild(guild, role);

                                if (theRole != null) {
                                    try {
                                        fixRoles(author, guild, channel);
                                        theUser.addRole(theRole);
                                        BufferedMessage.sendMessage(RolesModule.client, event, theUser.mention() + " now has the " + '"' + theRole.getName() + '"' + " role.");
                                    } catch (MissingPermissionsException e) {
                                        BufferedMessage.sendMessage(RolesModule.client, event, "I cannot add a role to that user because they have a role higher than mine.");
                                        e.printStackTrace();
                                    } catch (RateLimitException e) {
                                        e.printStackTrace();
                                    } catch (DiscordException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("rr")) {//Remove role from person
                        if (args.length > 2) {
                            if (userHasPerm(author, guild, Permissions.MANAGE_ROLES) || author.getID().equals(ownerID)) {
                                String user = args[1];
                                String role = "";
                                for (int i = 2; i < args.length; i++) {
                                    role += args[i] + " ";
                                }
                                role = role.trim();

                                IUser theUser = guild.getUserByID(parseUserID(user));
                                IRole theRole = roleFromGuild(guild, role);

                                if (theRole != null) {
                                    try {
                                        fixRoles(author, guild, channel);
                                        theUser.removeRole(theRole);
                                        BufferedMessage.sendMessage(RolesModule.client, event, theUser.mention() + " now longer has the " + '"' + theRole.getName() + '"' + " role.");
                                    } catch (MissingPermissionsException e) {
                                        BufferedMessage.sendMessage(RolesModule.client, event, "I cannot remove a role from that user because they have a role higher than mine.");
                                        e.printStackTrace();
                                    } catch (RateLimitException e) {
                                        e.printStackTrace();
                                    } catch (DiscordException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("iam")) {//Add role to self, if self assignable
                        if (args.length >= 1) {
                            IRole role = null;
                            for (IRole r : guild.getRoles()) {
                                if (r.getName().equalsIgnoreCase(argsconcat.trim())) {
                                    role = r;
                                }
                            }
                            if (role != null) {
                                if (roleISA(guild, role)) {
                                    try {
                                        fixRoles(author, guild, channel);
                                        author.addRole(role);
                                        IMessage m = BufferedMessage.sendMessage(RolesModule.client, event, "You now the have the " + role.getName() + " role.");
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        m.delete();
                                        event.getMessage().delete();
                                        return;
                                    } catch (MissingPermissionsException e) {
                                        if (e.getErrorMessage().contains("Missing permissions")) {
                                            BufferedMessage.sendMessage(RolesModule.client, event, "I do not have the proper permissions to add a role to you.");
                                        } else if (e.getErrorMessage().contains("Edited roles hierarchy is too high")) {
                                            BufferedMessage.sendMessage(RolesModule.client, event, "I cannot add a role to you because you have a role that is higher than me.");
                                        } else {
                                            e.printStackTrace();
                                        }
                                    } catch (RateLimitException e) {
                                        e.printStackTrace();
                                    } catch (DiscordException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "That role is not self assignable.");
                                    return;
                                }
                            }
                            BufferedMessage.sendMessage(RolesModule.client, event, "That is not a role!");
                        }
                    } else if (cmd.equalsIgnoreCase("iamn") || cmd.equalsIgnoreCase("iamnot")) {//Remove role from self, if self assignable
                        if (args.length > 1) {
                            //check if role is a role
                            //check if user has role
                            //remove role
                            IRole role = null;
                            for (IRole r : guild.getRoles()) {
                                if (r.getName().equalsIgnoreCase(argsconcat.trim())) {
                                    role = r;
                                }
                            }
                            if (role != null) {
                                boolean hasRole = false;
                                for (IRole r : author.getRolesForGuild(guild)) {
                                    if (r.getID().equalsIgnoreCase(role.getID())) {
                                        hasRole = true;
                                        break;
                                    }
                                }
                                if (hasRole) {
                                    if (roleISA(guild, role)) {
                                        try {
                                            fixRoles(author, guild, channel);
                                            author.removeRole(role);
                                            IMessage m = BufferedMessage.sendMessage(RolesModule.client, event, "Removed " + role.getName() + " role from you.");
                                            try {
                                                Thread.sleep(2000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            m.delete();
                                            event.getMessage().delete();
                                            return;
                                        } catch (MissingPermissionsException e) {
                                            if (e.getErrorMessage().contains("Missing permissions")) {
                                                BufferedMessage.sendMessage(RolesModule.client, event, "I do not have the proper permissions to remove a role from you.");
                                            } else if (e.getErrorMessage().contains("Edited roles hierarchy is too high")) {
                                                BufferedMessage.sendMessage(RolesModule.client, event, "I cannot remove a role from you because you have a role that is higher than me.");
                                            } else {
                                                e.printStackTrace();
                                            }
                                        } catch (RateLimitException e) {
                                            e.printStackTrace();
                                        } catch (DiscordException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        BufferedMessage.sendMessage(RolesModule.client, event, "That role is not self assignable.");
                                        return;
                                    }
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "You do not have that role.");
                                    return;
                                }
                            }
                            BufferedMessage.sendMessage(RolesModule.client, event, "That is not a role!");
                        }
                    } else if (cmd.equalsIgnoreCase("inrole")) {//Check people with given role
                        if (args.length > 1) {
                            String role = "";
                            for (int i = 1; i < args.length; i++) {
                                role += args[i] + " ";
                            }
                            role = role.trim();

                            Collection<IRole> roles = guild.getRoles();
                            IRole irole = roleFromGuild(guild, role);
                            if (irole == null) {
                                BufferedMessage.sendMessage(RolesModule.client, event, "That is not a role");
                            } else {
                                String output = "`Here is a list of people in the role " + '"' + irole.getName() + '"' + ":`\n";
                                List<IUser> users = event.getMessage().getGuild().getUsers();
                                for (IUser u : users) {
                                    List<IRole> userRoles = u.getRolesForGuild(guild);
                                    for (IRole r : userRoles) {
                                        if (r.getName().equalsIgnoreCase(irole.getName())) {
                                            output += "**" + u.getName() + "**#" + u.getDiscriminator() + ", ";
                                        }
                                    }
                                }
                                if (output.charAt(output.length() - 2) == ',') {
                                    output = output.substring(0, output.length() - 2);
                                }
                                BufferedMessage.sendMessage(RolesModule.client, event, output);
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("myroles")) {
                        fixRoles(author, guild, channel);
                        IUser user = event.getMessage().getAuthor();
                        List<IRole> userRoles = user.getRolesForGuild(guild);
                        String output = "`A list of your roles, " + user.getName() + "#" + user.getDiscriminator() + ":`";
                        for (IRole r : userRoles) {
                            if (userHasPerm(RolesModule.client.getOurUser(), guild, Permissions.MENTION_EVERYONE)) {
                                output += "\n•" + r.getName().replace("@", "");
                            } else {
                                output += "\n•" + r.getName();
                            }
                        }
                        BufferedMessage.sendMessage(RolesModule.client, event, output);
                    } else if (cmd.equalsIgnoreCase("roles")) {
                        String output = "`List of roles:`";
                        List<IRole> roles = guild.getRoles();
                        for (IRole r : roles) {
                            if (userHasPerm(RolesModule.client.getOurUser(), guild, Permissions.MENTION_EVERYONE)) {
                                output += "\n•" + r.getName().replace("@", "");
                            } else {
                                output += "\n•" + r.getName();
                            }
                        }
                        BufferedMessage.sendMessage(RolesModule.client, event, output);
                    } else if (cmd.equalsIgnoreCase("rolesof") && args.length > 1) {
                        IUser user = guild.getUserByID(parseUserID(args[1]));
                        if (user != null) {
                            fixRoles(user, guild, channel);
                            List<IRole> theirRoles = user.getRolesForGuild(guild);
                            String msg = "`List of roles for " + user.getName() + "#" + user.getDiscriminator() + ":`";
                            for (IRole r : theirRoles) {
                                if (userHasPerm(RolesModule.client.getOurUser(), guild, Permissions.MENTION_EVERYONE)) {
                                    msg += "\n•" + r.getName().replace("@", "") + "";
                                } else {
                                    msg += "\n•" + r.getName() + "";
                                }
                            }
                            BufferedMessage.sendMessage(RolesModule.client, event, msg);
                        } else {
                            BufferedMessage.sendMessage(RolesModule.client, event, "Invalid parameter, please @ the user to see their roles.");
                        }
                    } else if (cmd.equalsIgnoreCase("lsar")) {
                        String selfroles = perms.getPerms(miscTableName, miscCol1, "selfroles", miscCol2);
                        int count = 0;
                        String output = "";
                        if (selfroles != null && selfroles.length() > 0) {
                            String[] roles = selfroles.split(";");
                            Arrays.sort(roles);
                            for (int i = 0; i < roles.length; i++) {
                                IRole r = guild.getRoleByID(roles[i]);
                                if (r != null) {
                                    output += "**" + r.getName() + "**, ";
                                    count++;
                                }
                            }
                            if (count > 0) {
                                output = output.substring(0, output.length() - 2);
                            }
                        }
                        String thingy1 = "are";
                        String thingy2 = "roles";
                        if (count == 1) {
                            thingy1 = "is";
                            thingy2 = "role";
                        }
                        BufferedMessage.sendMessage(RolesModule.client, event, "There " + thingy1 + " `" + count + "` self assignable " + thingy2 + ":\n" + output);
                    } else if (cmd.equalsIgnoreCase("asar")) {
                        if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                            if (args.length > 1 && !message.contains(";")) {
                                IRole role = roleFromGuild(guild, argsconcat.trim());
                                if (role != null) {
                                    if (!roleISA(guild, role)) {
                                        perms.addPerms(miscTableName, miscCol1, "selfroles", miscCol2, role.getID());
                                        BufferedMessage.sendMessage(RolesModule.client, event, "Role has been successfully added as self assignable.");
                                    } else {
                                        BufferedMessage.sendMessage(RolesModule.client, event, "That role is already self assignable.");
                                    }
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "That is not a valid role.");
                                }
                            } else {
                                String[] roles = argsconcat.trim().split(";");
                                ArrayList<IRole> rolesToAdd = new ArrayList<IRole>();
                                for (int i = 0; i < roles.length; i++) {
                                    IRole r = roleFromGuild(guild, roles[i].trim());
                                    if (r != null) {
                                        rolesToAdd.add(r);
                                    }
                                }

                                int added = 0;
                                String output = "";
                                for (IRole r : rolesToAdd) {
                                    if (!roleISA(guild, r)) {
                                        perms.addPerms(miscTableName, miscCol1, "selfroles", miscCol2, r.getID());
                                        output += r.getName() + ", ";
                                        added++;
                                    }
                                }
                                if (added > 0) {
                                    output = output.substring(0, output.length() - 2).trim();
                                }
                                BufferedMessage.sendMessage(RolesModule.client, event, "Added `" + added + "` role" + (added == 1 ? "" : "s") + " as self assignable:\n" + output);
                            }
                        } else {
                            BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                        }
                    } else if (cmd.equalsIgnoreCase("rsar")) {
                        if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                            if (args.length > 1 && !message.contains(";")) {
                                IRole role = roleFromGuild(guild, argsconcat.trim());
                                if (role != null) {
                                    if (roleISA(guild, role)) {
                                        String[] csar = perms.getPerms(miscTableName, miscCol1, "selfroles", miscCol2).split(";");
                                        String nsar = "";
                                        for (int i = 0; i < csar.length; i++) {
                                            if (!csar[i].equalsIgnoreCase(role.getID())) {
                                                nsar += csar[i] + ";";
                                            }
                                        }
                                        if (nsar.length() > 0) {
                                            nsar = nsar.substring(0, nsar.lastIndexOf(";"));
                                        }
                                        perms.setPerms(miscTableName, miscCol1, "selfroles", miscCol2, nsar);
                                        BufferedMessage.sendMessage(RolesModule.client, event, "Role has been successfully removed from being self assignable.");
                                    } else {
                                        BufferedMessage.sendMessage(RolesModule.client, event, "That role is not self assignable.");
                                    }
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "That is not a valid role.");
                                }
                            } else {
                                String[] rolesToRemove = argsconcat.trim().split(";");
                                List<String> csar = Arrays.asList(perms.getPerms(miscTableName, miscCol1, "selfroles", miscCol2).split(";"));
                                ArrayList<IRole> removeRoles = new ArrayList<IRole>();
                                for (int i = 0; i < rolesToRemove.length; i++) {
                                    IRole r = roleFromGuild(guild, rolesToRemove[i]);
                                    if (r != null) {
                                        removeRoles.add(r);
                                    }
                                }
                                ArrayList<String> nsar = new ArrayList<String>();
                                int removed = 0;
                                String output = "";
                                for (int i = 0; i < csar.size(); i++) {
                                    boolean remove = false;
                                    for (int j = 0; j < removeRoles.size(); j++) {
                                        if (csar.get(i).equalsIgnoreCase(removeRoles.get(j).getID())) {
                                            removed++;
                                            remove = true;
                                            output += guild.getRoleByID(removeRoles.get(j).getID()).getName() + ", ";
                                        }
                                    }
                                    if (!remove) {
                                        nsar.add(csar.get(i));
                                    }
                                }
                                if (output.length() > 0) {
                                    output = output.substring(0, output.lastIndexOf(","));
                                }

                                String newPerm = "";
                                for (int i = 0; i < nsar.size(); i++) {
                                    newPerm += nsar.get(i) + ";";
                                }
                                if (newPerm.length() > 0) {
                                    newPerm = newPerm.substring(0, newPerm.lastIndexOf(";"));
                                }
                                perms.setPerms(miscTableName, miscCol1, "selfroles", miscCol2, newPerm);
                                BufferedMessage.sendMessage(RolesModule.client, event, "Removed `" + removed + "` role" + (removed == 1 ? "" : "s") + " from being self assignable:\n" + output);
                            }
                        } else {
                            BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                        }
                    } else if (cmd.equalsIgnoreCase("servertree")) {
                        if (author.getID().equalsIgnoreCase(ownerID)) {
                            List<IRole> roles = RolesModule.client.getGuildByID(guild.getID()).getRoles();
                            roles.sort(new Comparator<IRole>() {
                                @Override
                                public int compare(IRole o1, IRole o2) {
                                    return Integer.compare(o2.getPosition(), o1.getPosition());
                                }
                            });
                            IChannel pm = null;
                            try {
                                pm = RolesModule.client.getOrCreatePMChannel(author);
                                String s = "";
                                for (IRole r : roles) {
                                    s += r.getName() + "\n";
                                }
                                BufferedMessage.sendMessage(RolesModule.client, pm, s);
                            } catch (DiscordException e) {
                                e.printStackTrace();
                            } catch (RateLimitException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    perms.close();
                }
            }
        }
    }

    public String databaseString(IGuild guild) {
        //rolemisc:field|roles
        //         autorole
        //         selfroles
        //roles:role|general|roles|pad|music
        String s = "```\n";
        s += miscTableName + "\n";
        Permission perms = PermissionsListener.getPermissionDB(guild);
        ResultSet rs = perms.selectAllFrom(miscTableName);
        try {
            while (rs.next()) {
                s += rs.getString(miscCol1) + ": " + rs.getString(miscCol2) + "\n";
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        perms.close();
        return s + "\n```";
    }


    public boolean roleISA(IGuild guild, IRole role) {//checks to see if role is self assignable
        Permission perms = PermissionsListener.getPermissionDB(guild);
        String selfroles = perms.getPerms(miscTableName, miscCol1, "selfroles", miscCol2);
        if (selfroles != null && selfroles.length() > 0) {
            String[] split = selfroles.split(";");
            for (int i = 0; i < split.length; i++) {
                if (split[i].equalsIgnoreCase(role.getID())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean userHasPerm(IUser user, IGuild guild, Permissions perm) {
        List<IRole> roles = user.getRolesForGuild(guild);
        for (IRole r : roles) {
            if (r.getPermissions().contains(perm)) {
                return true;
            }
        }
        return false;
    }

    public IRole roleFromGuild(IGuild guild, String roleName) {
        List<IRole> roles = guild.getRoles();
        for (IRole r : roles) {
            if (r.getName().equalsIgnoreCase(roleName)) {
                return r;
            }
        }
        return null;
    }

    public String parseUserID(String arg) {
        String id = "";
        int startIndex = 2;
        if (arg.startsWith("<@!")) {
            startIndex++;
        }
        id += arg.substring(startIndex, arg.length() - 1);
        return id;
    }

    private boolean fixRoles(IUser user, IGuild guild, IChannel channel) {
        try {
            IRole tmpRole = new RoleBuilder(guild).withName("tmp").build();
            user.addRole(tmpRole);
            user.removeRole(tmpRole);
            tmpRole.delete();
        } catch (MissingPermissionsException e) {
            e.printStackTrace();
            return false;
        } catch (RateLimitException e) {
            e.printStackTrace();
            return false;
        } catch (DiscordException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
