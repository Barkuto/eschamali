package base;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import modules.ChannelPerms;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

public class General extends Module {
    private boolean ayy = false;

    public General(DiscordClient client) {
        super(client, "!");
    }

    @Override
    protected Map<String, Command> makeCommands() {
        Map<String, Command> commands = new HashMap<>();

        Command google = event -> {
            if (!ChannelPerms.canTalkInChannel(event.getGuild().block(), event.getMessage().getChannel().block()))
                return Mono.empty();

            String msg = EschaUtil.getMessage(event);
            if (msg.length() > 0) {
                String query = msg.substring(msg.indexOf(" ") + 1);
                query = query.replaceAll(" ", "+");
                String url = "https://www.google.com/#q=" + query;
                return EschaUtil.sendMessage(event, url);
            }
            return Mono.empty();
        };

        Command eval = event -> {
            if (!ChannelPerms.canTalkInChannel(event.getGuild().block(), event.getMessage().getChannel().block()))
                return Mono.empty();

            String msg = EschaUtil.getMessage(event);
            try {
                double ans = eval(msg.substring(msg.indexOf(" ") + 1));
                return EschaUtil.sendMessage(event, "`" + msg.substring(5).trim() + "` equals: " + ans);
            } catch (RuntimeException e) {
                return EschaUtil.sendMessage(event, "Invalid expression.");
            }
        };

        Command serverinfo = event -> {
            if (!ChannelPerms.canTalkInChannel(event.getGuild().block(), event.getMessage().getChannel().block()))
                return Mono.empty();
            String[] args = EschaUtil.getArgs(event);
            Guild guild = null;
            if (args.length == 0) {
                guild = event.getGuild().block();
            } else {
                List<Guild> guilds = client.getGuilds().collectList().block();
                for (Guild g : guilds) {
                    if (g.getId().asString().equalsIgnoreCase(args[0])) {
                        guild = g;
                        break;
                    }
                }
                if (guild == null) return EschaUtil.sendMessage(event, "Invalid Guild ID, or I am not in that server.");
            }
            Guild fGuild = guild;
            return event.getMessage().getChannel().flatMap(channel -> channel.createEmbed(e -> serverInfoEmbedSpec(e, fGuild)).then());
        };

        Command userinfo = event -> {
            if (!ChannelPerms.canTalkInChannel(event.getGuild().block(), event.getMessage().getChannel().block()))
                return Mono.empty();
            String[] args = EschaUtil.getArgs(event);
            String argsconcat = EschaUtil.getArgsConcat(event);
            Guild guild = event.getGuild().block();
            Member member = null;
            if (args.length == 0) {
                member = event.getMember().get();
            } else {
                Set<Snowflake> mentions = event.getMessage().getUserMentionIds();
                Iterator<Snowflake> iterator = mentions.iterator();
                if (iterator.hasNext()) {
                    member = guild.getMemberById(iterator.next()).block();
                } else {
                    List<Member> members = guild.getMembers().collectList().block();
                    for (Member m : members) {
                        if (m.getId().asString().equalsIgnoreCase(args[0])
                                || m.getDisplayName().equalsIgnoreCase(argsconcat)
                                || (m.getNickname().isPresent() && m.getNickname().get().equalsIgnoreCase(argsconcat))) {
                            member = m;
                            break;
                        }
                    }
                }
                if (member == null) return EschaUtil.sendMessage(event, "Invalid ID, name, or nickname");
            }
            Member fMember = member;
            return event.getMessage().getChannel().flatMap(channel -> channel.createEmbed(e -> userInfoEmbedSpec(e, guild, fMember)).then());
        };

        commands.put(prefix + "donate", EschaUtil.createMessageCommandGen("Donate for server/development funds at: https://streamlabs.com/barkuto"));
        commands.put(prefix + "maker", EschaUtil.createMessageCommandGen("Made by **Barkuto**#2315 specifically for Puzzle and Dragons servers. Code at https://github.com/Barkuto/Eschamali"));
        commands.put(prefix + "tilt", EschaUtil.createMessageCommandGen("*T* *I* *L* *T* *E* *D*"));
        commands.put(prefix + "riot", EschaUtil.createMessageCommandGen("ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ"));
        commands.put(prefix + "ping", EschaUtil.createMessageCommandGen("Pong!"));

        commands.put(prefix + "serverinfo", serverinfo);
        commands.put(prefix + "sinfo", serverinfo);
        commands.put(prefix + "userinfo", userinfo);
        commands.put(prefix + "uinfo", userinfo);

        commands.put("?eval", eval);
        commands.put("?g", google);
        commands.put("?google", google);

        return commands;
    }

    @Override
    public String getName() {
        return "General";
    }

