package modules.Roles;

import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.*;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RolesListener {
    private String prefix = ".";

    @EventSubscriber
    public void messageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        if (message.startsWith(prefix)) {
            IGuild guild = event.getMessage().getGuild();
            String[] args = message.split(" ");
            args[0] = args[0].substring(1, args[0].length());
            if (args[0].equalsIgnoreCase("ar")) {//Add role to person

            } else if (args[0].equalsIgnoreCase("rr")) {//Remove role from person

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
                                    event.getMessage().getAuthor().addRole(r);
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
                    IRole irole = null;

                    for (IRole r : roles) {
                        if (r.getName().equalsIgnoreCase(role)) {
                            irole = r;
                        }
                    }
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
                    output += "\n-" + r.getName().replace("@", "");
                }
                BufferedMessage.sendMessage(RolesModule.client, event, output);
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
}
