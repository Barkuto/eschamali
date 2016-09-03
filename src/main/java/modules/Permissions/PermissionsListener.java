package modules.Permissions;

import base.Eschamali;
import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.modules.IModule;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Iggie on 9/1/2016.
 */
public class PermissionsListener {
    public static String prefix = ";";
    private String ownerID = "85844964633747456";

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        String path = "servers/" + event.getGuild().getName() + "-" + event.getGuild().getID() + ".db";
        File f = new File(path);
        if (!f.exists()) {
            Db db = new Db(path);
            db.execute("CREATE TABLE channels (module string, channels string)");
            db.execute("CREATE TABLE modules (module string, enabled string)");

            TreeMap<IModule, Boolean> modules = Eschamali.defaultmodules;
            for (Map.Entry<IModule, Boolean> e : modules.entrySet()) {
                db.execute("INSERT INTO modules (module, enabled) VALUES ('" + e.getKey().getName() + "','" + e.getValue() + "')");
            }
            db.close();
        }
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            String message = event.getMessage().getContent();
            String[] args = message.split(" ");
            args[0] = args[0].replace(prefix, "").trim();
            String cmd = args[0];
            IGuild guild = event.getMessage().getGuild();

            Permission perms = new Permission(new Db("servers/" + guild.getName() + "-" + guild.getID() + ".db"));
            if (message.startsWith(prefix)) {
                if (cmd.equalsIgnoreCase("db")) {
                    BufferedMessage.sendMessage(Eschamali.client, event, databaseString(guild));
                } else if (cmd.equalsIgnoreCase("atc") || cmd.equalsIgnoreCase("addtalkchannel")) {
                    String channels = message.substring(message.indexOf(cmd) + cmd.length() + 1);
                    String[] split = channels.split(" ");
                    ArrayList<IChannel> channelsAdded = new ArrayList<IChannel>();
                    for (int i = 0; i < split.length; i++) {
                        IChannel channel = null;
                        if (split[i].contains("#")) {
                            channel = guild.getChannelByID(split[i].replace("<#", "").replace(">", ""));
                            if (channel != null) {
                                if (!canTalkInChannel(guild, channel)) {
                                    perms.addPerms("channels", "module", "general", "channels", channel.getID());
                                    channelsAdded.add(channel);
                                }
                            }
                        }
                    }
                    String output = "I can now talk in ";
                    for (int i = 0; i < channelsAdded.size(); i++) {
                        output += channelsAdded.get(i).mention() + " ";
                    }
                    if (channelsAdded.size() >= 1) {
                        BufferedMessage.sendMessage(Eschamali.client, event, output);
                    } else {
                        BufferedMessage.sendMessage(Eschamali.client, event, "No new talk channels added.");
                    }
                } else if (cmd.equalsIgnoreCase("dtc") || cmd.equalsIgnoreCase("deletetalkchannel")) {
                    String channels = message.substring(message.indexOf(cmd) + cmd.length() + 1);
                    String[] split = channels.split(" ");

                    ArrayList<IChannel> currentChannels = new ArrayList<IChannel>();
                    String[] currChans = perms.getPerms("channels", "module", "general", "channels").split(";");
                    for (int i = 0; i < currChans.length; i++) {
                        IChannel c = guild.getChannelByID(currChans[i]);
                        if (c != null) {
                            currentChannels.add(guild.getChannelByID(currChans[i]));
                        }
                    }

                    ArrayList<IChannel> deleteChannels = new ArrayList<IChannel>();
                    for (int i = 0; i < split.length; i++) {
                        IChannel channel = null;
                        if (split[i].contains("#")) {
                            channel = guild.getChannelByID(split[i].replace("<#", "").replace(">", ""));
                            if (channel != null) {
                                deleteChannels.add(channel);
                            }
                        }
                    }

                    ArrayList<IChannel> newChannels = new ArrayList<IChannel>();
                    for (int i = 0; i < currentChannels.size(); i++) {
                        boolean add = true;
                        for (int j = 0; j < deleteChannels.size(); j++) {
                            if (currentChannels.get(i).getID().equalsIgnoreCase(deleteChannels.get(j).getID())) {
                                add = false;
                            }
                        }
                        if (add) {
                            newChannels.add(currentChannels.get(i));
                        }
                    }

                    String output = "I can no longer talk in: ";
                    String newTalkChannels = "";
                    for (int i = 0; i < newChannels.size(); i++) {
                        newTalkChannels += newChannels.get(i).getID() + ";";
                    }
                    newTalkChannels = newTalkChannels.substring(0, newTalkChannels.lastIndexOf(";"));
                    perms.setPerms("channels", "module", "general", "channels", newTalkChannels);
                    for (int i = 0; i < deleteChannels.size(); i++) {
                        output += deleteChannels.get(i).mention() + " ";
                    }
                    BufferedMessage.sendMessage(Eschamali.client, event, output.trim());
                } else if (cmd.equalsIgnoreCase("rtc") || cmd.equalsIgnoreCase("resettalkchannels")) {
                    perms.setPerms("channels", "module", "general", "channels", "");
                    BufferedMessage.sendMessage(Eschamali.client, event, "General talk channels have been reset.");
                } else if (cmd.equalsIgnoreCase("tc") || cmd.equalsIgnoreCase("talkchannels")) {
                    List<String> channels = Arrays.asList(perms.getPerms("channels", "module", "general", "channels").split(";"));
                    String output = "General talk channels are: ";
                    for (String s : channels) {
                        output += guild.getChannelByID(s).mention() + " ";
                    }
                    BufferedMessage.sendMessage(Eschamali.client, event, output);
                    //test it
                }
            }
            perms.close();
        }
    }

    public String databaseString(IGuild guild) {
        //channels:module|channels
        //modules:module|enabled
        //rolemisc:field|roles
        //roles:role|general|roles|pad|music
        String s = "```\n";
        Db db = new Db("servers/" + guild.getName() + "-" + guild.getID() + ".db");
        s += "channels\n";
        ResultSet rs = db.executeQuery("SELECT * FROM channels");
        try {
            while (rs.next()) {
                s += rs.getString("module") + ": " + rs.getString("channels") + "\n";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        s += "\nmodules\n";
        rs = db.executeQuery("SELECT * FROM modules");
        try {
            while (rs.next()) {
                s += rs.getString("module") + ": " + rs.getString("enabled") + "\n";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return s + "\n```";
    }

    public static boolean canTalkInChannel(IGuild guild, IChannel channel) {
        Permission perms = new Permission(new Db("servers/" + guild.getName() + "-" + guild.getID() + ".db"));
        List<String> list = Arrays.asList(perms.getPerms("channels", "module", "general", "channels").split(";"));
        for (String s : list) {
            if (s.equalsIgnoreCase(channel.getID()) || s.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    public static boolean canModuleInChannel(IGuild guild, String module, IChannel channel) {
        Permission perms = new Permission(new Db("servers/" + guild.getName() + "-" + guild.getID() + ".db"));
        String chans = perms.getPerms("channels", "module", module, "channels");
        List<String> list = Arrays.asList(chans.split(";"));
        for (String s : list) {
            if (s.equalsIgnoreCase(channel.getID()) || s.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isModuleOn(IGuild guild, String module) {
        Permission perms = new Permission(new Db("servers/" + guild.getName() + "-" + guild.getID() + ".db"));
        return Boolean.parseBoolean(perms.getPerms("modules", "module", module, "enabled"));
    }
}
