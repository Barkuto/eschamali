package base;

import modules.BufferedMessage.Sender;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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
                    Sender.sendMessage(event.getChannel(), url + query);
                }
            }
        }
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            if (PermissionsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel())) {
                String msg = event.getMessage().getContent().toLowerCase();
                IChannel channel = event.getChannel();
                if (msg.equalsIgnoreCase("!donate")) {
                    Sender.sendMessage(channel, "Donate for server/development funds at: https://www.twitchalerts.com/donate/barkuto");
                } else if (msg.equalsIgnoreCase("!maker")) {
                    Sender.sendMessage(channel, "Made by **Barkuto**#2315 specifically for Puzzle and Dragons servers. Code at https://github.com/Barkuto/Eschamali");
                } else if (msg.equalsIgnoreCase("!ayy")) {
                    List<IRole> roles = event.getMessage().getAuthor().getRolesForGuild(event.getMessage().getGuild());
                    if (Eschamali.ownerIDs.contains(event.getMessage().getAuthor().getLongID())) {
                        ayy = !ayy;
                        if (ayy) {
                            Sender.sendMessage(channel, "lmao!");
                        } else {
                            Sender.sendMessage(channel, "lmao...");
                        }
                    } else {
                        for (IRole r : roles) {
                            if (r.getPermissions().contains(Permissions.ADMINISTRATOR) || r.getPermissions().contains(Permissions.MANAGE_SERVER)) {
                                ayy = !ayy;
                                if (ayy) {
                                    Sender.sendMessage(channel, "lmao!");
                                } else {
                                    Sender.sendMessage(channel, "lmao...");
                                }
                                break;
                            }
                        }
                    }
                } else if (msg.equalsIgnoreCase("ayy") && ayy) {
                    Sender.sendMessage(channel, "lmao");
                } else if (msg.equalsIgnoreCase("!tilt")) {
                    Sender.sendMessage(channel, "*T* *I* *L* *T* *E* *D*");
                } else if (msg.equalsIgnoreCase("!riot")) {
                    Sender.sendMessage(channel, "ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ");
                } else if (msg.equalsIgnoreCase("!ping")) {
                    Sender.sendMessage(channel, "pong!");
                } else if (msg.equalsIgnoreCase("!alert")) {
                    Sender.sendMessage(channel, Eschamali.client.getUserByID(Eschamali.ownerIDs.get(0)).mention() + " is on his way! Eventually...");
                } else if (msg.startsWith("!say")) {
                    if (Eschamali.ownerIDs.contains(event.getMessage().getAuthor().getLongID())) {
                        event.getMessage().delete();
                        Sender.sendMessage(channel, msg.substring(msg.indexOf(" ")));
                    }
                } else if (msg.equalsIgnoreCase("!serverinfo") || msg.startsWith("!sinfo")) {
                    IGuild guild = event.getMessage().getGuild();
                    LocalDateTime creationDate = guild.getCreationDate();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.withTitle(guild.getName());
                    eb.withThumbnail(guild.getIconURL());
                    eb.withDesc("ID: " + guild.getLongID());
                    eb.appendField("Created", creationDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")), true);
                    eb.appendField("Server Age", DAYS.between(creationDate, LocalDateTime.now()) + " days", true);
                    eb.appendField("Region", guild.getRegion().getName(), true);
                    eb.appendField("Owner", guild.getOwner().mention(), true);
                    eb.appendField("Users", guild.getUsers().size() + "", true);
                    eb.appendField("Roles", guild.getRoles().size() + "", true);
                    eb.withColor(Color.WHITE);
                    EmbedObject embed = eb.build();

                    Sender.sendEmbed(channel, embed);
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
                            user = guild.getUserByID(Long.parseLong(id));
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
                        Sender.sendMessage(channel, "Could not find that user.");
                        return;
                    }
                    String name = user.getName();
                    String disc = user.getDiscriminator();
                    String nick = user.getNicknameForGuild(guild) != null ? user.getNicknameForGuild(guild) : "";
                    long id = user.getLongID();
                    String avatar = user.getAvatarURL();
                    LocalDateTime accCreated = user.getCreationDate();
                    LocalDateTime guildJoinDate = null;
                    List<IRole> roles = user.getRolesForGuild(guild);
                    EmbedBuilder eb = new EmbedBuilder();

                    try {
                        guildJoinDate = guild.getJoinTimeForUser(user);
                    } catch (DiscordException e) {
                        e.printStackTrace();
                    }

                    String statusType = "";
                    switch (user.getPresence().getStatus()) {
                        case ONLINE:
                            statusType = "Online";
                            break;
                        case OFFLINE:
                            statusType = "Offline";
                            break;
                        case IDLE:
                            statusType = "Idle";
                            break;
                        case DND:
                            statusType = "Do Not Disturb";
                            break;
                        case STREAMING:
                            statusType = "Streaming ";
                            break;
                        case UNKNOWN:
                            statusType = "Unknown";
                            break;
                    }

                    eb.withTitle(name + "#" + disc + (nick.length() > 0 ? " AKA " + nick : ""));
                    eb.withDesc("Status: " + (user.getPresence().getPlayingText().isPresent() ? "Playing " + user.getPresence().getPlayingText().get() : statusType));
                    eb.withThumbnail(avatar.replace(".jpg", ".gif"));
                    List<IUser> usersSortedByJoin = guild.getUsers();
                    usersSortedByJoin.sort((o1, o2) -> {
                        try {
                            return guild.getJoinTimeForUser(o1).compareTo(guild.getJoinTimeForUser(o2));
                        } catch (DiscordException e) {
                            e.printStackTrace();
                        }
                        return -2;
                    });
                    eb.withFooterText("Member #" + (usersSortedByJoin.indexOf(user) + 1) + " | " + "ID: " + id);
                    eb.appendField("Account Created", accCreated.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")) + "\n" + DAYS.between(accCreated, LocalDateTime.now()) + " days ago", true);
                    eb.appendField("Guild Joined", guildJoinDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")) + "\n" + DAYS.between(guildJoinDate, LocalDateTime.now()) + " days ago", true);
                    String rolesString = roles.toString().replace("[", "").replace("]", "");
                    int count = 0;
                    for (IRole r : roles) {
                        if (r == guild.getEveryoneRole()) {
                            count++;
                        }
                    }
                    if (count > 1) {
                        rolesString = rolesString.replaceFirst(", @everyone", "");
                    }
                    eb.appendField("Roles", rolesString, false);
                    eb.withColor(roles.get(0).getColor());
                    EmbedObject embed = eb.build();
                    Sender.sendEmbed(channel, embed);
                } else if (msg.startsWith("?eval")) {
                    try {
                        double ans = eval(msg.substring(msg.indexOf(" ") + 1));
                        Sender.sendMessage(channel, "`" + msg.substring(5, msg.length()).trim() + "` equals: " + ans);
                    } catch (RuntimeException e) {
                        Sender.sendMessage(channel, "Invalid expression.");
                    }
                }
            }
        }
    }

    @EventSubscriber
    public void helpMessages(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            if (PermissionsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel())) {
                String msg = event.getMessage().getContent();
                IChannel channel = event.getChannel();
                String[] args = msg.split(" ");
                if (msg.startsWith("!help")) {
                    if (args.length == 1) {
                        String output = "__Eschamali Bot commands - Prefix:__ !\n";
                        ArrayList<String> commands = new ArrayList<>();
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
                        commands.add("`?google`: Give a link to google based on your query. Not !?google **UASGE**: ?google <query>");
                        commands.add("`?eval`: Evaluate a simple math expression. Supports +,-,*,/,^,sqrt,sin,cos,tan. **USAGE**: ?eval <expression>");
                        Collections.sort(commands);
                        for (int i = 0; i < commands.size(); i++) {
                            output += commands.get(i) + "\n";
                        }
                        Sender.sendMessage(channel, output);
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
                            Sender.sendMessage(channel, output);
                        } else {
                            Sender.sendMessage(channel, "There is no module with that name.");
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

    //https://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('%')) x %= parseFactor(); // modulus
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch == 'e') {
                    nextChar();
                    x = Math.E;
                } else if (ch == 'p') {
                    nextChar();
                    if (ch == 'i') {
                        nextChar();
                        x = Math.PI;
                    } else throw new RuntimeException("Derp");
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else if (func.equals("asin")) x = Math.toDegrees(Math.asin(x));
                    else if (func.equals("acos")) x = Math.toDegrees(Math.acos(x));
                    else if (func.equals("atan")) x = Math.toDegrees(Math.atan(x));
                    else if (func.equals("floor")) x = Math.floor(x);
                    else if (func.equals("ceil")) x = Math.ceil(x);
                    else if (func.equals("rad")) x = Math.toRadians(x);
                    else if (func.equals("deg")) x = Math.toDegrees(x);
                    else if (func.equals("ln")) x = Math.log(x);
                    else if (func.equals("log")) x = Math.log10(x);
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}
