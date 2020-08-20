package modules;

import base.Command;
import base.EschaUtil;
import base.Eschamali;
import base.Module;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Channel;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ChannelPerms extends Module {
    private String ownerID = "85844964633747456";

    public static final String channelsTableName = "channels";
    public static final String channelsCol1 = "module";
    public static final String channelsCol2 = "channels";
    public final String[] channelsCols = new String[]{channelsCol1, channelsCol2};

    public static final String modulesTableName = "modules";
    public static final String modulesCol1 = "module";
    public static final String modulesCol2 = "enabled";
    public final String[] modulesCols = new String[]{modulesCol1, modulesCol2};

    public static final String GENERAL = "General";

    public ChannelPerms(GatewayDiscordClient client) {
        super(client, ";");

        client.on(GuildCreateEvent.class)
                .flatMap(event -> {
                    DBDriver driver = getPermissionDB(event.getGuild());
                    if (!driver.tableExists(channelsTableName)) {
                        driver.createTable(channelsTableName, channelsCols, new String[]{"string", "string"}, false);
                    }
                    if (!driver.tableExists(modulesTableName)) {
                        driver.createTable(modulesTableName, modulesCols, new String[]{"string", "string"}, false);
                        for (Map.Entry<Module, Boolean> e : Eschamali.defaultmodules.entrySet()) {
                            driver.addPerms(modulesTableName, modulesCol1, e.getKey().getName(), modulesCol2, e.getValue() + "");
                        }
                    }
                    driver.close();
                    return Mono.empty();
                }).subscribe();
    }

    @Override
    protected Map<String, Command> makeCommands() {
        Map<String, Command> commands = new HashMap<>();

        commands.put("db", event ->
                EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD) ?
                        EschaUtil.sendMessage(event, databaseString(event.getGuild().block())) :
                        Mono.empty()
        );

        ////////////////
        //CHANNEL PERMS
        //////////////
        Command addtalkchannel = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            String[] args = EschaUtil.getArgs(event);
            String argsconcat = EschaUtil.getArgsConcat(event);
            Guild guild = event.getGuild().block();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            if (args.length >= 1) {
                if (argsconcat.equalsIgnoreCase("all")) {
                    if (driver.getPerms(channelsTableName, channelsCol1, GENERAL, channelsCol2).equalsIgnoreCase("all")) {
                        return EschaUtil.sendMessage(event, "I can already talk in all channels.");
                    } else {
                        driver.setPerms(channelsTableName, channelsCol1, GENERAL, channelsCol2, "all");
                        return EschaUtil.sendMessage(event, "I can now talk in all channels.");
                    }
                }

                ArrayList<Channel> channelsAdded = new ArrayList<>();
                for (String s : args) {
                    Channel aChannel = null;
                    if (s.contains("#")) {
                        aChannel = guild.getChannelById(Snowflake.of(Long.parseLong(s.replace("<#", "").replace(">", "")))).block();
                        if (aChannel != null) {
                            if (!canTalkInChannel(guild, aChannel)) {
                                driver.addPerms(channelsTableName, channelsCol1, GENERAL, channelsCol2, aChannel.getId().asString());
                                channelsAdded.add(aChannel);
                            }
                        }
                    }
                }
                String output = "I can now talk in ";
                for (int i = 0; i < channelsAdded.size(); i++) {
                    output += channelsAdded.get(i).getMention() + " ";
                }
                if (channelsAdded.size() >= 1)
                    return EschaUtil.sendMessage(event, output);
                else
                    return EschaUtil.sendMessage(event, "No new talk channels added.");
            }
            return Mono.empty();
        };

        Command deletetalkchannel = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            String[] args = EschaUtil.getArgs(event);
            Guild guild = event.getGuild().block();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            if (args.length >= 1) {
                ArrayList<Channel> currentChannels = new ArrayList<>();
                String[] currChans = driver.getPerms(channelsTableName, channelsCol1, GENERAL, channelsCol2).split(";");
                if (currChans[0].equalsIgnoreCase("all")) {
                    return EschaUtil.sendMessage(event, "I can currently talk in all channels.");
                }
                for (int i = 0; i < currChans.length; i++) {
                    Channel c = guild.getChannelById(Snowflake.of(Long.parseLong(currChans[i]))).block();
                    if (c != null) {
                        currentChannels.add(c);
                    }
                }

                ArrayList<Channel> deleteChannels = new ArrayList<>();
                for (int i = 0; i < args.length; i++) {
                    Channel aChannel = null;
                    if (args[i].contains("#")) {
                        aChannel = guild.getChannelById(Snowflake.of(Long.parseLong(args[i].replace("<#", "").replace(">", "")))).block();
                        if (aChannel != null) {
                            deleteChannels.add(aChannel);
                        }
                    }
                }

                ArrayList<Channel> newChannels = new ArrayList<>();
                for (int i = 0; i < currentChannels.size(); i++) {
                    boolean add = true;
                    for (int j = 0; j < deleteChannels.size(); j++) {
                        if (currentChannels.get(i).getId().asLong() == deleteChannels.get(j).getId().asLong()) {
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
                    newTalkChannels += newChannels.get(i).getId().asString() + ";";
                }
                if (newTalkChannels.contains(";")) {
                    newTalkChannels = newTalkChannels.substring(0, newTalkChannels.lastIndexOf(";"));
                }
                driver.setPerms(channelsTableName, channelsCol1, GENERAL, channelsCol2, newTalkChannels);
                for (int i = 0; i < deleteChannels.size(); i++) {
                    output += deleteChannels.get(i).getMention() + " ";
                }
                return EschaUtil.sendMessage(event, output.trim());
            }
            return Mono.empty();
        };

        Command resettalkchannels = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
            driver.setPerms(channelsTableName, channelsCol1, GENERAL, channelsCol2, "");
            return EschaUtil.sendMessage(event, "General talk channels have been reset.");
        };

        Command talkchannels = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            Guild guild = event.getGuild().block();
            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
            List<String> channels = Arrays.asList(driver.getPerms(channelsTableName, channelsCol1, GENERAL, channelsCol2).split(";"));
            if (channels.get(0).equalsIgnoreCase("all")) {
                return EschaUtil.sendMessage(event, "I can talk in all channels.");
            }
            String output = "General talk channels are: ";
            int len = output.length();
            for (String s : channels) {
                if (s.length() == 0) continue;
                Channel chan = guild.getChannelById(Snowflake.of(Long.parseLong(s))).block();
                if (chan != null) {
                    output += chan.getMention() + " ";
                }
            }
            if (output.length() == len)
                return EschaUtil.sendMessage(event, "There are no General talk channels.");
            else return EschaUtil.sendMessage(event, output);
        };

        ///////////////
        //MODULE PERMS
        /////////////
        Command modules = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
            String output = "`List of modules: `\n";
            for (Module m : Eschamali.modules) {
                String value = driver.getPerms(modulesTableName, modulesCol1, m.getName(), modulesCol2);
                if (value != null) {
                    Boolean enabled = Boolean.parseBoolean(value);
                    output += (enabled ? ":o: " : ":x: ") + m.getName() + "\n";
                }
            }
            return EschaUtil.sendMessage(event, output);
        };

        Command disableallmodules = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
            driver.deleteTable(modulesTableName);
            driver.createTable(modulesTableName, modulesCols, new String[]{"string", "string"}, false);
            for (Module m : Eschamali.modules) {
                driver.addPerms(modulesTableName, modulesCol1, m.getName(), modulesCol2, "false");
            }
            return EschaUtil.sendMessage(event, "All Modules have been __disabled__.");
        };

        Command enableedefaultmodules = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
            driver.deleteTable(modulesTableName);
            driver.createTable(modulesTableName, modulesCols, new String[]{"string", "string"}, false);
            for (Map.Entry<Module, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                driver.addPerms(modulesTableName, modulesCol1, entry.getKey().getName(), modulesCol2, entry.getValue().toString());
            }
            return EschaUtil.sendMessage(event, "Default Modules have been __enabled__.");
        };

        Command enableallmodules = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
            driver.deleteTable(modulesTableName);
            driver.createTable(modulesTableName, modulesCols, new String[]{"string", "string"}, false);
            for (Module m : Eschamali.modules) {
                driver.addPerms(modulesTableName, modulesCol1, m.getName(), modulesCol2, "true");
            }
            return EschaUtil.sendMessage(event, "All Modules have been __enabled__.");
        };

        Command enablemodule = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
            String[] args = EschaUtil.getArgs(event);
            String argsconcat = EschaUtil.getArgsConcat(event);
            if (args.length >= 1) {
                Module module = matchModule(argsconcat);
                if (module != null) {
                    driver.setPerms(modulesTableName, modulesCol1, module.getName(), modulesCol2, "true");
                    return EschaUtil.sendMessage(event, "The " + module.getName() + " module has been enabled.");
                } else {
                    return EschaUtil.sendMessage(event, "That is not a valid module.");
                }
            }
            return Mono.empty();
        };

        Command disablemodule = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
            String[] args = EschaUtil.getArgs(event);
            String argsconcat = EschaUtil.getArgsConcat(event);
            if (args.length >= 1) {
                Module module = matchModule(argsconcat);
                if (module != null) {
                    driver.setPerms(modulesTableName, modulesCol1, module.getName(), modulesCol2, "false");
                    return EschaUtil.sendMessage(event, "The " + module.getName() + " module has been disabled.");
                } else {
                    return EschaUtil.sendMessage(event, "That is not a valid module.");
                }
            }
            return Mono.empty();
        };

        //////////////////////
        //MODULE CHANNEL PERMS
        /////////////////////
        Command addmodulechannel = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            Guild guild = event.getGuild().block();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            String[] args = EschaUtil.getArgs(event);
            String argsconcat = EschaUtil.getArgsConcat(event);

            if (args.length >= 2) {
                String moduleName = args[0];
                Module module = matchModule(moduleName);

                if (module != null) {
                    if (argsconcat.substring(moduleName.length() + 1).equalsIgnoreCase("all")) {
                        driver.setPerms(channelsTableName, channelsCol1, module.getName(), channelsCol2, "all");
                        return EschaUtil.sendMessage(event, "The " + module.getName() + " module can now be used in all channels.");
                    }
                    if (driver.getPerms(channelsTableName, channelsCol1, module.getName(), channelsCol2).equalsIgnoreCase("all")) {
                        return EschaUtil.sendMessage(event, "The " + module.getName() + " can already be used in all channels.");
                    }
                    ArrayList<Channel> channelsAdded = new ArrayList<>();
                    for (int i = 1; i < args.length; i++) {
                        Channel aChannel = matchChannel(args[i], guild);
                        if (aChannel != null && !canModuleInChannel(guild, module.getName(), aChannel)) {
                            channelsAdded.add(aChannel);
                            driver.addPerms(channelsTableName, channelsCol1, module.getName(), channelsCol2, aChannel.getId().asString());
                        }
                    }
                    String output = "The " + module.getName() + " module can now be used in: ";
                    for (Channel c : channelsAdded) {
                        output += c.getMention() + " ";
                    }
                    return EschaUtil.sendMessage(event, output);
                } else {
                    return EschaUtil.sendMessage(event, "\"" + moduleName + "\" is a not a valid module.");
                }
            }
            return Mono.empty();
        };

        Command deletemodulechannel = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            Guild guild = event.getGuild().block();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            String[] args = EschaUtil.getArgs(event);

            if (args.length >= 2) {
                String moduleName = args[0];
                Module module = matchModule(moduleName);

                if (module != null) {

                    ArrayList<Channel> currentChannels = new ArrayList<>();
                    String[] currChans = driver.getPerms(channelsTableName, channelsCol1, module.getName(), channelsCol2).split(";");
                    if (currChans[0].equalsIgnoreCase("all")) {
                        return EschaUtil.sendMessage(event, "The " + module.getName() + " module can currently be used in all channels.");
                    }
                    for (int i = 0; i < currChans.length; i++) {
                        Channel c = matchChannel(currChans[i], guild);
                        if (c != null) {
                            currentChannels.add(c);
                        }
                    }

                    ArrayList<Channel> deleteChannels = new ArrayList<>();
                    for (int i = 1; i < args.length; i++) {
                        Channel aChannel = matchChannel(args[i], guild);
                        if (aChannel != null) {
                            deleteChannels.add(aChannel);
                        }
                    }

                    ArrayList<Channel> newChannels = new ArrayList<>();
                    for (int i = 0; i < currentChannels.size(); i++) {
                        boolean add = true;
                        for (int j = 0; j < deleteChannels.size(); j++) {
                            if (currentChannels.get(i).getId().asLong() == deleteChannels.get(j).getId().asLong()) {
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
                        newTalkChannels += newChannels.get(i).getId().asString() + ";";
                    }
                    if (newTalkChannels.contains(";")) {
                        newTalkChannels = newTalkChannels.substring(0, newTalkChannels.lastIndexOf(";"));
                    }
                    driver.setPerms(channelsTableName, channelsCol1, module.getName(), channelsCol2, newTalkChannels);
                    for (int i = 0; i < deleteChannels.size(); i++) {
                        output += deleteChannels.get(i).getMention() + " ";
                    }
                    return EschaUtil.sendMessage(event, output.trim());
                } else {
                    return EschaUtil.sendMessage(event, "\"" + moduleName + "\" is a not a valid module.");
                }
            }
            return Mono.empty();
        };

        Command resetmodulechannels = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            Guild guild = event.getGuild().block();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            String[] args = EschaUtil.getArgs(event);

            if (args.length >= 1) {
                String output = "The following modules channels have been reset: ";
                for (int i = 0; i < args.length; i++) {
                    Module module = matchModule(args[i]);
                    if (module != null) {
                        driver.resetPerms(channelsTableName, channelsCols, module.getName());
                        output += "**" + module.getName() + "**" + ", ";
                    }
                }
                if (output.contains(",")) {
                    output = output.substring(0, output.lastIndexOf(","));
                }
                return EschaUtil.sendMessage(event, output);
            }
            return Mono.empty();
        };

        Command modulechannels = event -> {
            if (!EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) return Mono.empty();

            Guild guild = event.getGuild().block();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            String argsconcat = EschaUtil.getArgsConcat(event);

            Module module = matchModule(argsconcat);
            if (module != null) {
                String output = "The " + module.getName() + " module can be used in the following channels: ";
                String chans = driver.getPerms(channelsTableName, channelsCol1, module.getName(), channelsCol2);
                if (chans.length() == 0) {
                    driver.addPerms(channelsTableName, channelsCol1, module.getName(), channelsCol2, "all");
                    chans = "all";
                }
                if (chans.equalsIgnoreCase("all")) {
                    return EschaUtil.sendMessage(event, "The " + module.getName() + " module can used in all channels.");
                }
                String[] channels = chans.split(";");
                for (int i = 0; i < channels.length; i++) {
                    Channel theChannel = guild.getChannelById(Snowflake.of(channels[i])).block();
                    if (theChannel != null) {
                        output += theChannel.getMention() + " ";
                    }
                }
                return EschaUtil.sendMessage(event, output);
            } else {
                return EschaUtil.sendMessage(event, "That is not a valid module.");
            }
        };

        commands.put(prefix + "addtalkchannel", addtalkchannel);
        commands.put(prefix + "atc", addtalkchannel);
        commands.put(prefix + "deletetalkchannel", deletetalkchannel);
        commands.put(prefix + "resettalkchannels", resettalkchannels);
        commands.put(prefix + "rtc", resettalkchannels);
        commands.put(prefix + "talkchannels", talkchannels);
        commands.put(prefix + "tc", talkchannels);

        commands.put(prefix + "modules", modules);
        commands.put(prefix + "m", modules);
        commands.put(prefix + "disableallmodules", disableallmodules);
        commands.put(prefix + "dam", disableallmodules);
        commands.put(prefix + "enabledefaultmodules", enableedefaultmodules);
        commands.put(prefix + "edm", enableedefaultmodules);
        commands.put(prefix + "enableallmodules", enableallmodules);
        commands.put(prefix + "eam", enableallmodules);
        commands.put(prefix + "enablemodule", enablemodule);
        commands.put(prefix + "em", enablemodule);
        commands.put(prefix + "disablemodule", disablemodule);
        commands.put(prefix + "dm", disablemodule);

        commands.put(prefix + "addmodulechannel", addmodulechannel);
        commands.put(prefix + "amc", addmodulechannel);
        commands.put(prefix + "deletemodulechannel", deletemodulechannel);
        commands.put(prefix + "dmc", deletemodulechannel);
        commands.put(prefix + "resetmodulechannels", resetmodulechannels);
        commands.put(prefix + "rmc", resetmodulechannels);
        commands.put(prefix + "modulechannels", modulechannels);
        commands.put(prefix + "mc", modulechannels);

        return commands;
    }

    @Override
    public String getName() {
        return "Permissions";
    }

    public String databaseString(Guild guild) {
        //channels:module|channels
        //modules:module|enabled
        String s = "```\n";
        s += "channels\n";
        DBDriver driver = getPermissionDB(guild);
        ResultSet rs = driver.selectAllFrom(channelsTableName);
        try {
            while (rs.next()) {
                s += rs.getString(channelsCol1) + ": " + rs.getString(channelsCol2) + "\n";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        s += "\nmodules\n";
        rs = driver.selectAllFrom(modulesTableName);
        try {
            while (rs.next()) {
                s += rs.getString(modulesCol1) + ": " + rs.getString(modulesCol2) + "\n";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        driver.close();
        return s + "\n```";
    }

    private Module matchModule(String string) {
        Module module = null;
        for (Module m : Eschamali.modules) {
            if (string.equalsIgnoreCase(m.getName())) {
                module = m;
                break;
            }
        }
        return module;
    }

    private Channel matchChannel(String string, Guild guild) {
        Channel channel = null;
        if (string.contains("#")) {
            channel = guild.getChannelById(Snowflake.of(
                    Long.parseLong(string
                            .replace("<#", "")
                            .replace(">", "")))).block();
        }
        return channel;
    }

    public static boolean canTalkInChannel(Guild guild, Channel channel) {
        DBDriver driver = getPermissionDB(guild);
        String[] list = driver.getPerms(channelsTableName, channelsCol1, GENERAL, channelsCol2).split(";");
        for (String s : list) {
            if (s.equalsIgnoreCase(channel.getId().asString()) || s.equalsIgnoreCase("all")) {
                driver.close();
                return true;
            }
        }
        driver.close();
        return false;
    }

    private static boolean canModuleInChannel(Guild guild, String module, Channel channel) {
        DBDriver driver = getPermissionDB(guild);
        String chans = driver.getPerms(channelsTableName, channelsCol1, module, channelsCol2);
        String[] list = chans.split(";");
        for (String s : list) {
            if (s.equalsIgnoreCase(channel.getId().asString()) || s.equalsIgnoreCase("all")) {
                driver.close();
                return true;
            }
        }
        driver.close();
        return false;
    }

    public static boolean isModuleOn(Guild guild, String module) {
        DBDriver driver = getPermissionDB(guild);
        boolean enabled = Boolean.parseBoolean(driver.getPerms(modulesTableName, modulesCol1, module, modulesCol2));
        driver.close();
        return enabled;
    }

    public static boolean canModuleIn(Guild guild, String module, Channel channel) {
        return isModuleOn(guild, module) && canModuleInChannel(guild, module, channel);
    }

    public static DBDriver getPermissionDB(Guild guild) {
        String path = "servers/";
        new File(path).mkdirs();
        return new DBDriver(new DB(path + guild.getId().asString() + ".db"));
    }
}
