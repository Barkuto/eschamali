package modules;

import base.Command;
import base.EschaUtil;
import base.Module;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Admin extends Module {
    private String tableName = "admin";
    private String col1 = "field";
    private String col2 = "role";
    private String[] table1Cols = {col1, col2};

    private String table2Name = "strikes";
    private String table2col1 = "user";
    private String table2col2 = "strikes";
    private String[] table2Cols = {table2col1, table2col2};

    private String bannedField = "bannedwords";
    private String muteRoleField = "muterole";

    public Admin(DiscordClient client) {
        super(client, "/");

        client.getEventDispatcher().on(GuildCreateEvent.class).flatMap(event -> {
            Guild guild = event.getGuild();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            if (!driver.tableExists(tableName)) {
                driver.createTable(tableName, table1Cols, new String[]{"string", "string"}, false);
            }
            if (!driver.tableExists(table2Name)) {
                driver.createTable(table2Name, table2Cols, new String[]{"string", "string"}, false);
            }
            driver.close();
            return Mono.empty();
        }).subscribe();

        client.getEventDispatcher().on(MessageCreateEvent.class).flatMap(event -> Mono.just(event.getMessage())
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .flatMap(message -> message.getChannel()
                        .ofType(TextChannel.class)
                        .flatMap(channel -> checkForBannedWords(event)))).subscribe();
        client.getEventDispatcher().on(MessageUpdateEvent.class).flatMap(event -> event.getMessage()
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .flatMap(message -> message.getChannel()
                        .ofType(TextChannel.class)
                        .flatMap(channel -> checkForBannedWords(event)))).subscribe();
    }

    private Mono<Void> checkForBannedWords(Event event) {
        Guild guild;
        Message message;
        Channel channel;
        if (event instanceof MessageCreateEvent) {
            MessageCreateEvent e = (MessageCreateEvent) event;
            guild = e.getGuild().block();
            message = e.getMessage();
            channel = message.getChannel().block();
        } else if (event instanceof MessageUpdateEvent) {
            MessageUpdateEvent e = (MessageUpdateEvent) event;
            guild = e.getGuild().block();
            message = e.getMessage().block();
            channel = message.getChannel().block();
        } else return Mono.empty();

        if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            String msg = message.getContent().get();
            String[] bannedWords = driver.getPerms(tableName, col1, bannedField, col2).split(";");
            for (int i = 0; i < bannedWords.length; i++) {
                String word = bannedWords[i].trim();
                if (word.length() > 0 && msg.contains(word)) {
                    return message.delete();
                }
            }
        }
        return Mono.empty();
    }

    @Override
    protected Map<String, Command> makeCommands() {
        Map<String, Command> commands = new HashMap<>();

        Command kick = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.KICK_MEMBERS)) {
                    Snowflake[] usersToKick = event.getMessage().getUserMentionIds().toArray(new Snowflake[0]);
                    for (int i = 0; i < usersToKick.length; i++) {
                        String mention = guild.getMemberById(usersToKick[i]).block().getMention();
                        guild.kick(usersToKick[i])
                                .then(EschaUtil.sendMessage(event, "Kicked " + mention))
                                .onErrorResume(v -> EschaUtil.sendMessage(event, "Could not kick " + mention))
                                .subscribe();
                    }
                }
            }
            return Mono.empty();
        };

        Command ban = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.BAN_MEMBERS)) {
                    Snowflake[] usersToBan = event.getMessage().getUserMentionIds().toArray(new Snowflake[0]);
                    for (int i = 0; i < usersToBan.length; i++) {
                        String mention = guild.getMemberById(usersToBan[i]).block().getMention();
                        guild.ban(usersToBan[i], q -> {
                        })
                                .then(EschaUtil.sendMessage(event, "Banned " + mention))
                                .onErrorResume(v -> EschaUtil.sendMessage(event, "Could not ban " + mention))
                                .subscribe();
                    }
                }
            }
            return Mono.empty();
        };

        Command prune = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES)) {
                    String[] args = EschaUtil.getArgs(event);
                    if (args.length >= 2) {
                        Member user = guild.getMemberById(Snowflake.of(parseUserID(args[0]))).block();
                        int msgCount;
                        try {
                            msgCount = Integer.parseInt(args[1].trim());
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                            msgCount = 10;
                        }
                        if (user != null && msgCount > 0) {
                            // J A N K
                            int delete = msgCount;
                            ArrayList<Message> toDelete = new ArrayList<>();
                            return channel.getMessagesBefore(event.getMessage().getId())
                                    .collectList()
                                    .flatMap(messages -> {
                                        for (Message m : messages) {
                                            if (toDelete.size() >= delete) break;
                                            m.getAuthorAsMember().flatMap(member -> {
                                                if (member.getId().equals(user.getId())) {
                                                    toDelete.add(m);
                                                    m.delete().subscribe();
                                                }
                                                return Mono.empty();
                                            }).subscribe();
                                        }
                                        return EschaUtil.sendMessage(event, "Deleted `" + toDelete.size() + "` messages of user " + user.getMention());
                                    });
                        }
                    }
                }
            }
            return Mono.empty();
        };

        Command mute = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES)) {
                    String[] args = EschaUtil.getArgs(event);
                    if (args.length >= 1) {
                        DBDriver driver = ChannelPerms.getPermissionDB(guild);
                        List<Member> mentions = event.getMessage().getUserMentions().flatMap(u -> u.asMember(guild.getId())).collectList().block();
                        if (mentions.size() > 0) {
                            Role muteRole = guild.getRoleById(Snowflake.of(Long.parseLong(driver.getPerms(tableName, col1, muteRoleField, col2)))).block();
                            if (muteRole == null) {
                                muteRole = guild.createRole(r -> r.setName("Muted").setColor(new Color(12, 0, 0))).block();
                                driver.setPerms(tableName, col1, muteRoleField, col2, muteRole.getId().asString() + "");

                                List<GuildChannel> serverChannels = guild.getChannels().collectList().block();
                                for (GuildChannel c : serverChannels) {
                                    c.addRoleOverwrite(muteRole.getId(),
                                            PermissionOverwrite.forRole(muteRole.getId(),
                                                    PermissionSet.none(),
                                                    PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY, Permission.SEND_MESSAGES)))
                                            .subscribe();
                                }
                            }

                            String output = "";
                            for (Member m : mentions) {
                                m.addRole(muteRole.getId()).subscribe();
                                output += m.getMention() + " ";
                            }

                            return EschaUtil.sendMessage(event, output + (mentions.size() > 1 ? "have" : " has") + " been muted.");
                        } else {
                            return EschaUtil.sendMessage(event, "Invalid user");
                        }
                    }
                }
            }
            return Mono.empty();
        };

        Command unmute = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES)) {
                    String[] args = EschaUtil.getArgs(event);
                    if (args.length >= 1) {
                        DBDriver driver = ChannelPerms.getPermissionDB(guild);
                        List<Member> mentions = event.getMessage().getUserMentions().flatMap(u -> u.asMember(guild.getId())).collectList().block();
                        if (mentions.size() > 0) {
                            Role muteRole = guild.getRoleById(Snowflake.of(Long.parseLong(driver.getPerms(tableName, col1, muteRoleField, col2)))).block();
                            if (muteRole != null) {
                                String output = "";
                                for (Member m : mentions) {
                                    m.removeRole(muteRole.getId()).subscribe();
                                    output += m.getMention() + " ";
                                }
                                return EschaUtil.sendMessage(event, output + (mentions.size() > 1 ? "have" : " has") + " been unmuted.");
                            } else {
                                return EschaUtil.sendMessage(event, "There is no mute role.");
                            }
                        } else {
                            return EschaUtil.sendMessage(event, "Invalid user");
                        }
                    }
                }
            }
            return Mono.empty();
        };

        Command muterole = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES)) {
                    String[] args = EschaUtil.getArgs(event);
                    String argsconcat = EschaUtil.getArgsConcat(event);
                    DBDriver driver = ChannelPerms.getPermissionDB(guild);
                    if (args.length == 0) {
                        return EschaUtil.sendMessage(event, "The current mute role is: " + guild.getRoleById(Snowflake.of(Long.parseLong(driver.getPerms(tableName, col1, muteRoleField, col2)))).block().getMention());
                    } else {
                        Role newMuteRole = EschaUtil.roleFromGuild(guild, argsconcat.trim());
                        Role oldMuteRole = guild.getRoleById(Snowflake.of(Long.parseLong(driver.getPerms(tableName, col1, muteRoleField, col2)))).block();
                        if (newMuteRole != null && oldMuteRole != null) {
                            List<GuildChannel> serverChannels = guild.getChannels().collectList().block();
                            for (GuildChannel c : serverChannels) {
                                c.addRoleOverwrite(oldMuteRole.getId(),
                                        PermissionOverwrite.forRole(oldMuteRole.getId(),
                                                PermissionSet.none(),
                                                PermissionSet.none()))
                                        .subscribe();
                                c.addRoleOverwrite(newMuteRole.getId(),
                                        PermissionOverwrite.forRole(newMuteRole.getId(),
                                                PermissionSet.none(),
                                                PermissionSet.of(Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY, Permission.SEND_MESSAGES)))
                                        .subscribe();
                            }
                        }
                        driver.setPerms(tableName, col1, muteRoleField, col2, newMuteRole.getId().asString() + "");

                        guild.getMembers().flatMap(m -> {
                            if (m.getRoleIds().contains(oldMuteRole.getId())) {
                                m.removeRole(oldMuteRole.getId()).subscribe();
                                m.addRole(newMuteRole.getId()).subscribe();
                            }
                            return Mono.empty();
                        }).subscribe();

                        return EschaUtil.sendMessage(event, "The mute role has been set to the role: " + newMuteRole.getMention());
                    }
                }
            }
            return Mono.empty();
        };

        Command lock = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES)) {
                    return EschaUtil.sendMessage(event, "Channel locked.")
                            .then(guild.getChannelById(channel.getId()).flatMap(c ->
                                    guild.getEveryoneRole().flatMap(e ->
                                            c.addRoleOverwrite(e.getId(), PermissionOverwrite.forRole(e.getId(), PermissionSet.none(), PermissionSet.of(Permission.SEND_MESSAGES))))));
                }
            }
            return Mono.empty();
        };

        Command unlock = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES)) {
                    return guild.getChannelById(channel.getId()).flatMap(c ->
                            guild.getEveryoneRole().flatMap(e ->
                                    c.addRoleOverwrite(e.getId(), PermissionOverwrite.forRole(e.getId(), PermissionSet.of(Permission.SEND_MESSAGES), PermissionSet.none()))))
                            .then(EschaUtil.sendMessage(event, "Channel unlocked."));
                }
            }
            return Mono.empty();
        };

        Command warn = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.BAN_MEMBERS, Permission.KICK_MEMBERS)) {
                    String[] args = EschaUtil.getArgs(event);
                    DBDriver driver = ChannelPerms.getPermissionDB(guild);
                    if (args.length >= 2) {
                        int strikesToAdd = 1;
                        try {
                            strikesToAdd = Integer.parseInt(args[1]);
                        } catch (Exception ignored) {
                        }

                        Member user = guild.getMemberById(Snowflake.of(parseUserID(args[0]))).block();
                        String userId = user.getId().asString();
                        String strikes = driver.getPerms(table2Name, table2col1, userId, table2col2);
                        int numStrikes = 0;
                        if (strikes.length() > 0) {
                            numStrikes = Integer.parseInt(strikes);
                        }
                        numStrikes += strikesToAdd;
                        driver.setPerms(table2Name, table2col1, userId, table2col2, numStrikes + "");
                        return EschaUtil.sendMessage(event, user.getMention() + " has been warned, and now has `" + numStrikes + "` strike(s).")
                                .then(Mono.just(numStrikes).flatMap(n -> {
                                    if (n >= 5) return guild.ban(user.getId(), q -> {
                                    });
                                    else if (n >= 3) return guild.kick(user.getId());
                                    return Mono.empty();
                                }));
                    }
                }
            }
            return Mono.empty();
        };

        Command warnings = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.BAN_MEMBERS, Permission.KICK_MEMBERS)) {
                    String[] args = EschaUtil.getArgs(event);
                    DBDriver driver = ChannelPerms.getPermissionDB(guild);
                    if (args.length == 0) {
                        String strikes = driver.getPerms(table2Name, table2col1, event.getMember().get().getId().asString(), table2col2);
                        int numStrikes = 0;
                        if (strikes.length() > 0) {
                            numStrikes = Integer.parseInt(strikes);
                        }
                        return EschaUtil.sendMessage(event, "You have `" + numStrikes + "` strike" + (numStrikes > 1 ? "s" : "") + ".");
                    } else {
                        List<Member> mentions = event.getMessage().getUserMentions().flatMap(u -> u.asMember(guild.getId())).collectList().block();
                        if (mentions != null && mentions.size() > 0) {
                            Member user = mentions.get(0);
                            String userID = user.getId().asString();
                            String strikes = driver.getPerms(table2Name, table2col1, userID, table2col2);
                            int numStrikes = 0;
                            if (strikes.length() > 0) {
                                numStrikes = Integer.parseInt(strikes);
                            }
                            return EschaUtil.sendMessage(event, user.getMention() + " has `" + numStrikes + "` strike" + (numStrikes > 1 ? "s" : "") + ".");
                        }
                    }
                }
            }
            return Mono.empty();
        };

        Command addbannedword = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES, Permission.MANAGE_ROLES, Permission.MANAGE_CHANNELS, Permission.MANAGE_GUILD)) {
                    String argsconcat = EschaUtil.getArgsConcat(event);
                    if (argsconcat.length() > 0) {
                        DBDriver driver = ChannelPerms.getPermissionDB(guild);
                        driver.addPerms(tableName, col1, bannedField, col2, argsconcat);
                        return EschaUtil.sendMessage(event, "Banned word/phrase `" + argsconcat + "` was added.");
                    }
                }
            }
            return Mono.empty();
        };

        Command deletebannedword = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES, Permission.MANAGE_ROLES, Permission.MANAGE_CHANNELS, Permission.MANAGE_GUILD)) {
                    String argsconcat = EschaUtil.getArgsConcat(event);
                    if (argsconcat.length() > 0) {
                        DBDriver driver = ChannelPerms.getPermissionDB(guild);
                        String wordToDelete = argsconcat;
                        String[] bannedWords = driver.getPerms(tableName, col1, bannedField, col2).split(";");
                        boolean found = false;
                        for (int i = 0; i < bannedWords.length; i++) {
                            if (bannedWords[i].equalsIgnoreCase(wordToDelete)) {
                                bannedWords[i] = "";
                                found = true;
                                break;
                            }
                        }

                        if (found) {
                            driver.deletePerms(tableName, col1, bannedField);
                            for (int i = 0; i < bannedWords.length; i++) {
                                if (bannedWords[i].length() > 0) {
                                    driver.addPerms(tableName, col1, bannedField, col2, bannedWords[i]);
                                }
                            }

                            return EschaUtil.sendMessage(event, "Banned word/phrase `" + argsconcat + "` was deleted.");
                        } else
                            return EschaUtil.sendMessage(event, "`" + argsconcat + "` is not a banned word.");
                    }
                }
            }
            return Mono.empty();
        };

        Command bannedwords = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_MESSAGES, Permission.MANAGE_ROLES, Permission.MANAGE_CHANNELS, Permission.MANAGE_GUILD)) {
                    DBDriver driver = ChannelPerms.getPermissionDB(guild);
                    String[] bannedWords = driver.getPerms(tableName, col1, bannedField, col2).split(";");
                    String output = "`Banned words:` ";
                    for (int i = 0; i < bannedWords.length; i++) {
                        output += bannedWords[i] + ", ";
                    }
                    if (output.contains(",")) {
                        output = output.substring(0, output.lastIndexOf(','));
                    }
                    return EschaUtil.sendMessage(event, output);
                }
            }
            return Mono.empty();
        };

        commands.put("kick", kick);
        commands.put("ban", ban);
        commands.put("prune", prune);
        commands.put("mute", mute);
        commands.put("unmute", unmute);
        commands.put("muterole", muterole);
        commands.put("lock", lock);
        commands.put("unlock", unlock);
        commands.put("warn", warn);
        commands.put("warnings", warnings);
        commands.put("addbannedword", addbannedword);
        commands.put("abw", addbannedword);
        commands.put("deletebannedword", deletebannedword);
        commands.put("dbw", deletebannedword);
        commands.put("bannedwords", bannedwords);
        commands.put("bw", bannedwords);

        return commands;
    }

    @Override
    public String getName() {
        return "Admin";
    }

    private Long parseUserID(String arg) {
        String id = "";
        int startIndex = 2;
        if (arg.startsWith("<@!")) {
            startIndex++;
        }
        id += arg.substring(startIndex, arg.length() - 1);
        return Long.parseLong(id);
    }
}
