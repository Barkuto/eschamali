package modules.Permissions;

import base.Eschamali;
import modules.BufferedMessage.Sender;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.modules.IModule;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Iggie on 9/1/2016.
 */
public class PermissionsListener {
    public static String prefix = ";";
    private String ownerID = "85844964633747456";
    private String channelsTableName = "channels";
    private String channelsCol1 = "module";
    private String channelsCol2 = "channels";
    private String[] channelsCols = new String[]{channelsCol1, channelsCol2};

    private String modulesTableName = "modules";
    private String modulesCol1 = "module";
    private String modulesCol2 = "enabled";
    private String[] modulesCols = new String[]{modulesCol1, modulesCol2};

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        Permission perms = getPermissionDB(event.getGuild());
        if (!perms.tableExists("channels")) {
            perms.createTable("channels", channelsCols, new String[]{"string", "string"}, false);
        }
        if (!perms.tableExists("modules")) {
            perms.createTable("modules", modulesCols, new String[]{"string", "string"}, false);
            for (Map.Entry<IModule, Boolean> e : Eschamali.defaultmodules.entrySet()) {
                perms.addPerms("modules", "module", e.getKey().getName(), "enabled", e.getValue() + "");
            }
        }
        perms.close();
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            String message = event.getMessage().getContent().toLowerCase().trim();
            IChannel channel = event.getChannel();
            IUser author = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();
            if (userHasPerm(author, guild, Permissions.MANAGE_SERVER) || author.getStringID().equals(ownerID)) {
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

                    if (cmd.equals("db")) {
                        Sender.sendMessage(channel, databaseString(guild));
                    }
                    ////////////////
                    //CHANNEL PERMS
                    //////////////
                    else if (cmd.equals("atc") || cmd.equals("addtalkchannel")) {
                        if (args.length >= 2) {
                            if (argsconcat.trim().equalsIgnoreCase("all")) {
                                perms.setPerms("channels", "module", "General", "channels", "all");
                                Sender.sendMessage(channel, "I can now talk in all channels.");
                                return;
                            }
                            if (perms.getPerms("channels", "module", "General", "channels").equalsIgnoreCase("all")) {
                                Sender.sendMessage(channel, "I can already talk in all channels.");
                                return;
                            }
                            String channels = message.substring(message.indexOf(cmd) + cmd.length() + 1);
                            String[] split = channels.split(" ");
                            ArrayList<IChannel> channelsAdded = new ArrayList<>();
                            for (int i = 0; i < split.length; i++) {
                                IChannel aChannel = null;
                                if (split[i].contains("#")) {
                                    aChannel = guild.getChannelByID(Long.parseLong(split[i].replace("<#", "").replace(">", "")));
                                    if (aChannel != null) {
                                        if (!canTalkInChannel(guild, aChannel)) {
                                            perms.addPerms("channels", "module", "General", "channels", aChannel.getStringID());
                                            channelsAdded.add(aChannel);
                                        }
                                    }
                                }
                            }
                            String output = "I can now talk in ";
                            for (int i = 0; i < channelsAdded.size(); i++) {
                                output += channelsAdded.get(i).mention() + " ";
                            }
                            if (channelsAdded.size() >= 1) {
                                Sender.sendMessage(channel, output);
                            } else {
                                Sender.sendMessage(channel, "No new talk channels added.");
                            }
                        }
                    } else if (cmd.equals("dtc") || cmd.equals("deletetalkchannel")) {
                        if (args.length >= 2) {
                            String channels = message.substring(message.indexOf(cmd) + cmd.length() + 1);
                            String[] split = channels.split(" ");

                            ArrayList<IChannel> currentChannels = new ArrayList<IChannel>();
                            String[] currChans = perms.getPerms("channels", "module", "General", "channels").split(";");
                            if (currChans[0].equalsIgnoreCase("all")) {
                                Sender.sendMessage(channel, "I can currently talk in all channels.");
                                return;
                            }
                            for (int i = 0; i < currChans.length; i++) {
                                IChannel c = guild.getChannelByID(Long.parseLong(currChans[i]));
                                if (c != null) {
                                    currentChannels.add(guild.getChannelByID(Long.parseLong(currChans[i])));
                                }
                            }

                            ArrayList<IChannel> deleteChannels = new ArrayList<IChannel>();
                            for (int i = 0; i < split.length; i++) {
                                IChannel aChannel = null;
                                if (split[i].contains("#")) {
                                    aChannel = guild.getChannelByID(Long.parseLong(split[i].replace("<#", "").replace(">", "")));
                                    if (aChannel != null) {
                                        deleteChannels.add(aChannel);
                                    }
                                }
                            }

                            ArrayList<IChannel> newChannels = new ArrayList<IChannel>();
                            for (int i = 0; i < currentChannels.size(); i++) {
                                boolean add = true;
                                for (int j = 0; j < deleteChannels.size(); j++) {
                                    if (currentChannels.get(i).getLongID() == deleteChannels.get(j).getLongID()) {
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
                                newTalkChannels += newChannels.get(i).getStringID() + ";";
                            }
                            if (newTalkChannels.contains(";")) {
                                newTalkChannels = newTalkChannels.substring(0, newTalkChannels.lastIndexOf(";"));
                            }
                            perms.setPerms("channels", "module", "General", "channels", newTalkChannels);
                            for (int i = 0; i < deleteChannels.size(); i++) {
                                output += deleteChannels.get(i).mention() + " ";
                            }
                            Sender.sendMessage(channel, output.trim());
                        }
                    } else if (cmd.equals("rtc") || cmd.equals("resettalkchannels")) {
                        perms.setPerms("channels", "module", "General", "channels", "");
                        Sender.sendMessage(channel, "General talk channels have been reset.");
                    } else if (cmd.equals("tc") || cmd.equals("talkchannels")) {
                        List<String> channels = Arrays.asList(perms.getPerms("channels", "module", "General", "channels").split(";"));
                        if (channels.get(0).equalsIgnoreCase("all")) {
                            Sender.sendMessage(channel, "I can talk in all channels.");
                            return;
                        }
                        String output = "General talk channels are: ";
                        for (String s : channels) {
                            IChannel chan = guild.getChannelByID(Long.parseLong(s));
                            if (chan != null) {
                                output += chan.mention() + " ";
                            }
                        }
                        Sender.sendMessage(channel, output);
                    }
                    ///////////////
                    //MODULE PERMS
                    /////////////
                    else if (cmd.equals("m") || cmd.equals("mods") || cmd.equals("modules")) {
                        String output = "`List of modules: `\n";
                        for (IModule m : Eschamali.modules) {
                            String value = perms.getPerms("modules", "module", m.getName(), "enabled");
                            if (value != null) {
                                Boolean enabled = Boolean.parseBoolean(value);
                                output += (enabled ? ":o: " : ":x: ") + m.getName() + "\n";
                            }
                        }
                        Sender.sendMessage(channel, output);
                    } else if (cmd.equals("dam") || cmd.equals("disableallmodules")) {
                        perms.deleteTable("modules");
                        perms.createTable("modules", modulesCols, new String[]{"string", "string"}, false);
                        for (IModule m : Eschamali.modules) {
                            perms.addPerms("modules", "module", m.getName(), "enabled", "false");
                        }
                        Sender.sendMessage(channel, "All Modules have been __disabled__.");
                    } else if (cmd.equals("edm") || cmd.equals("enabledefaultmodules")) {
                        perms.deleteTable("modules");
                        perms.createTable("modules", modulesCols, new String[]{"string", "string"}, false);
                        for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                            perms.addPerms("modules", "module", entry.getKey().getName(), "enabled", entry.getValue().toString());
                        }
                        Sender.sendMessage(channel, "Default Modules have been __enabled__.");
                    } else if (cmd.equals("eam") || cmd.equals("enableallmodules")) {
                        perms.deleteTable("modules");
                        perms.createTable("modules", modulesCols, new String[]{"string", "string"}, false);
                        for (IModule m : Eschamali.modules) {
                            perms.addPerms("modules", "module", m.getName(), "enabled", "true");
                        }
                        Sender.sendMessage(channel, "All Modules have been __enabled__.");
                    } else if (cmd.equals("em") || cmd.equals("enablemodule")) {
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
                                Sender.sendMessage(channel, "The " + module.getName() + " module has been enabled.");
                            } else {
                                Sender.sendMessage(channel, "That is not a valid module.");
                            }
                        }
                    } else if (cmd.equals("dm") || cmd.equals("disablemodule")) {
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
                                Sender.sendMessage(channel, "The " + module.getName() + " module has been disabled.");
                            } else {
                                Sender.sendMessage(channel, "That is not a valid module.");
                            }
                        }
                    }
                    //////////////////////
                    //MODULE CHANNEL PERMS
                    /////////////////////
                    else if (cmd.equals("amc") || cmd.equals("addmodulechannel")) {
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
                                    Sender.sendMessage(channel, "The " + module.getName() + " module can now be used in all channels.");
                                    return;
                                }
                                if (perms.getPerms("channels", "module", module.getName(), "channels").equalsIgnoreCase("all")) {
                                    Sender.sendMessage(channel, "The " + module.getName() + " can already be used in all channels.");
                                    return;
                                }
                                ArrayList<IChannel> channelsAdded = new ArrayList<>();
                                for (int i = 2; i < args.length; i++) {
                                    IChannel aChannel = null;
                                    if (args[i].contains("#")) {
                                        aChannel = guild.getChannelByID(Long.parseLong(args[i].replace("<#", "").replace(">", "")));
                                        if (aChannel != null) {
                                            if (!canModuleInChannel(guild, module.getName(), aChannel)) {
                                                channelsAdded.add(aChannel);
                                                perms.addPerms("channels", "module", module.getName(), "channels", aChannel.getStringID());
                                            }
                                        }
                                    }
                                }
                                String output = "The " + module.getName() + " module can now be used in: ";
                                for (IChannel c : channelsAdded) {
                                    output += c.mention() + " ";
                                }
                                Sender.sendMessage(channel, output);
                            } else {
                                Sender.sendMessage(channel, "\"" + moduleName + "\" is a not a valid module.");
                            }
                        }
                    } else if (cmd.equals("dmc") || cmd.equals("deletemodulechannel")) {
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

                                ArrayList<IChannel> currentChannels = new ArrayList<>();
                                String[] currChans = perms.getPerms("channels", "module", module.getName(), "channels").split(";");
                                if (currChans[0].equalsIgnoreCase("all")) {
                                    Sender.sendMessage(channel, "The " + module.getName() + " module can currently be used in all channels.");
                                    return;
                                }
                                for (int i = 0; i < currChans.length; i++) {
                                    IChannel c = guild.getChannelByID(Long.parseLong(currChans[i]));
                                    if (c != null) {
                                        currentChannels.add(guild.getChannelByID(Long.parseLong(currChans[i])));
                                    }
                                }

                                ArrayList<IChannel> deleteChannels = new ArrayList<>();
                                for (int i = 0; i < split.length; i++) {
                                    IChannel aChannel = null;
                                    if (split[i].contains("#")) {
                                        aChannel = guild.getChannelByID(Long.parseLong(split[i].replace("<#", "").replace(">", "")));
                                        if (aChannel != null) {
                                            deleteChannels.add(aChannel);
                                        }
                                    }
                                }

                                ArrayList<IChannel> newChannels = new ArrayList<>();
                                for (int i = 0; i < currentChannels.size(); i++) {
                                    boolean add = true;
                                    for (int j = 0; j < deleteChannels.size(); j++) {
                                        if (currentChannels.get(i).getLongID() == deleteChannels.get(j).getLongID()) {
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
                                    newTalkChannels += newChannels.get(i).getStringID() + ";";
                                }
                                if (newTalkChannels.contains(";")) {
                                    newTalkChannels = newTalkChannels.substring(0, newTalkChannels.lastIndexOf(";"));
                                }
                                perms.setPerms("channels", "module", module.getName(), "channels", newTalkChannels);
                                for (int i = 0; i < deleteChannels.size(); i++) {
                                    output += deleteChannels.get(i).mention() + " ";
                                }
                                Sender.sendMessage(channel, output.trim());
                            } else {
                                Sender.sendMessage(channel, "\"" + moduleName + "\" is a not a valid module.");
                            }
                        }
                    } else if (cmd.equals("rmc") || cmd.equals("resetmodulechannels")) {
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
//                                    perms.resetPerms("channels", "module", module.getName(), "channels");
                                    perms.resetPerms("channels", channelsCols, module.getName());
                                    output += "**" + module.getName() + "**" + ", ";
                                }
                            }
                            if (output.contains(",")) {
                                output = output.substring(0, output.lastIndexOf(","));
                            }
                            Sender.sendMessage(channel, output);
                        }
                    } else if (cmd.equals("mc") || cmd.equals("modulechannels")) {
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
                                Sender.sendMessage(channel, "The " + module.getName() + " module can used in all channels.");
                                return;
                            }
                            String[] channels = chans.split(";");
                            for (int i = 0; i < channels.length; i++) {
                                IChannel theChannel = guild.getChannelByID(Long.parseLong(channels[i]));
                                if (theChannel != null) {
                                    output += theChannel.mention() + " ";
                                }
                            }
                            Sender.sendMessage(channel, output);
                        } else {
                            Sender.sendMessage(channel, "That is not a valid module.");
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
            if (s.equalsIgnoreCase(channel.getStringID()) || s.equalsIgnoreCase("all")) {
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
            if (s.equalsIgnoreCase(channel.getStringID()) || s.equalsIgnoreCase("all")) {
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
        return new Permission(new Db(path + guild.getStringID() + ".db"));
    }
}
