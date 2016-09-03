package base;

import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.Status;
import sx.blah.discord.util.DiscordException;
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
                } else if (cmd.equalsIgnoreCase("guilds")) {
                    List<IGuild> guilds = Eschamali.client.getGuilds();
                    String output = "Connected to `" + guilds.size() + "` guilds.\n";
                    for (IGuild g : guilds) {
                        String invite = null;
                        try {
                            invite = "https://discord.gg/" + g.getInvites().get(0).getInviteCode();
                        } catch (DiscordException e) {
                            e.printStackTrace();
                        } catch (RateLimitException e) {
                            e.printStackTrace();
                        } catch (IndexOutOfBoundsException e) {
//                            e.printStackTrace();
                        } catch (NullPointerException e) {

                        }
                        output += g.getName() + ": " + g.getUsers().size() + " users | " + (invite == null ? "No invite link." : invite) + "\n";
                    }
                    BufferedMessage.sendMessage(Eschamali.client, event, output);
                } else if (cmd.equalsIgnoreCase("leave")) {

                } else if (cmd.equalsIgnoreCase("setavatar")) {

                } else if (cmd.equalsIgnoreCase("uptime")) {
                    long uptime = System.currentTimeMillis() - Eschamali.startTime;
//                    RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
//                    long uptime = rb.getUptime();
//
                    long second = (uptime / 1000) % 60;
                    long minute = (uptime / (1000 * 60)) % 60;
                    long hour = (uptime / (1000 * 60 * 60)) % 24;

                    String time = String.format("%02dhrs:%02dmins:%02ds:%dms", hour, minute, second, uptime);
                    BufferedMessage.sendMessage(Eschamali.client, event, "`Uptime: " + time + "`");
                }
            }
        }

    }
}
