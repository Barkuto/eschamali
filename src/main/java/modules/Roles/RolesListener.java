package modules.Roles;

import base.ModuleListener;
import modules.BufferedMessage.BufferedMessage;
import modules.Channels.ChannelsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

import java.io.*;
import java.nio.Buffer;
import java.util.*;
import java.util.List;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RolesListener {
    private String prefix = ".";
    private String ownerID = "85844964633747456";

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        File autoroleF = new File("servers/" + event.getGuild().getName() + "-" + event.getGuild().getID() + "/autorole.txt");
        File selfrolesF = new File("servers/" + event.getGuild().getName() + "-" + event.getGuild().getID() + "/selfroles.txt");

        if (!autoroleF.exists()) {
            autoroleF.getParentFile().mkdirs();
            try {
                autoroleF.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!selfrolesF.exists()) {
            selfrolesF.getParentFile().mkdirs();
            try {
                selfrolesF.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventSubscriber
    public void newUserJoin(UserJoinEvent event) {
        if (ModuleListener.isModuleOn(event.getGuild(), RolesModule.name)) {
            File autoroleF = new File("servers/" + event.getGuild().getName() + "-" + event.getGuild().getID() + "/autorole.txt");
            if (autoroleF.exists()) {
                try {
                    Scanner s = new Scanner(autoroleF);
                    if (s.hasNextLine()) {
                        String line = s.nextLine();
                        IRole r = roleFromGuild(event.getGuild(), line);
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
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @EventSubscriber
    public void messageReceived(MessageReceivedEvent event) {
        if (ModuleListener.isModuleOn(event.getMessage().getGuild(), RolesModule.name) && ChannelsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel().getName())) {
            String message = event.getMessage().getContent();
            File autoroleF = new File("servers/" + event.getMessage().getGuild().getName() + "-" + event.getMessage().getGuild().getID() + "/autorole.txt");
            File selfrolesF = new File("servers/" + event.getMessage().getGuild().getName() + "-" + event.getMessage().getGuild().getID() + "/selfroles.txt");
            if (message.startsWith(prefix)) {
                IGuild guild = event.getMessage().getGuild();
                IUser author = event.getMessage().getAuthor();
                String[] args = message.split(" ");
                args[0] = args[0].substring(1, args[0].length());

                if (args[0].equalsIgnoreCase("autorole")) {
                    if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                        if (args.length > 1) {
                            String role = "";
                            for (int i = 1; i < args.length; i++) {
                                role += args[i] + " ";
                            }
                            role = role.trim();

                            IRole theRole = roleFromGuild(guild, role);

                            if (theRole != null) {
                                try {
                                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(autoroleF, false)));
                                    pw.println(theRole.getName());
                                    pw.close();
                                    BufferedMessage.sendMessage(RolesModule.client, event, theRole.getName() + " role has been added as the auto role.");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                BufferedMessage.sendMessage(RolesModule.client, event, "That is not a valid role.");
                            }
                        } else if (args.length == 1) {
                            try {
                                Scanner s = new Scanner(autoroleF);
                                if (s.hasNextLine()) {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "The current autorole is " + s.nextLine());
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "There is no autorole.");
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                    }
                } else if (args[0].equalsIgnoreCase("removeautorole")) {
                    if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                        if (autoroleF.exists()) {
                            try {
                                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(autoroleF, false)));
                                pw.close();
                                BufferedMessage.sendMessage(RolesModule.client, event, "Auto role has been removed.");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                    }
                } else if (args[0].equalsIgnoreCase("ar")) {//Add role to person
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
                                    theUser.addRole(theRole);
                                    BufferedMessage.sendMessage(RolesModule.client, event, theUser.mention() + " now has the " + '"' + theRole.getName() + '"' + " role.");
                                } catch (MissingPermissionsException e) {
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

                } else if (args[0].equalsIgnoreCase("rr")) {//Remove role from person
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
                                    theUser.removeRole(theRole);
                                    BufferedMessage.sendMessage(RolesModule.client, event, theUser.mention() + " now longer has the " + '"' + theRole.getName() + '"' + " role.");
                                } catch (MissingPermissionsException e) {
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
                } else if (args[0].equalsIgnoreCase("iam")) {//Add role to self, if self assignable
                    if (args.length > 1) {
                        String role = "";
                        for (int i = 1; i < args.length; i++) {
                            role += args[i] + " ";
                        }
                        role = role.trim();
                        List<IRole> roles = guild.getRoles();
                        for (IRole r : roles) {
                            if (r.getName().equalsIgnoreCase(role)) {
                                if (roleISA(guild, role)) {
                                    try {
                                        author.addRole(r);
                                        IMessage m = BufferedMessage.sendMessage(RolesModule.client, event, "You now the have the " + r.getName() + " role.");
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        m.delete();
                                        event.getMessage().delete();
                                    } catch (MissingPermissionsException e) {
                                        BufferedMessage.sendMessage(RolesModule.client, event, "Insufficient perms to add that role to yourself.");
                                        e.printStackTrace();
                                    } catch (RateLimitException e) {
                                        e.printStackTrace();
                                    } catch (DiscordException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "That role is not self assignable.");
                                }
                                return;
                            }
                        }
                    }

                } else if (args[0].equalsIgnoreCase("iamn")) {//Remove role from self, if self assignable
                    if (args.length > 1) {
                        String role = "";
                        for (int i = 1; i < args.length; i++) {
                            role += args[i] + " ";
                        }
                        role = role.trim();
                        List<IRole> roles = guild.getRoles();
                        for (IRole r : roles) {
                            if (r.getName().equalsIgnoreCase(role)) {
                                if (roleISA(guild, role)) {
                                    try {
                                        author.removeRole(r);
                                        IMessage m = BufferedMessage.sendMessage(RolesModule.client, event, "Removed " + r.getName() + " role from you.");
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        m.delete();
                                        event.getMessage().delete();
                                    } catch (MissingPermissionsException e) {
                                        BufferedMessage.sendMessage(RolesModule.client, event, "Insufficient perms to remove that role from yourself.");
                                        e.printStackTrace();
                                    } catch (RateLimitException e) {
                                        e.printStackTrace();
                                    } catch (DiscordException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "That role is not self assignable.");
                                }
                                return;
                            }
                        }
                    }

                } else if (args[0].equalsIgnoreCase("inrole")) {//Check people with given role
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
                            output = output.substring(0, output.length() - 2);
                            BufferedMessage.sendMessage(RolesModule.client, event, output);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("myroles")) {
                    IUser user = event.getMessage().getAuthor();
                    List<IRole> userRoles = user.getRolesForGuild(guild);
                    String output = "`A list of your roles, " + user.getName() + "#" + user.getDiscriminator() + ":`";
                    for (IRole r : userRoles) {
                        output += "\n•" + r.getName().replace("@", "");
                    }
                    BufferedMessage.sendMessage(RolesModule.client, event, output);
                } else if (args[0].equalsIgnoreCase("roles")) {
                    String output = "`List of roles:`";
                    List<IRole> roles = guild.getRoles();
                    for (IRole r : roles) {
                        output += "\n•" + r.getName().replace("@", "");
                    }
                    BufferedMessage.sendMessage(RolesModule.client, event, output);
                } else if (args[0].equalsIgnoreCase("rolesof") && args.length > 1) {
                    IUser user = guild.getUserByID(parseUserID(args[1]));
                    if (user != null) {
                        List<IRole> theirRoles = user.getRolesForGuild(guild);
                        String msg = "`List of roles for " + user.getName() + "#" + user.getDiscriminator() + ":`";
                        for (IRole r : theirRoles) {
                            msg += "\n•" + r.getName().replace("@", "") + "";
                        }
                        BufferedMessage.sendMessage(RolesModule.client, event, msg);
                    } else {
                        BufferedMessage.sendMessage(RolesModule.client, event, "Invalid parameter, please @ the user to see their roles.");
                    }
                } else if (args[0].equalsIgnoreCase("lsar")) {
                    int count = 0;
                    String msg = "";
                    if (selfrolesF.exists()) {
                        try {
                            Scanner s = new Scanner(selfrolesF);
                            while (s.hasNextLine()) {
                                msg += "**" + s.nextLine() + "**, ";
                                count++;
                            }
                            if (count > 0) {
                                msg = msg.substring(0, msg.length() - 2);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    String thingy1 = "are";
                    String thingy2 = "roles";
                    if (count == 1) {
                        thingy1 = "is";
                        thingy2 = "role";
                    }
                    BufferedMessage.sendMessage(RolesModule.client, event, "There " + thingy1 + " `" + count + "` self assignable " + thingy2 + ":\n" + msg);
                } else if (args[0].equalsIgnoreCase("asar") && args.length > 1) {
                    if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                        String role = "";
                        for (int i = 1; i < args.length; i++) {
                            role += args[i] + " ";
                        }
                        role = role.trim();

                        IRole theRole = roleFromGuild(guild, role);

                        if (theRole != null) {
                            if (!roleISA(guild, role)) {
                                try {
                                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(selfrolesF, true)));
                                    pw.println(theRole.getName());
                                    pw.close();
                                    BufferedMessage.sendMessage(RolesModule.client, event, "Role has been successfully added as self assignable.");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                BufferedMessage.sendMessage(RolesModule.client, event, "That role is already self assignable.");
                            }
                        } else {
                            BufferedMessage.sendMessage(RolesModule.client, event, "That is not a valid role.");
                        }
                    } else {
                        BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                    }
                } else if (args[0].equalsIgnoreCase("amsar")) {
                    if (args.length > 2) {
                        if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                            ArrayList<IRole> argRoles = new ArrayList<IRole>();
                            String argConcat = "";
                            for (int i = 1; i < args.length; i++) {
                                argConcat += args[i] + " ";
                            }
                            argConcat = argConcat.trim();

                            String[] roles = argConcat.split(";");
                            for (int i = 0; i < roles.length; i++) {
                                roles[i] = roles[i].trim();
                                IRole r = roleFromGuild(guild, roles[i]);
                                if (r != null) {
                                    argRoles.add(r);
                                }
                            }

                            ArrayList<IRole> addedRoles = new ArrayList<IRole>();
                            try {
                                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(selfrolesF, true)));

                                for (int i = 0; i < argRoles.size(); i++) {
                                    if (!roleISA(guild, argRoles.get(i).getName())) {
                                        pw.println(argRoles.get(i).getName());
                                        addedRoles.add(argRoles.get(i));
                                    }
                                }

                                pw.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            String output = "";
                            for (int i = 0; i < addedRoles.size(); i++) {
                                output += addedRoles.get(i).getName() + ", ";
                            }
                            output = output.trim();
                            BufferedMessage.sendMessage(RolesModule.client, event, "Successfully added `" + addedRoles.size() + "` roles as self assignable:\n" + output);
                        } else {
                            BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                        }
                    }
                } else if (args[0].equalsIgnoreCase("rsar")) {
                    if (args.length > 1) {
                        if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                            String role = "";
                            for (int i = 1; i < args.length; i++) {
                                role += args[i] + " ";
                            }
                            role = role.trim();

                            IRole theRole = roleFromGuild(guild, role);

                            if (theRole == null) {
                                BufferedMessage.sendMessage(RolesModule.client, event, "That is not a valid role.");
                            } else {
                                if (roleISA(guild, role)) {
                                    if (selfrolesF.exists()) {
                                        try {
                                            Scanner s = new Scanner(selfrolesF);
                                            ArrayList<IRole> currentRoles = new ArrayList<IRole>();
                                            List<IRole> roles = guild.getRoles();
                                            while (s.hasNextLine()) {
                                                String line = s.nextLine();

                                                for (IRole r : roles) {
                                                    if (!r.getName().equalsIgnoreCase(role) && roleISA(guild, r.getName()) && !currentRoles.contains(r)) {
                                                        currentRoles.add(r);
                                                    }
                                                }
                                            }

                                            PrintWriter pw = new PrintWriter(selfrolesF);
                                            pw.close();

                                            pw = new PrintWriter(new BufferedWriter(new FileWriter(selfrolesF, true)));
                                            for (IRole r : currentRoles) {
                                                pw.println(r.getName());
                                            }
                                            pw.close();
                                            BufferedMessage.sendMessage(RolesModule.client, event, "Role has been successfully removed as self assignable.");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        BufferedMessage.sendMessage(RolesModule.client, event, "There are no self assignable roles.");
                                    }
                                } else {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "That role is not self assignable.");
                                }
                            }
                        }
                    } else {
                        BufferedMessage.sendMessage(RolesModule.client, event, "You do not have permissions to manage roles.");
                    }
                }
            }
        }
    }


    public boolean roleISA(IGuild guild, String role) {//checks to see if role is self assignable
        try {
            File selfrolesF = new File("servers/" + guild.getName() + "-" + guild.getID() + "/selfroles.txt");
            Scanner s = new Scanner(selfrolesF);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.equalsIgnoreCase(role)) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
}
