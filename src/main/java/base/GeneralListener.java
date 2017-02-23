package base;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Created by Iggie on 8/14/2016.
 */
public class GeneralListener {
    private boolean ayy = false;

    @EventSubscriber
    public void onMessageToGoogle(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            if (PermissionsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel())) {
                String msg = event.getMessage().getContent();
                if (msg.startsWith("?g ") || msg.startsWith("?google ")) {
                    String query = msg.substring(msg.indexOf(" ") + 1, msg.length());
                    query = query.replaceAll(" ", "+");
                    String url = "https://www.google.com/#q=";
                    BufferedMessage.sendMessage(Eschamali.client, event, url + query);
                }
            }
        }
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            if (PermissionsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel())) {
                String msg = event.getMessage().getContent();
                if (msg.equalsIgnoreCase("!donate")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "Donate for server/development funds at: https://www.twitchalerts.com/donate/barkuto");
                } else if (msg.equalsIgnoreCase("!maker")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "Made by **Barkuto**#2315 specifically for Puzzle and Dragons servers. Code at https://github.com/Barkuto/Eschamali");
                } else if (msg.equalsIgnoreCase("!ayy")) {
                    List<IRole> roles = event.getMessage().getAuthor().getRolesForGuild(event.getMessage().getGuild());
                    if (Eschamali.ownerIDs.contains(event.getMessage().getAuthor().getID())) {
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
                                if (ayy) {
                                    BufferedMessage.sendMessage(Eschamali.client, event, "lmao!");
                                } else {
                                    BufferedMessage.sendMessage(Eschamali.client, event, "lmao...");
                                }
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
                    BufferedMessage.sendMessage(Eschamali.client, event, Eschamali.client.getUserByID(Eschamali.ownerIDs.get(0)).mention() + " is on his way! Eventually...");
                } else if (msg.startsWith("!say")) {
                    if (Eschamali.ownerIDs.contains(event.getMessage().getAuthor().getID())) {
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
                } else if (msg.equalsIgnoreCase("!serverinfo") || msg.startsWith("!sinfo")) {
                    IGuild guild = event.getMessage().getGuild();
                    LocalDateTime creationDate = guild.getCreationDate();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.withTitle(guild.getName());
                    eb.withThumbnail(guild.getIconURL());
                    eb.withDesc("ID: " + guild.getID());
                    eb.appendField("Created", creationDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")), true);
                    eb.appendField("Server Age", DAYS.between(creationDate, LocalDateTime.now()) + " days", true);
                    eb.appendField("Region", guild.getRegion().getName(), true);
                    eb.appendField("Owner", guild.getOwner().mention(), true);
                    eb.appendField("Users", guild.getUsers().size() + "", true);
                    eb.appendField("Roles", guild.getRoles().size() + "", true);
                    eb.withColor(Color.WHITE);
                    EmbedObject embed = eb.build();

                    BufferedMessage.sendEmbed(Eschamali.client, event, embed);
                } else if (msg.startsWith("!userinfo") || msg.startsWith("!uinfo")) {
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
                    List<IRole> roles = user.getRolesForGuild(guild);
                    String status = user.getStatus().getStatusMessage();
                    EmbedBuilder eb = new EmbedBuilder();

                    try {
                        guildJoinDate = guild.getJoinTimeForUser(user);
                    } catch (DiscordException e) {
                        e.printStackTrace();
                    }

                    String statusType = "";
                    switch (user.getStatus().getType().ordinal()) {
                        case 0://GAME
                            statusType = "Playing ";
                            break;
                        case 1://STREAM
                            statusType = "Streaming ";
                            break;
                        case 2://NONE
                            break;
                    }

                    eb.withTitle(name + "#" + disc + (nick.length() > 0 ? " AKA " + nick : ""));
                    eb.withDesc("Status: " + (user.getStatus().getType() != Status.StatusType.NONE ? statusType + status : "None."));
                    eb.withThumbnail(avatar.replace(".jpg", ".gif"));
                    List<IUser> usersSortedByJoin = guild.getUsers();
                    usersSortedByJoin.sort(new Comparator<IUser>() {
                        @Override
                        public int compare(IUser o1, IUser o2) {
                            try {
                                return guild.getJoinTimeForUser(o1).compareTo(guild.getJoinTimeForUser(o2));
                            } catch (DiscordException e) {
                                e.printStackTrace();
                            }
                            return -2;
                        }
                    });
                    eb.withFooterText("Member #" + (usersSortedByJoin.indexOf(user) + 1) + " | " + "ID: " + id);
                    eb.appendField("Account Created", accCreated.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")) + "\n" + DAYS.between(accCreated, LocalDateTime.now()) + " days ago", true);
                    eb.appendField("Guild Joined", guildJoinDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")) + "\n" + DAYS.between(guildJoinDate, LocalDateTime.now()) + " days ago", true);
                    eb.appendField("Roles", roles.toString().replace("[", "").replace("]", ""), false);
                    eb.withColor(roles.get(0).getColor());
                    EmbedObject embed = eb.build();
                    BufferedMessage.sendEmbed(Eschamali.client, event, embed);
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
                        commands.add("`?google`: Give a link to google based on your query. Not !?google **UASGE** ?google <query>");
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

    public String timeBetween(LocalDateTime from, LocalDateTime to) {
        LocalDateTime fromDateTime = from;
        LocalDateTime toDateTime = to;

        LocalDateTime tempDateTime = LocalDateTime.from(fromDateTime);

        long years = tempDateTime.until(toDateTime, ChronoUnit.YEARS);
        tempDateTime = tempDateTime.plusYears(years);

        long months = tempDateTime.until(toDateTime, ChronoUnit.MONTHS);
        tempDateTime = tempDateTime.plusMonths(months);

        long days = tempDateTime.until(toDateTime, DAYS);
        tempDateTime = tempDateTime.plusDays(days);


        long hours = tempDateTime.until(toDateTime, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours(hours);

        long minutes = tempDateTime.until(toDateTime, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes(minutes);

        long seconds = tempDateTime.until(toDateTime, ChronoUnit.SECONDS);
        return (years > 0 ? (years > 1 ? years + " years, " : years + " year, ") : "") +
                (months > 0 ? (months > 1 ? months + " months, " : months + " month, ") : "") +
                (days > 0 ? (days > 1 ? days + " days, " : days + " day, ") : "") +
                (hours > 0 ? (hours > 1 ? hours + " hours, " : hours + " hour, ") : "") +
                (minutes > 0 ? (minutes > 1 ? minutes + " minutes" : minutes + " minute") : "");
    }
}
