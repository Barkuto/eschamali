package base;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Owner extends Module {
    Map<String, Command> privateCommands;

    public Owner(GatewayDiscordClient client) {
        super(client, "~");
        privateCommands = new HashMap<>();

        Command changestatus = event -> {
            String argsconcat = EschaUtil.getArgsConcat(event);

            return argsconcat.length() > 0 ?
                    client.updatePresence(Presence.online(Activity.playing(argsconcat))) :
                    client.updatePresence(Presence.online());
        };

        Command changedefaultstatus = event -> {
            String argsconcat = EschaUtil.getArgsConcat(event);
            Properties props = new Properties();
            try {
                props.load(new FileReader(Eschamali.configFileName));
                props.setProperty("status", argsconcat);

                String comments = "";
                Scanner s = new Scanner(new File(Eschamali.configFileName));
                String str = s.nextLine();
                if (str.startsWith("#"))
                    comments += str.replaceFirst("#", "");
                props.store(new FileWriter(Eschamali.configFileName), comments);

                return EschaUtil.sendMessage(event, "Default status has been changed to `" + argsconcat + "`.")
                        .then(argsconcat.length() > 0 ?
                                client.updatePresence(Presence.online(Activity.playing(argsconcat))) :
                                client.updatePresence(Presence.online()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Mono.empty();
        };

        Command servers = event -> {
            List<Guild> guilds = client.getGuilds().collectList().block();
            String output = "Connected to `" + guilds.size() + "` guilds.\n";
            output += "```xl\n";
            output += String.format("%-50s | %-20s | %-10s", centerString("Server Name", 50), centerString("Server ID", 20), centerString("Users", 10)) + "\n";
            output += String.format("%-50s-|-%-20s-|-%-10s", repeatString("-", 50), repeatString("-", 20), repeatString("-", 10)) + "\n";
            for (Guild g : guilds) {
                output += String.format("%50s | %s | %s", g.getName(), centerString(g.getId().asString() + "", 20), centerString((g.getMemberCount()) + "", 10)) + "\n";
            }
            output += "```";
            String m = output;
            return EschaUtil.sendMessage(event, m);
        };

        Command uptime = EschaUtil.createMessageCommand("`Uptime: " + timeBetween(Eschamali.startTime, LocalDateTime.now()) + "`");

        Command version = event -> {
            Path file = FileSystems.getDefault().getPath("", "Eschamali-1.0-SNAPSHOT-shaded.jar");
            try {
                BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                return EschaUtil.sendMessage(event, "Last Modified: " + new SimpleDateFormat().format(attr.lastModifiedTime().toMillis()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Mono.empty();
        };

        Command shutdown = event ->
                EschaUtil.sendMessage(event, "Shutting down...")
                        .doOnSuccess(ignored -> System.exit(0))
                        .doOnError(ignored -> System.exit(0));

        privateCommands.put("changestatus", changestatus);
        privateCommands.put("status", changestatus);
        privateCommands.put("cs", changestatus);
        privateCommands.put("changedefaultstatus", changedefaultstatus);
        privateCommands.put("cds", changedefaultstatus);
        privateCommands.put("servers", servers);
        privateCommands.put("guilds", servers);
        privateCommands.put("uptime", uptime);
        privateCommands.put("up", uptime);
        privateCommands.put("version", version);
        privateCommands.put("v", version);
        privateCommands.put("shutdown", shutdown);

        this.client.on(MessageCreateEvent.class)
                // Get event Message
                .flatMap(event -> Mono.justOrEmpty(event.getMessage())
                        // Filter out other bot messages
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        // Filter out non-owner messages
                        .filter(message -> message.getAuthor().map(user -> Eschamali.ownerIDs.contains(user.getId().asLong())).orElse(false))
                        // Only read from private chats
                        .flatMap(message -> message.getChannel()
                                .ofType(PrivateChannel.class)
                                // Turn message into its contents
                                .flatMap(channel -> Mono.justOrEmpty(event.getMessage().getContent())
                                        // Iterate through all commands
                                        .flatMap(content -> Flux.fromIterable(privateCommands.entrySet())
                                                // If a command matches, execute it
                                                .filter(entry -> content.contains(" ") ?
                                                        content.substring(0, content.indexOf(" ")).equalsIgnoreCase(prefix + entry.getKey()) :
                                                        content.equalsIgnoreCase(prefix + entry.getKey()))
                                                .flatMap(entry -> entry.getValue().execute(event))
                                                .next()))))
                .subscribe();
    }

    @Override
    protected Map<String, Command> makeCommands() {
        Map<String, Command> commands = new HashMap<>();

        return commands;
    }

    @Override
    public String getName() {
        return "Owner";
    }

    public String centerString(String str, int width) {
        if (str.length() >= width)
            return str;
        String formatted = str;
        double toAdd = width - str.length();
        double addFr = Math.floor(toAdd / 2);
        double addBa = Math.ceil(toAdd / 2);
        for (int i = 0; i < addFr; i++) {
            formatted = " " + formatted;
        }
        for (int i = 0; i < addBa; i++) {
            formatted += " ";
        }
        return formatted;
    }

    public String repeatString(String str, int times) {
        String s = "";
        for (int i = 0; i < times; i++) {
            s += str;
        }
        return s;
    }

    public String timeBetween(LocalDateTime from, LocalDateTime to) {
        LocalDateTime fromDateTime = from;
        LocalDateTime toDateTime = to;

        LocalDateTime tempDateTime = LocalDateTime.from(fromDateTime);

        long years = tempDateTime.until(toDateTime, ChronoUnit.YEARS);
        tempDateTime = tempDateTime.plusYears(years);

        long months = tempDateTime.until(toDateTime, ChronoUnit.MONTHS);
        tempDateTime = tempDateTime.plusMonths(months);

        long days = tempDateTime.until(toDateTime, ChronoUnit.DAYS);
        tempDateTime = tempDateTime.plusDays(days);


        long hours = tempDateTime.until(toDateTime, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours(hours);

        long minutes = tempDateTime.until(toDateTime, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes(minutes);

        long seconds = tempDateTime.until(toDateTime, ChronoUnit.SECONDS);
        tempDateTime = tempDateTime.plusSeconds(seconds);

        return (years > 0 ? (years > 1 ? years + " years, " : years + " year, ") : "") +
                (months > 0 ? (months > 1 ? months + " months, " : months + " month, ") : "") +
                (days > 0 ? (days > 1 ? days + " days, " : days + " day, ") : "") +
                (hours > 0 ? (hours > 1 ? hours + " hours, " : hours + " hour, ") : "") +
                (minutes > 0 ? (minutes > 1 ? minutes + " minutes, " : minutes + " minute, ") : "") +
                (seconds > 0 ? (seconds > 1 ? seconds + " seconds" : seconds + " second") : "");
    }
}
