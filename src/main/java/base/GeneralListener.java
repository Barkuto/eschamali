package base;

import com.sun.org.apache.bcel.internal.generic.ARRAYLENGTH;
import modules.BufferedMessage.BufferedMessage;
import modules.Channels.ChannelsListener;
import modules.Channels.ChannelsModule;
import modules.Games.GamesModule;
import modules.Music.MusicModule;
import modules.Parrot.ParrotModule;
import modules.Roles.RolesModule;
import net.dv8tion.d4j.player.MusicPlayer;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collection;
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
        if (ChannelsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel().getName())) {
            String msg = event.getMessage().getContent();
            if (msg.equalsIgnoreCase("!donate")) {
                BufferedMessage.sendMessage(Eschamali.client, event, "Donate for server/development funds at: https://www.twitchalerts.com/donate/barkuto");
            } else if (msg.equalsIgnoreCase("!maker")) {
                BufferedMessage.sendMessage(Eschamali.client, event, "Made by **Barkuto**#2315 specifically for the PAD w/ MZeus server. Down with A.LB!");
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
                BufferedMessage.sendMessage(Eschamali.client, event, "*T* *I* *L* *T*");
            } else if (msg.equalsIgnoreCase("!riot")) {
                BufferedMessage.sendMessage(Eschamali.client, event, "ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ");
            } else if (msg.equalsIgnoreCase("!ping")) {
                BufferedMessage.sendMessage(Eschamali.client, event, "pong!");
            }
        }
    }

    @EventSubscriber
    public void helpMessages(MessageReceivedEvent event) {
        if (ChannelsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel().getName())) {
            String msg = event.getMessage().getContent();
            String[] args = msg.split(" ");
            if (msg.startsWith("!help") || msg.startsWith("!h")) {
                if (args.length == 1) {
                    String output = "__Eschamali Bot commands - Prefix:__ !\n";
                    ArrayList<String> commands = new ArrayList<String>();
                    commands.add("`donate`: See where you can donate to fund development/server keep up.");
                    commands.add("`maker`: See who made me.");
                    commands.add("`ayy`: Enable ayy mode for the server, requires ADMIN/MANAGE SERVER perm");
                    commands.add("`tilt`: Send a message indicating you are tilted.");
                    commands.add("`riot`: riot.");
                    commands.add("`ping`: Visually check your ping with a pong.");
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
