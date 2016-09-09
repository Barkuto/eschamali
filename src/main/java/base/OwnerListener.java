package base;

import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.Status;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import javax.lang.model.util.ElementScanner6;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Iggie on 8/23/2016.
 */
public class OwnerListener {
    public static String prefix = "~";

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (event.getMessage().getChannel() instanceof IPrivateChannel) {
            if (event.getMessage().getAuthor().getID().equals(Eschamali.ownerID) && event.getMessage().getContent().startsWith(prefix)) {
                String message = event.getMessage().getContent();
                String[] args = message.split(" ");
                String argsconcat = "";
                String cmd = args[0].replace(prefix, "");

                for (int i = 1; i < args.length; i++) {
                    argsconcat += args[i] + " ";
                }
                argsconcat = argsconcat.trim();
                if (cmd.equalsIgnoreCase("changestatus")) {
                    if (args.length > 1) {
                        Eschamali.client.changeStatus(Status.game(argsconcat));
                    } else {
                        Eschamali.client.changeStatus(Status.empty());
                    }
                } else if (cmd.equalsIgnoreCase("guilds") || cmd.equalsIgnoreCase("servers")) {
                    List<IGuild> guilds = Eschamali.client.getGuilds();
                    String output = "Connected to `" + guilds.size() + "` guilds.\n";
                    output += "```xl\n";
                    output += String.format("%-50s | %-20s | %-10s", centerString("Server Name", 50), centerString("Server ID", 20), centerString("Users", 10)) + "\n";
                    output += String.format("%-50s-|-%-20s-|-%-10s", repeatString("-", 50), repeatString("-", 20), repeatString("-", 10)) + "\n";
                    for (IGuild g : guilds) {
//                        String invite = null;
//                        try {
//                            invite = "https://discord.gg/" + g.getInvites().get(0).getInviteCode();
//                        } catch (DiscordException e) {
//                            e.printStackTrace();
//                        } catch (RateLimitException e) {
//                            e.printStackTrace();
//                        } catch (IndexOutOfBoundsException e) {
////                            e.printStackTrace();
//                        } catch (NullPointerException e) {
//
//                        }
//                        output += g.getName() + ": " + g.getUsers().size() + " users | " + (invite == null ? "No invite link." : invite) + "\n";
                        output += String.format("%50s | %s | %s", g.getName(), centerString(g.getID(), 20), centerString(g.getUsers().size() + "", 10)) + "\n";
                    }
                    output += "```";
                    BufferedMessage.sendMessage(Eschamali.client, event, output);
                } else if (cmd.equalsIgnoreCase("leave")) {

                } else if (cmd.equalsIgnoreCase("setavatar")) {

                } else if (cmd.equalsIgnoreCase("uptime")) {
                    long uptime = System.currentTimeMillis() - Eschamali.startTime;
//                    RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
//                    long uptime = rb.getUptime();

                    long second = (uptime / 1000) % 60;
                    long minute = (uptime / (1000 * 60)) % 60;
                    long hour = (uptime / (1000 * 60 * 60)) % 24;

                    String time = String.format("%02dhrs:%02dmins:%02ds:%dms", hour, minute, second, uptime);
                    BufferedMessage.sendMessage(Eschamali.client, event, "`Uptime: " + time + "`");
                } else if (cmd.equalsIgnoreCase("shutdown")) {
                    BufferedMessage.sendMessage(Eschamali.client, event.getMessage().getChannel(), "Shutting down...");
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
}
