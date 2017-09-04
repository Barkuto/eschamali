package base;

import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.Image;
import sx.blah.discord.util.RateLimitException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Created by Iggie on 8/23/2016.
 */
public class OwnerListener {
    public static String prefix = "~";

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (event.getMessage().getChannel() instanceof IPrivateChannel) {
            if (Eschamali.ownerIDs.contains(event.getMessage().getAuthor().getLongID()) && event.getMessage().getContent().startsWith(prefix)) {
                String message = event.getMessage().getContent();
                String[] args = message.split(" ");
                String argsconcat = "";
                String cmd = args[0].replace(prefix, "");

                for (int i = 1; i < args.length; i++) {
                    argsconcat += args[i] + " ";
                }
                argsconcat = argsconcat.trim();
                if (cmd.equalsIgnoreCase("changestatus") || cmd.equalsIgnoreCase("status") || cmd.equalsIgnoreCase("cs")) {
                    if (args.length > 1) {
                        Eschamali.client.changePlayingText(argsconcat);
                    } else {
                        Eschamali.client.changePlayingText(null);
                    }
                } else if (cmd.equalsIgnoreCase("changedefaultstatus") || cmd.equalsIgnoreCase("cds")) {
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
                        BufferedMessage.sendMessage(Eschamali.client, event, "Default status has been changed to `" + argsconcat + "`.");

                        if (args.length > 1) {
                            Eschamali.client.changePlayingText(argsconcat);
                        } else {
                            Eschamali.client.changePlayingText(null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (cmd.equalsIgnoreCase("guilds") || cmd.equalsIgnoreCase("servers")) {
                    List<IGuild> guilds = Eschamali.client.getGuilds();
                    String output = "Connected to `" + guilds.size() + "` guilds.\n";
                    output += "```xl\n";
                    output += String.format("%-50s | %-20s | %-10s", centerString("Server Name", 50), centerString("Server ID", 20), centerString("Users", 10)) + "\n";
                    output += String.format("%-50s-|-%-20s-|-%-10s", repeatString("-", 50), repeatString("-", 20), repeatString("-", 10)) + "\n";
                    for (IGuild g : guilds) {
                        output += String.format("%50s | %s | %s", g.getName(), centerString(g.getLongID() + "", 20), centerString(g.getUsers().size() + "", 10)) + "\n";
                    }
                    output += "```";
                    BufferedMessage.sendMessage(Eschamali.client, event, output);
                } else if (cmd.equalsIgnoreCase("leave")) {
                    if (args.length > 1) {
                        String id = message.substring(message.indexOf(" "));
                        IGuild g = Eschamali.client.getGuildByID(Long.parseLong(id));
                        if (g != null) {
                            g.leave();
                            BufferedMessage.sendMessage(Eschamali.client, event.getMessage().getChannel(), "Left server `" + g.getName() + "`");
                        }
                    }
                } else if (cmd.equalsIgnoreCase("setavatar")) {
                    String url = message.substring(message.indexOf(" "));
                    String imgtype = url.substring(url.lastIndexOf(".") + 1);
                    Eschamali.client.changeAvatar(Image.forUrl(imgtype, url));
                    BufferedMessage.sendMessage(Eschamali.client, event.getMessage().getChannel(), "Avatar changed.");
                } else if (cmd.equalsIgnoreCase("changename")) {
                    Eschamali.client.changeUsername(message.substring(message.indexOf(" ") + 1));
                    BufferedMessage.sendMessage(Eschamali.client, event.getMessage().getChannel(), "Username changed.");
                } else if (cmd.equalsIgnoreCase("uptime") || cmd.equalsIgnoreCase("up")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, "`Uptime: " + timeBetween(Eschamali.startTime, LocalDateTime.now()) + "`");
                } else if (cmd.equalsIgnoreCase("shutdown") || cmd.equalsIgnoreCase("sd")) {
                    BufferedMessage.sendMessage(Eschamali.client, event.getMessage().getChannel(), "Shutting down...");
                    List<IVoiceChannel> connectedVoice = Eschamali.client.getConnectedVoiceChannels();
                    for (IVoiceChannel v : connectedVoice) {
                        v.leave();
                    }
                    System.exit(0);
                }
            }
        }
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