    private void serverInfoEmbedSpec(EmbedCreateSpec e, Guild g) {
        Instant creationDate = g.getId().getTimestamp();
        DateTimeFormatter dtf = new DateTimeFormatterBuilder()
                .appendPattern("MMM dd, yyyy hh:mm a")
                .toFormatter()
                .withLocale(Locale.US)
                .withZone(ZoneId.systemDefault());
        Optional<String> iconURL = g.getIconUrl(Image.Format.GIF);
        if (iconURL.isPresent()) {
            try {
                HttpURLConnection connection = (HttpURLConnection) (new URL(iconURL.get()).openConnection());
                connection.setRequestMethod("GET");
                connection.connect();
                int code = connection.getResponseCode();
                if (code >= 400)
                    iconURL = g.getIconUrl(Image.Format.PNG);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        e.setTitle(g.getName());
        iconURL.ifPresent(e::setThumbnail);
        e.setDescription("ID: " + g.getId().asString());

        e.addField("Created", dtf.format(creationDate), true);
        e.addField("Server Age", DAYS.between(creationDate, Instant.now()) + " days", true);
        e.addField("Region", g.getRegion().block().getName(), true);
        e.addField("Owner", g.getOwner().block().getMention(), true);
        e.addField("Users", g.getMemberCount().getAsInt() + "", true);
        e.addField("Roles", g.getRoleIds().size() + "", true);
        e.setColor(Color.WHITE);
    }

    private void userInfoEmbedSpec(EmbedCreateSpec e, Guild g, Member m) {
        String name = m.getUsername();
        String disc = m.getDiscriminator();
        String nick = m.getNickname().isPresent() ? m.getNickname().get() : "";
        String id = m.getId().asString();
        String avatar = m.getAvatarUrl();
        Instant accCreated = m.getId().getTimestamp();
        Instant guildJoinDate = m.getJoinTime();
        List<Role> roles = m.getRoles().collectList().block();
        Presence prescence = m.getPresence().block();
        Status status = prescence.getStatus();
        Activity activity = prescence.getActivity().isPresent() ? prescence.getActivity().get() : null;

        List<Member> usersSortedByJoin = g.getMembers().collectList().block();
        usersSortedByJoin.sort(Comparator.comparing(Member::getJoinTime));
        DateTimeFormatter dtf = new DateTimeFormatterBuilder()
                .appendPattern("MMM dd, yyyy hh:mm a")
                .toFormatter()
                .withLocale(Locale.US)
                .withZone(ZoneId.systemDefault());

        e.setTitle(name + "#" + disc + (nick.length() > 0 ? " AKA " + nick : ""));
        String activityString = "";
        if (activity != null)
            switch (activity.getType()) {
                case LISTENING:
                    activityString = "\nListening to **" + activity.getName() + "**";
                    break;
                case PLAYING:
                    activityString = "\nPlaying **" + activity.getName() + "**";
                    break;
                case STREAMING:
                    activityString = "\nStreaming" + (activity.getStreamingUrl().isPresent() ? ": " + activity.getStreamingUrl().get() : "");
                    break;
                case WATCHING:
                    activityString = "\nWatching **" + activity.getName() + "**";
                    break;
            }
        e.setDescription("Status **" + status.getValue().toUpperCase() + "**" + activityString);
        e.setThumbnail(avatar);

        e.setFooter("Member #" + (usersSortedByJoin.indexOf(m) + 1) + " | " + "ID: " + id, "");
        e.addField("Account Created", dtf.format(accCreated) + "\n" + DAYS.between(accCreated, Instant.now()) + " days ago", true);
        e.addField("Guild Joined", dtf.format(guildJoinDate) + "\n" + DAYS.between(guildJoinDate, Instant.now()) + " days ago", true);

        if (roles.size() > 0) {
            Collections.reverse(roles);
            StringBuilder sb = new StringBuilder();
            for (Role r : roles) {
                sb.append(r.getMention()).append(", ");
            }
            e.addField("Roles", sb.toString().substring(0, sb.toString().lastIndexOf(",")), false);
            e.setColor(roles.get(0).getColor());
        }
    }

//    @EventSubscriber
//    public void helpMessages(MessageReceivedEvent event) {
//        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
//            if (PermissionsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel())) {
//                String msg = event.getMessage().getContent();
//                IChannel channel = event.getChannel();
//                String[] args = msg.split(" ");
//                if (msg.startsWith("!help")) {
//                    if (args.length == 1) {
//                        String output = "__Eschamali Bot commands - Prefix:__ !\n";
//                        ArrayList<String> commands = new ArrayList<>();
//                        commands.add("`help`: Lists commands for certains parts of the bot. **USAGE**: help <module name>");
//                        commands.add("`donate`: See where you can donate to fund development/server keep up.");
//                        commands.add("`maker`: See who made me.");
//                        commands.add("`ayy`: Enable ayy mode for the server, requires ADMIN/MANAGE SERVER perm");
//                        commands.add("`tilt`: Send a message indicating you are tilted.");
//                        commands.add("`riot`: riot.");
//                        commands.add("`ping`: Visually check your ping with a pong.");
//                        commands.add("`alert`: Alerts Barkuto that something went wrong!");
//                        commands.add("`serverinfo`: Shows some information about the current server.");
//                        commands.add("`userinfo`: Shows some information about yourself, or the given user.");
//                        commands.add("`?google`: Give a link to google based on your query. Not !?google **UASGE**: ?google <query>");
//                        commands.add("`?eval`: Evaluate a simple math expression. Supports +,-,*,/,^,sqrt,sin,cos,tan. **USAGE**: ?eval <expression>");
//                        Collections.sort(commands);
//                        for (int i = 0; i < commands.size(); i++) {
//                            output += commands.get(i) + "\n";
//                        }
//                        Sender.sendMessage(channel, output);
//                    } else {
//                        String module = "";
//                        for (int i = 1; i < args.length; i++) {
//                            module += args[i];
//                        }
//                        String output = "";
//                        IModule theModule = null;
//                        ArrayList<String> cmds = null;
//                        for (IModule m : Eschamali.modules) {
//                            if (m.getName().equalsIgnoreCase(module)) {
//                                if (m instanceof ICommands) {
//                                    theModule = m;
//                                    cmds = ((ICommands) m).commands();
//                                    break;
//                                }
//                            }
//                        }
//                        if (cmds != null) {
//                            output += "__" + theModule.getName() + " module commands - Prefix:__ " + cmds.get(0) + "\n";
//                            for (int i = 1; i < cmds.size(); i++) {
//                                output += cmds.get(i) + "\n";
//                            }
//                            Sender.sendMessage(channel, output);
//                        } else {
//                            Sender.sendMessage(channel, "There is no module with that name.");
//                        }
//                    }
//                }
//            }
//        }
//    }

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
