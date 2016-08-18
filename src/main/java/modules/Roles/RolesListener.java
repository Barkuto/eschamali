package modules.Roles;

import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.*;

import java.awt.*;
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
    public void messageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        if (message.startsWith(prefix)) {
            IGuild guild = event.getMessage().getGuild();
            IUser author = event.getMessage().getAuthor();
            String[] args = message.split(" ");
            args[0] = args[0].substring(1, args[0].length());

            if (args[0].equalsIgnoreCase("ar")) {//Add role to person
                if (userHasPerm(author, guild, Permissions.MANAGE_ROLES) || author.getID().equals(ownerID)) {
                    String user = args[1];
                    String role = "";
                    for (int i = 2; i < args.length; i++) {
                        role += args[i] + " ";
                    }
                    role = role.trim();

                    IUser theUser = guild.getUserByID(user.substring(1, user.length() - 1).replace("@", ""));
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
            } else if (args[0].equalsIgnoreCase("rr")) {//Remove role from person
                if (userHasPerm(author, guild, Permissions.MANAGE_ROLES) || author.getID().equals(ownerID)) {
                    String user = args[1];
                    String role = "";
                    for (int i = 2; i < args.length; i++) {
                        role += args[i] + " ";
                    }
                    role = role.trim();

                    IUser theUser = guild.getUserByID(user.substring(1, user.length() - 1).replace("@", ""));
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
            } else if (args[0].equalsIgnoreCase("iam")) {//Add role to self, if self assignable
                if (args.length > 1) {
                    String role = "";
                    for (int i = 1; i < args.length; i++) {
                        role += args[i] + " ";
                    }
                    role = role.trim();
                    Collection<IRole> roles = RolesModule.client.getRoles();
                    for (IRole r : roles) {
                        if (r.getName().equalsIgnoreCase(role)) {
                            if (roleISA(guild.getID(), role)) {
                                try {
                                    author.addRole(r);
                                    BufferedMessage.sendMessage(RolesModule.client, event, "You now the have the " + r.getName() + " role.");
                                } catch (MissingPermissionsException e) {
                                    BufferedMessage.sendMessage(RolesModule.client, event, "Your roles are too high to add that role to yourself.");
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
                    Collection<IRole> roles = RolesModule.client.getRoles();
                    for (IRole r : roles) {
                        if (r.getName().equalsIgnoreCase(role)) {
                            if (roleISA(guild.getID(), role)) {
                                try {
                                    event.getMessage().getAuthor().removeRole(r);
                                    BufferedMessage.sendMessage(RolesModule.client, event, "Removed " + r.getName() + " role from you.");
                                } catch (MissingPermissionsException e) {
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

                    Collection<IRole> roles = RolesModule.client.getRoles();
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
                String output = "`A list of your roles for " + user.getName() + "#" + user.getDiscriminator() + ":`";
                for (IRole r : userRoles) {
                    System.out.println(r.getName());
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
            } else if (args[0].equalsIgnoreCase("lsar")) {
                File f = new File("servers/" + guild.getID() + "/selfroles.txt");
                int count = 0;
                String msg = "";
                if (f.exists()) {
                    try {
                        Scanner s = new Scanner(f);
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
            } else if (args[0].equalsIgnoreCase("asar")) {
                if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                    File f = new File("servers/" + guild.getID() + "/selfroles.txt");
                    if (!f.exists()) {
                        f.getParentFile().mkdirs();
                        try {
                            f.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    String role = "";
                    for (int i = 1; i < args.length; i++) {
                        role += args[i] + " ";
                    }
                    role = role.trim();

                    IRole theRole = roleFromGuild(guild, role);

                    if (theRole != null) {
                        if (!roleISA(guild.getID(), role)) {
                            try {
                                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                                pw.println(theRole.getName());
                                pw.close();
                                BufferedMessage.sendMessage(RolesModule.client, event, "Role has been successfully added as self assignable.");
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
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
            } else if (args[0].equalsIgnoreCase("rsar") || event.getMessage().getAuthor().getID().equals(ownerID)) {
                if (userHasPerm(event.getMessage().getAuthor(), guild, Permissions.MANAGE_ROLES)) {
                    String role = "";
                    for (int i = 1; i < args.length; i++) {
                        role += args[i] + " ";
                    }
                    role = role.trim();

                    IRole theRole = roleFromGuild(guild, role);

                    if (theRole == null) {
                        BufferedMessage.sendMessage(RolesModule.client, event, "That is not a valid role.");
                    } else {
                        if (roleISA(guild.getID(), role)) {
                            File f = new File("servers/" + guild.getID() + "/selfroles.txt");
                            if (f.exists()) {
                                try {
                                    Scanner s = new Scanner(f);
                                    ArrayList<IRole> currentRoles = new ArrayList<IRole>();
                                    List<IRole> roles = guild.getRoles();
                                    while (s.hasNextLine()) {
                                        String line = s.nextLine();

                                        for (IRole r : roles) {
                                            if (!r.getName().equalsIgnoreCase(role) && roleISA(guild.getID(), r.getName()) && !currentRoles.contains(r)) {
                                                currentRoles.add(r);
                                            }
                                        }
                                    }
                                    System.out.println(currentRoles);

                                    PrintWriter pw = new PrintWriter(f);
                                    pw.close();

                                    pw = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
                                    for (IRole r : currentRoles) {
                                        pw.println(r.getName());
                                    }
                                    pw.close();
                                    BufferedMessage.sendMessage(RolesModule.client, event, "Role has been successfully removed as self assignable.");
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
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


    public boolean roleISA(String guildID, String role) {//checks to see if role is self assignable
        File f = new File("servers/" + guildID + "/selfroles.txt");
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        } else {
            try {
                Scanner s = new Scanner(f);
                while (s.hasNextLine()) {
                    String line = s.nextLine();
                    if (line.equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
}
