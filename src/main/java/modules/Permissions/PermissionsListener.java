package modules.Permissions;

import base.Eschamali;
import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
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
        Permission perms = getPermissionDB(event.getGuild());
        if (!perms.tableExists("channels")) {
            perms.createTable("channels", "module", "string", "channels", "string");
        }
        if (!perms.tableExists("modules")) {
            perms.createTable("modules", "module", "string", "enabled", "string");
            for (Map.Entry<IModule, Boolean> e : Eschamali.defaultmodules.entrySet()) {
                perms.addPerms("modules", "module", e.getKey().getName(), "enabled", e.getValue() + "");
            }
        }
        perms.close();
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            String message = event.getMessage().getContent();
            IUser author = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();
            if (userHasPerm(author, guild, Permissions.MANAGE_SERVER) || author.getID().equals(ownerID)) {
                if (message.startsWith(prefix)) {
                    String[] args = message.split(" ");
                    args[0] = args[0].replace(prefix, "").trim();
                    String cmd = args[0];
                    String argsconcat;
                    try {
                        argsconcat = message.substring(cmd.length() + 2, message.length()).trim();
                    } catch (StringIndexOutOfBoundsException e) {
                        argsconcat = "";
                    }

                    Permission perms = getPermissionDB(guild);

                    if (cmd.equalsIgnoreCase("db")) {
                        BufferedMessage.sendMessage(Eschamali.client, event, databaseString(guild));
                    }
                    ////////////////
                    //CHANNEL PERMS
                    //////////////
                    else if (cmd.equalsIgnoreCase("atc") || cmd.equalsIgnoreCase("addtalkchannel")) {
                        if (args.length >= 2) {
                            if (argsconcat.trim().equalsIgnoreCase("all")) {
                                perms.setPerms("channels", "module", "General", "channels", "all");
                                BufferedMessage.sendMessage(Eschamali.client, event, "I can now talk in all channels.");
                                return;
                            }
                            if (perms.getPerms("channels", "module", "General", "channels").equalsIgnoreCase("all")) {
                                BufferedMessage.sendMessage(Eschamali.client, event, "I can already talk in all channels.");
                                return;
                            }
                            String channels = message.substring(message.indexOf(cmd) + cmd.length() + 1);
                            String[] split = channels.split(" ");
                            ArrayList<IChannel> channelsAdded = new ArrayList<IChannel>();
                            for (int i = 0; i < split.length; i++) {
                                IChannel channel = null;
                                if (split[i].contains("#")) {
                                    channel = guild.getChannelByID(split[i].replace("<#", "").replace(">", ""));
                                    if (channel != null) {
                                        if (!canTalkInChannel(guild, channel)) {
                                            perms.addPerms("channels", "module", "General", "channels", channel.getID());
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
                        }
                    } else if (cmd.equalsIgnoreCase("dtc") || cmd.equalsIgnoreCase("deletetalkchannel")) {
                        if (args.length >= 2) {
                            String channels = message.substring(message.indexOf(cmd) + cmd.length() + 1);
                            String[] split = channels.split(" ");

                            ArrayList<IChannel> currentChannels = new ArrayList<IChannel>();
                            String[] currChans = perms.getPerms("channels", "module", "General", "channels").split(";");
                            if (currChans[0].equalsIgnoreCase("all")) {
                                BufferedMessage.sendMessage(Eschamali.client, event, "I can currently talk in all channels.");
                                return;
                            }
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
                            if (newTalkChannels.contains(";")) {
                                newTalkChannels = newTalkChannels.substring(0, newTalkChannels.lastIndexOf(";"));
                            }
                            perms.setPerms("channels", "module", "General", "channels", newTalkChannels);
                            for (int i = 0; i < deleteChannels.size(); i++) {
                                output += deleteChannels.get(i).mention() + " ";
                            }
                            BufferedMessage.sendMessage(Eschamali.client, event, output.trim());
                        }
                    } else if (cmd.equalsIgnoreCase("rtc") || cmd.equalsIgnoreCase("resettalkchannels")) {
                        perms.setPerms("channels", "module", "General", "channels", "");
                        BufferedMessage.sendMessage(Eschamali.client, event, "General talk channels have been reset.");
                    } else if (cmd.equalsIgnoreCase("tc") || cmd.equalsIgnoreCase("talkchannels")) {
                        List<String> channels = Arrays.asList(perms.getPerms("channels", "module", "General", "channels").split(";"));
                        if (channels.get(0).equalsIgnoreCase("all")) {
                            BufferedMessage.sendMessage(Eschamali.client, event, "I can talk in all channels.");
                            return;
                        }
                        String output = "General talk channels are: ";
                        for (String s : channels) {
                            IChannel chan = guild.getChannelByID(s);
                            if (chan != null) {
                                output += chan.mention() + " ";
                            }
                        }
                        BufferedMessage.sendMessage(Eschamali.client, event, output);
                    }
                    ///////////////
                    //MODULE PERMS
                    /////////////
                    else if (cmd.equalsIgnoreCase("m") || cmd.equalsIgnoreCase("mods") || cmd.equalsIgnoreCase("modules")) {
                        String output = "`List of modules: `\n";
                        for (IModule m : Eschamali.modules) {
                            String value = perms.getPerms("modules", "module", m.getName(), "enabled");
                            if (value != null) {
                                Boolean enabled = Boolean.parseBoolean(value);
                                output += (enabled ? ":o: " : ":x: ") + m.getName() + "\n";
                            }
                        }
                        BufferedMessage.sendMessage(Eschamali.client, event, output);
                    } else if (cmd.equalsIgnoreCase("dam") || cmd.equalsIgnoreCase("disableallmodules")) {
                        perms.deleteTable("modules");
                        perms.createTable("modules", "module", "string", "enabled", "string");
                        for (IModule m : Eschamali.modules) {
                            perms.addPerms("modules", "module", m.getName(), "enabled", "false");
                        }
                        BufferedMessage.sendMessage(Eschamali.client, event, "All Modules have been __disabled__.");
                    } else if (cmd.equalsIgnoreCase("edm") || cmd.equalsIgnoreCase("enabledefaultmodules")) {
                        perms.deleteTable("modules");
                        perms.createTable("modules", "module", "string", "enabled", "string");
                        for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                            perms.addPerms("modules", "module", entry.getKey().getName(), "enabled", entry.getValue().toString());
                        }
                        BufferedMessage.sendMessage(Eschamali.client, event, "Default Modules have been __enabled__.");
                    } else if (cmd.equalsIgnoreCase("eam") || cmd.equalsIgnoreCase("enableallmodules")) {
                        perms.deleteTable("modules");
                        perms.createTable("modules", "module", "string", "enabled", "string");
                        for (IModule m : Eschamali.modules) {
                            perms.addPerms("modules", "module", m.getName(), "enabled", "true");
                        }
                        BufferedMessage.sendMessage(Eschamali.client, event, "All Modules have been __enabled__.");
                    } else if (cmd.equalsIgnoreCase("em") || cmd.equalsIgnoreCase("enablemodule")) {
                        if (args.length >= 2) {
                            IModule module = null;
                            for (IModule m : Eschamali.modules) {
                                if (argsconcat.equalsIgnoreCase(m.getName())) {
                                    module = m;
                                    break;
                                }
                            }
                            if (module != null) {
                                perms.setPerms("modules", "module", module.getName(), "enabled", "true");
                                BufferedMessage.sendMessage(Eschamali.client, event, "The " + module.getName() + " module has been enabled.");
                            } else {
                                BufferedMessage.sendMessage(Eschamali.client, event, "That is not a valid module.");
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("dm") || cmd.equalsIgnoreCase("disablemodule")) {
                        if (args.length >= 2) {
                            IModule module = null;
                            for (IModule m : Eschamali.modules) {
                                if (argsconcat.equalsIgnoreCase(m.getName())) {
                                    module = m;
                                    break;
                                }
                            }
                            if (module != null) {
                                perms.setPerms("modules", "module", module.getName(), "enabled", "false");
                                BufferedMessage.sendMessage(Eschamali.client, event, "The " + module.getName() + " module has been disabled.");
                            } else {
                                BufferedMessage.sendMessage(Eschamali.client, event, "That is not a valid module.");
                            }
                        }
                    }
                    //////////////////////
                    //MODULE CHANNEL PERMS
                    /////////////////////
                    else if (cmd.equalsIgnoreCase("amc") || cmd.equalsIgnoreCase("addmodulechannel")) {
                        if (args.length >= 3) {
                            String moduleName = args[1];
                            IModule module = null;
                            for (IModule m : Eschamali.modules) {
                                if (m.getName().equalsIgnoreCase(moduleName)) {
                                    module = m;
                                    break;
                                }
                            }

                            if (module != null) {
                                if (argsconcat.substring(moduleName.length() + 1, argsconcat.length()).equalsIgnoreCase("all")) {
                                    perms.setPerms("channels", "module", module.getName(), "channels", "all");
                                    BufferedMessage.sendMessage(Eschamali.client, event, "The " + module.getName() + " module can now be used in all channels.");
                                    return;
                                }
                                if (perms.getPerms("channels", "module", module.getName(), "channels").equalsIgnoreCase("all")) {
                                    BufferedMessage.sendMessage(Eschamali.client, event, "The " + module.getName() + " can already be used in all channels.");
                                    return;
                                }
                                ArrayList<IChannel> channelsAdded = new ArrayList<IChannel>();
                                for (int i = 2; i < args.length; i++) {
                                    IChannel channel = null;
                                    if (args[i].contains("#")) {
                                        channel = guild.getChannelByID(args[i].replace("<#", "").replace(">", ""));
                                        if (channel != null) {
                                            if (!canModuleInChannel(guild, module.getName(), channel)) {
                                                channelsAdded.add(channel);
                                                perms.addPerms("channels", "module", module.getName(), "channels", channel.getID());
                                            }
                                        }
                                    }
                                }
                                String output = "The " + module.getName() + " module can now be used in: ";
                                for (IChannel c : channelsAdded) {
                                    output += c.mention() + " ";
                                }
                                BufferedMessage.sendMessage(Eschamali.client, event, output);
                            } else {
                                BufferedMessage.sendMessage(Eschamali.client, event, "\"" + moduleName + "\" is a not a valid module.");
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("dmc") || cmd.equalsIgnoreCase("deletemodulechannel")) {
                        if (args.length >= 3) {
                            String moduleName = args[1];
                            IModule module = null;
                            for (IModule m : Eschamali.modules) {
                                if (m.getName().equalsIgnoreCase(moduleName)) {
                                    module = m;
                                    break;
                                }
                            }

                            if (module != null) {
                                String channels = message.substring(message.indexOf(cmd) + cmd.length() + 1);
                                String[] split = channels.split(" ");

                                ArrayList<IChannel> currentChannels = new ArrayList<IChannel>();
                                String[] currChans = perms.getPerms("channels", "module", module.getName(), "channels").split(";");
                                if (currChans[0].equalsIgnoreCase("all")) {
                                    BufferedMessage.sendMessage(Eschamali.client, event, "The " + module.getName() + " module can currently be used in all channels.");
                                    return;
                                }
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

                                String output = module.getName() + " module can no longer be used in: ";
                                String newTalkChannels = "";
                                for (int i = 0; i < newChannels.size(); i++) {
                                    newTalkChannels += newChannels.get(i).getID() + ";";
                                }
                                if (newTalkChannels.contains(";")) {
                                    newTalkChannels = newTalkChannels.substring(0, newTalkChannels.lastIndexOf(";"));
                                }
                                perms.setPerms("channels", "module", module.getName(), "channels", newTalkChannels);
                                for (int i = 0; i < deleteChannels.size(); i++) {
                                    output += deleteChannels.get(i).mention() + " ";
                                }
                                BufferedMessage.sendMessage(Eschamali.client, event, output.trim());
                            } else {
                                BufferedMessage.sendMessage(Eschamali.client, event, "\"" + moduleName + "\" is a not a valid module.");
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("rmc") || cmd.equalsIgnoreCase("resetmodulechannels")) {
                        if (args.length >= 2) {
                            String output = "The following modules channels have been reset: ";
                            String[] moduleNames = argsconcat.split(";");
                            for (int i = 0; i < moduleNames.length; i++) {
                                IModule module = null;
                                for (IModule m : Eschamali.modules) {
                                    if (m.getName().equalsIgnoreCase(moduleNames[i])) {
                                        module = m;
                                        break;
                                    }
                                }
                                if (module != null) {
                                    perms.resetPerms("channels", "module", module.getName(), "channels");
                                    output += "**" + module.getName() + "**" + ", ";
                                }
                            }
                            if (output.contains(",")) {
                                output = output.substring(0, output.lastIndexOf(","));
                            }
                            BufferedMessage.sendMessage(Eschamali.client, event, output);
                        }
                    } else if (cmd.equalsIgnoreCase("mc") || cmd.equalsIgnoreCase("modulechannels")) {
                        IModule module = null;
                        for (IModule m : Eschamali.modules) {
                            if (m.getName().equalsIgnoreCase(argsconcat)) {
                                module = m;
                            }
                        }
                        if (module != null) {
                            String output = "The " + module.getName() + " module can be used in the following channels: ";
                            String chans = perms.getPerms("channels", "module", module.getName(), "channels");
                            if (chans.equalsIgnoreCase("all")) {
                                BufferedMessage.sendMessage(Eschamali.client, event, "The " + module.getName() + " module can used in all channels.");
                                return;
                            }
                            String[] channels = chans.split(";");
                            for (int i = 0; i < channels.length; i++) {
                                IChannel channel = guild.getChannelByID(channels[i]);
                                if (channel != null) {
                                    output += channel.mention() + " ";
                                }
                            }
                            BufferedMessage.sendMessage(Eschamali.client, event, output);
                        } else {
                            BufferedMessage.sendMessage(Eschamali.client, event, "That is not a valid module.");
                        }
                    }
                    perms.close();
                }
            }
        }
    }

    public String databaseString(IGuild guild) {
        //channels:module|channels
        //modules:module|enabled
        String s = "```\n";
        s += "channels\n";
        Permission perms = getPermissionDB(guild);
        ResultSet rs = perms.selectAllFrom("channels");
        try {
            while (rs.next()) {
                s += rs.getString("module") + ": " + rs.getString("channels") + "\n";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        s += "\nmodules\n";
        rs = perms.selectAllFrom("modules");
        try {
            while (rs.next()) {
                s += rs.getString("module") + ": " + rs.getString("enabled") + "\n";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        perms.close();
        return s + "\n```";
    }

    public static boolean canTalkInChannel(IGuild guild, IChannel channel) {
        Permission perms = getPermissionDB(guild);
        List<String> list = Arrays.asList(perms.getPerms("channels", "module", "General", "channels").split(";"));
        for (String s : list) {
            if (s.equalsIgnoreCase(channel.getID()) || s.equalsIgnoreCase("all")) {
                perms.close();
                return true;
            }
        }
        perms.close();
        return false;
    }

    public static boolean canModuleInChannel(IGuild guild, String module, IChannel channel) {
        Permission perms = getPermissionDB(guild);
        String chans = perms.getPerms("channels", "module", module, "channels");
        List<String> list = Arrays.asList(chans.split(";"));
        for (String s : list) {
            if (s.equalsIgnoreCase(channel.getID()) || s.equalsIgnoreCase("all")) {
                perms.close();
                return true;
            }
        }
        perms.close();
        return false;
    }

    public static boolean isModuleOn(IGuild guild, String module) {
        Permission perms = getPermissionDB(guild);
        Boolean enabled = Boolean.parseBoolean(perms.getPerms("modules", "module", module, "enabled"));
        perms.close();
        return enabled;
    }

    public boolean userHasPerm(IUser user, IGuild guild, Permissions perm) {
        List<IRole> roles = user.getRolesForGuild(guild);
        for (IRole r : roles) {
            if (r.getPermissions().contains(perm)) {
                return true;
            }
        }
        return false;
    }

    public static Permission getPermissionDB(IGuild guild) {
        String path = "servers/";
        new File(path).mkdirs();
        return new Permission(new Db(path + guild.getID() + ".db"));
    }
}
