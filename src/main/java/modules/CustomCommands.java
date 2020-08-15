package modules;

import base.Command;
import base.EschaUtil;
import base.Module;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CustomCommands extends Module {
    private String tableName = "customcommands";
    private String col1 = "command";
    private String col2 = "message";
    private String[] tableCols = new String[]{col1, col2};

    public CustomCommands(GatewayDiscordClient client) {
        super(client, "!");

        client.on(GuildCreateEvent.class).flatMap(event -> {
            Guild guild = event.getGuild();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            if (!driver.tableExists(tableName)) {
                driver.createTable(tableName, tableCols, new String[]{"string", "string"}, false);
            }
            driver.close();
            return Mono.empty();
        }).subscribe();

        client.on(MessageCreateEvent.class)
                .flatMap(event -> Mono.justOrEmpty(event.getMessage())
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        .flatMap(message -> message.getChannel())
                        .ofType(TextChannel.class)
                        .flatMap(channel -> Mono.justOrEmpty(event.getMessage().getContent())
                                .flatMap(content -> {
                                    if (content.startsWith(prefix)) {
                                        DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild().block());
                                        String cmd = content.substring(1).split(" ")[0];
                                        if (cmd.length() > 0) {
                                            String result = driver.getPerms(tableName, col1, cmd, col2);
                                            if (result.length() > 0) {
                                                return EschaUtil.sendMessage(event, result);
                                            }
                                        }
                                    }
                                    return Mono.empty();
                                })))
                .subscribe();
    }

    @Override
    protected Map<String, Command> makeCommands() {
        Map<String, Command> commands = new HashMap<>();

        Command addcustomcommand = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD, Permission.MANAGE_ROLES)) {
                    String[] parse = parseCustomArgs(event);

                    if (parse != null) {
                        DBDriver driver = ChannelPerms.getPermissionDB(guild);
                        if (driver.getPerms(tableName, col1, parse[0], col2).equals("")) {
                            driver.setPerms(tableName, col1, parse[0], col2, parse[1]);
                            return EschaUtil.sendMessage(event, "Added custom command \"" + parse[0] + "\"");
                        } else {
                            return EschaUtil.sendMessage(event, "That is already a custom command!");
                        }
                    } else return EschaUtil.sendMessage(event, "Invalid Arguments.");
                }
            }
            return Mono.empty();
        };

        Command deletecustomcommand = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD, Permission.MANAGE_ROLES)) {
                    String[] args = EschaUtil.getArgs(event);
                    if (args.length >= 1) {
                        DBDriver driver = ChannelPerms.getPermissionDB(guild);
                        String commandName = args[0].trim();
                        if (!driver.getPerms(tableName, col1, commandName, col2).equals("")) {
                            driver.deletePerms(tableName, col1, commandName);
                            return EschaUtil.sendMessage(event, "Deleted custom command \"" + commandName + "\"");
                        } else {
                            return EschaUtil.sendMessage(event, "That is not a custom command!");
                        }
                    } else return EschaUtil.sendMessage(event, "Invalid Arguments.");
                }
            }
            return Mono.empty();
        };

        Command editcustomcommand = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD, Permission.MANAGE_ROLES)) {
                    String[] parse = parseCustomArgs(event);

                    if (parse != null) {
                        DBDriver driver = ChannelPerms.getPermissionDB(guild);
                        if (!driver.getPerms(tableName, col1, parse[0], col2).equals("")) {
                            driver.setPerms(tableName, col1, parse[0], col2, parse[1]);
                            return EschaUtil.sendMessage(event, "Edited custom command \"" + parse[0] + "\"");
                        } else {
                            return EschaUtil.sendMessage(event, "That is not a custom command!");
                        }
                    } else return EschaUtil.sendMessage(event, "Invalid Arguments.");
                }
            }
            return Mono.empty();
        };

        Command customcommands = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                DBDriver driver = ChannelPerms.getPermissionDB(guild);
                ResultSet rs = driver.selectAllFrom(tableName);
                String output = "`Custom commands " + prefix + ":` ";
                try {
                    while (rs.next()) {
                        output += rs.getString(col1) + ", ";
                    }
                    if (output.trim().endsWith(",")) {
                        output = output.trim().substring(0, output.length() - 2);
                    }
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return EschaUtil.sendMessage(event, output);
            }
            return Mono.empty();
        };

        commands.put(prefix + "addcustomcommand", addcustomcommand);
        commands.put(prefix + "acc", addcustomcommand);
        commands.put(prefix + "deletecustomcommand", deletecustomcommand);
        commands.put(prefix + "dcc", deletecustomcommand);
        commands.put(prefix + "editcustomcommand", editcustomcommand);
        commands.put(prefix + "ecc", editcustomcommand);
        commands.put(prefix + "customcommands", customcommands);
        commands.put(prefix + "cc", customcommands);

        return commands;
    }

    @Override
    public String getName() {
        return "CustomCommands";
    }

    private String[] parseCustomArgs(MessageCreateEvent event) {
        String args[] = EschaUtil.getArgs(event);
        if (args.length >= 2) {
            String commandName = args[0].trim();
            String commandText = "";
            for (int i = 1; i < args.length; i++) {
                commandText += args[i] + " ";
            }
            commandText = commandText.trim();

            if (commandName.length() > 0 && commandText.length() > 0) {
                return new String[]{commandName, commandText};
            }
        }
        return null;
    }
}
