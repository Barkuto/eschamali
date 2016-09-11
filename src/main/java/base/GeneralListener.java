package base;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Iggie on 8/14/2016.
 */
public class GeneralListener {
    private boolean ayy = false;

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        Eschamali.client.changeStatus(Status.game("with REM rates"));
    }

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
//        BufferedMessage.sendMessage(base.Eschamali.client, event, "Online.");
//        IGuild guild = event.getGuild();
//        try {
//            IPrivateChannel channel = Eschamali.client.getOrCreatePMChannel(Eschamali.client.getUserByID(Eschamali.ownerID));
//            channel.sendMessage(guild.getName());
//        } catch (DiscordException e) {
//            e.printStackTrace();
//        } catch (RateLimitException e) {
//            e.printStackTrace();
//        } catch (MissingPermissionsException e) {
//            e.printStackTrace();
//        }
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            if (PermissionsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel())) {
                String msg = event.getMessage().getContent();
                if (msg.equalsIgnoreCase("!donate")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "Donate for server/development funds at: https://www.twitchalerts.com/donate/barkuto");
                } else if (msg.equalsIgnoreCase("!maker")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "Made by **Barkuto**#2315 specifically for Puzzle and Dragons servers.");
                } else if (msg.equalsIgnoreCase("!ayy")) {
                    List<IRole> roles = event.getMessage().getAuthor().getRolesForGuild(event.getMessage().getGuild());
                    if (event.getMessage().getAuthor().getID().equals(Eschamali.ownerID)) {
                        ayy = !ayy;
                        if (ayy) {
                            BufferedMessage.sendMessage(Eschamali.client, event, "lmao!");
                        } else {
                            BufferedMessage.sendMessage(Eschamali.client, event, "lmao...");
                        }
                    } else {
                        for (IRole r : roles) {
                            if (r.getPermissions().contains(Permissions.ADMINISTRATOR) || r.getPermissions().contains(Permissions.MANAGE_SERVER)) {
                                ayy = !ayy;
                                break;
                            }
                        }
                    }
                } else if (msg.equalsIgnoreCase("ayy") && ayy) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "lmao");
                } else if (msg.equalsIgnoreCase("!tilt")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "*T* *I* *L* *T* *E* *D*");
                } else if (msg.equalsIgnoreCase("!riot")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ");
                } else if (msg.equalsIgnoreCase("!ping")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "pong!");
                } else if (msg.equalsIgnoreCase("!alert")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, Eschamali.client.getUserByID(Eschamali.ownerID).mention() + " is on his way! Eventually...");
                } else if (msg.startsWith("!say")) {
                    if (event.getMessage().getAuthor().getID().equals(Eschamali.ownerID)) {
                        try {
                            event.getMessage().delete();
                        } catch (MissingPermissionsException e) {
                        } catch (RateLimitException e) {
                            e.printStackTrace();
                        } catch (DiscordException e) {
                            e.printStackTrace();
                        }
                        BufferedMessage.sendMessage(Eschamali.client, event, msg.substring(msg.indexOf(" ")));
                    }
                } else if (msg.equalsIgnoreCase("!serverinfo")) {
                    IGuild guild = event.getMessage().getGuild();
                    String name = guild.getName();
                    String serverID = guild.getID();
                    LocalDateTime creationDate = guild.getCreationDate();
                    IRegion region = guild.getRegion();
                    IUser owner = guild.getOwner();
                    long users = guild.getUsers().size();
                    long roles = guild.getRoles().size();
                    String iconURL = guild.getIconURL();

                    String output = "```xl\n";
                    output += String.format("%-12s %s\n", "Server Name:", name);
                    output += String.format("%-12s %s\n", "Server ID:", serverID);
                    output += String.format("%-12s %s\n", "Icon URL:", iconURL);
                    output += String.format("%-12s %s\n", "Created:", creationDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
                    output += String.format("%-12s %s\n", "Region:", region.getName());
                    output += String.format("%-12s %s\n", "Owner:", owner.getName() + "#" + owner.getDiscriminator());
                    output += String.format("%-12s %s\n", "Users:", users);
                    output += String.format("%-12s %s\n", "Roles:", roles);
                    output += "```";
                    BufferedMessage.sendMessage(Eschamali.client, event, output);
                } else if (msg.startsWith("!userinfo")) {
                    IGuild guild = event.getMessage().getGuild();
                    IUser user = null;
                    if (msg.contains(" ")) {
                        String arg = msg.substring(msg.indexOf(" ") + 1).trim();
                        if (arg.startsWith("<@")) {
                            String id = "";
                            int startIndex = 2;
                            if (arg.startsWith("<@!")) {
                                startIndex++;
                            }
                            id += arg.substring(startIndex, arg.length() - 1);
                            user = guild.getUserByID(id);
                        } else {
                            List<IUser> users = guild.getUsers();
                            for (IUser u : users) {
                                System.out.println(u.getName());
                                if (u.getName().toLowerCase().contains(arg.toLowerCase())) {
                                    user = u;
                                    break;
                                }
                            }
                        }
                    } else {
                        user = event.getMessage().getAuthor();
                    }
                    if (user == null) {
                        BufferedMessage.sendMessage(Eschamali.client, event, "Could not find that user.");
                        return;
                    }
                    String name = user.getName();
                    String disc = user.getDiscriminator();
                    String nick = user.getNicknameForGuild(guild).isPresent() ? user.getNicknameForGuild(guild).get() : "";
                    String id = user.getID();
                    String avatar = user.getAvatarURL();
                    LocalDateTime accCreated = user.getCreationDate();
                    LocalDateTime guildJoinDate = null;
                    try {
                        guildJoinDate = guild.getJoinTimeForUser(user);
                    } catch (DiscordException e) {
                        e.printStackTrace();
                    }
                    List<IRole> roles = user.getRolesForGuild(guild);
                    String allRoles = "";
                    for (int i = 0; i < roles.size(); i++) {
                        allRoles += roles.get(i).getName() + ", ";
                    }
                    if (allRoles.length() > 0) {
                        allRoles = allRoles.substring(0, allRoles.lastIndexOf(", "));
                    }
                    String status = user.getStatus().getStatusMessage();
                    String output = "```xl\n";
                    output += String.format("%-16s %s\n", "Username:", name);
                    output += String.format("%-16s #%s\n", "Discriminator:", disc);
                    output += String.format("%-16s %s\n", "Nickname:", nick);
                    output += String.format("%-16s %s\n", "User ID:", id);
                    output += String.format("%-16s %s\n", "Avatar URL:", avatar);
                    output += String.format("%-16s %s\n", "Account Created:", accCreated.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
                    output += String.format("%-16s %s\n", "\nInfo For Guild: ", guild.getName());
                    output += String.format("%-16s %s\n", "Join Date:", guildJoinDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
                    output += String.format("%-16s %s\n", "Roles:", allRoles);
                    output += String.format("%-16s %s\n", "Status:", status);
                    output += "```";
                    BufferedMessage.sendMessage(Eschamali.client, event, output);
                }
            }
        }
    }

    @EventSubscriber
    public void helpMessages(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            if (PermissionsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel())) {
                String msg = event.getMessage().getContent();
                String[] args = msg.split(" ");
                if (msg.startsWith("!help") || msg.startsWith("!h")) {
                    if (args.length == 1) {
                        String output = "__Eschamali Bot commands - Prefix:__ !\n";
                        ArrayList<String> commands = new ArrayList<String>();
                        commands.add("`help`: Lists commands for certains parts of the bot. **USAGE**: help <module name>");
                        commands.add("`donate`: See where you can donate to fund development/server keep up.");
                        commands.add("`maker`: See who made me.");
                        commands.add("`ayy`: Enable ayy mode for the server, requires ADMIN/MANAGE SERVER perm");
                        commands.add("`tilt`: Send a message indicating you are tilted.");
                        commands.add("`riot`: riot.");
                        commands.add("`ping`: Visually check your ping with a pong.");
                        commands.add("`alert`: Alerts Barkuto that something went wrong!");
                        commands.add("`serverinfo`: Shows some information about the current server.");
                        commands.add("`userinfo`: Shows some information about yourself, or the given user.");
                        Collections.sort(commands);
                        for (int i = 0; i < commands.size(); i++) {
                            output += commands.get(i) + "\n";
                        }
                        BufferedMessage.sendMessage(Eschamali.client, event, output);
                    } else {
                        String module = "";
                        for (int i = 1; i < args.length; i++) {
                            module += args[i];
                        }
                        String output = "";
                        IModule theModule = null;
                        ArrayList<String> cmds = null;
                        for (IModule m : Eschamali.modules) {
                            if (m.getName().equalsIgnoreCase(module)) {
                                if (m instanceof ICommands) {
                                    theModule = m;
                                    cmds = ((ICommands) m).commands();
                                    break;
                                }
                            }
                        }
                        if (cmds != null) {
                            output += "__" + theModule.getName() + " module commands - Prefix:__ " + cmds.get(0) + "\n";
                            for (int i = 1; i < cmds.size(); i++) {
                                output += cmds.get(i) + "\n";
                            }
                            BufferedMessage.sendMessage(Eschamali.client, event, output);
                        } else {
                            BufferedMessage.sendMessage(Eschamali.client, event, "There is no module with that name.");
                        }
                    }
                }
            }
        }
    }
}
