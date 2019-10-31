package base;

import discord4j.core.DiscordClient;
import modules.ChannelPerms;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;

public class GeneralListener extends Module {
    private boolean ayy = false;

    public GeneralListener(DiscordClient client) {
        super(client, "");
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

        commands.put("!donate", EschaUtil.createMessageCommandGen("Donate for server/development funds at: https://streamlabs.com/barkuto"));
        commands.put("!maker", EschaUtil.createMessageCommandGen("Made by **Barkuto**#2315 specifically for Puzzle and Dragons servers. Code at https://github.com/Barkuto/Eschamali"));
        commands.put("!tilt", EschaUtil.createMessageCommandGen("*T* *I* *L* *T* *E* *D*"));
        commands.put("!riot", EschaUtil.createMessageCommandGen("ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ"));
        commands.put("!ping", EschaUtil.createMessageCommandGen("Pong!"));

        commands.put("?eval", eval);
        commands.put("?g", google);
        commands.put("?google", google);

        return commands;
    }

    @Override
    public String getName() {
        return "General";
    }

    //    @EventSubscriber
//    public void onMessage(MessageReceivedEvent event) {
//        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
//            if (PermissionsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel())) {
//                String msg = event.getMessage().getContent().toLowerCase().trim();
//                IChannel channel = event.getChannel();
//                if (msg.equals("!ayy")) {
//                    List<IRole> roles = event.getMessage().getAuthor().getRolesForGuild(event.getMessage().getGuild());
//                    if (Eschamali.ownerIDs.contains(event.getMessage().getAuthor().getLongID())) {
//                        ayy = !ayy;
//                        if (ayy) {
//                            Sender.sendMessage(channel, "lmao!");
//                        } else {
//                            Sender.sendMessage(channel, "lmao...");
//                        }
//                    } else {
//                        for (IRole r : roles) {
//                            if (r.getPermissions().contains(Permissions.ADMINISTRATOR) || r.getPermissions().contains(Permissions.MANAGE_SERVER)) {
//                                ayy = !ayy;
//                                if (ayy) {
//                                    Sender.sendMessage(channel, "lmao!");
//                                } else {
//                                    Sender.sendMessage(channel, "lmao...");
//                                }
//                                break;
//                            }
//                        }
//                    }
//                } else if (msg.equals("ayy") && ayy) {
//                    Sender.sendMessage(channel, "lmao");
//                } else if (msg.equals("!serverinfo") || msg.equals("!sinfo")) {
//                    IGuild guild = event.getMessage().getGuild();
////                    LocalDateTime creationDate = guild.getCreationDate();
//                    Instant creationDate = guild.getCreationDate();
//                    DateTimeFormatter dtf = new DateTimeFormatterBuilder()
//                            .appendPattern("MMM dd, yyyy hh:mm a")
//                            .toFormatter()
//                            .withLocale(Locale.US)
//                            .withZone(ZoneId.systemDefault());
//
//                    EmbedBuilder eb = new EmbedBuilder();
//                    eb.withTitle(guild.getName());
//                    eb.withThumbnail(guild.getIconURL());
//                    eb.withDesc("ID: " + guild.getLongID());
//                    eb.appendField("Created", dtf.format(creationDate), true);
//                    eb.appendField("Server Age", DAYS.between(creationDate, Instant.now()) + " days", true);
//                    eb.appendField("Region", guild.getRegion().getName(), true);
//                    eb.appendField("Owner", guild.getOwner().mention(), true);
//                    eb.appendField("Users", guild.getUsers().size() + "", true);
//                    eb.appendField("Roles", guild.getRoles().size() + "", true);
//                    eb.withColor(Color.WHITE);
//                    EmbedObject embed = eb.build();
//
//                    Sender.sendEmbed(channel, embed);
//                } else if (msg.equals("!userinfo") || msg.equals("!uinfo")) {
//                    IGuild guild = event.getMessage().getGuild();
//                    IUser user = null;
//                    if (msg.contains(" ")) {
//                        String arg = msg.substring(msg.indexOf(" ") + 1).trim();
//                        if (arg.startsWith("<@")) {
//                            String id = "";
//                            int startIndex = 2;
//                            if (arg.startsWith("<@!")) {
//                                startIndex++;
//                            }
//                            id += arg.substring(startIndex, arg.length() - 1);
//                            user = guild.getUserByID(Long.parseLong(id));
//                        } else {
//                            List<IUser> users = guild.getUsers();
//                            for (IUser u : users) {
//                                if (u.getName().toLowerCase().contains(arg.toLowerCase())) {
//                                    user = u;
//                                    break;
//                                }
//                            }
//                        }
//                    } else {
//                        user = event.getMessage().getAuthor();
//                    }
//                    if (user == null) {
//                        Sender.sendMessage(channel, "Could not find that user.");
//                        return;
//                    }
//                    String name = user.getName();
//                    String disc = user.getDiscriminator();
//                    String nick = user.getNicknameForGuild(guild) != null ? user.getNicknameForGuild(guild) : "";
//                    long id = user.getLongID();
//                    String avatar = user.getAvatarURL();
//                    Instant accCreated = user.getCreationDate();
//                    Instant guildJoinDate = null;
//                    DateTimeFormatter dtf = new DateTimeFormatterBuilder()
//                            .appendPattern("MMM dd, yyyy hh:mm a")
//                            .toFormatter()
//                            .withLocale(Locale.US)
//                            .withZone(ZoneId.systemDefault());
//                    List<IRole> roles = user.getRolesForGuild(guild);
//                    EmbedBuilder eb = new EmbedBuilder();
//
//                    try {
//                        guildJoinDate = guild.getJoinTimeForUser(user);
//                    } catch (DiscordException e) {
//                        e.printStackTrace();
//                    }
//
//                    String statusType = "";
//                    switch (user.getPresence().getStatus()) {
//                        case ONLINE:
//                            statusType = "Online";
//                            break;
//                        case OFFLINE:
//                            statusType = "Offline";
//                            break;
//                        case IDLE:
//                            statusType = "Idle";
//                            break;
//                        case DND:
//                            statusType = "Do Not Disturb";
//                            break;
////                        case STREAMING:
////                            statusType = "Streaming ";
////                            break;
//                        case UNKNOWN:
//                            statusType = "Unknown";
//                            break;
//                    }
//
//                    eb.withTitle(name + "#" + disc + (nick.length() > 0 ? " AKA " + nick : ""));
//                    eb.withDesc("Status: " + (user.getPresence().getText().isPresent() ? "Playing " + user.getPresence().getText().get() : statusType));
//                    eb.withThumbnail(avatar.replace(".jpg", ".gif"));
//                    List<IUser> usersSortedByJoin = guild.getUsers();
//                    usersSortedByJoin.sort((o1, o2) -> {
//                        try {
//                            return guild.getJoinTimeForUser(o1).compareTo(guild.getJoinTimeForUser(o2));
//                        } catch (DiscordException e) {
//                            e.printStackTrace();
//                        }
//                        return -2;
//                    });
//                    eb.withFooterText("Member #" + (usersSortedByJoin.indexOf(user) + 1) + " | " + "ID: " + id);
//                    eb.appendField("Account Created", dtf.format(accCreated) + "\n" + DAYS.between(accCreated, Instant.now()) + " days ago", true);
//                    eb.appendField("Guild Joined", dtf.format(guildJoinDate) + "\n" + DAYS.between(guildJoinDate, Instant.now()) + " days ago", true);
//                    String rolesString = roles.toString().replace("[", "").replace("]", "");
//                    int count = 0;
//                    for (IRole r : roles) {
//                        if (r == guild.getEveryoneRole()) {
//                            count++;
//                        }
//                    }
//                    if (count > 1) {
//                        rolesString = rolesString.replaceFirst(", @everyone", "");
//                    }
//                    eb.appendField("Roles", rolesString, false);
//                    eb.withColor(roles.get(0).getColor());
//                    EmbedObject embed = eb.build();
//                    Sender.sendEmbed(channel, embed);
//                }
//            }
//        }
//    }

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
