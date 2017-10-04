package modules.Admin;

import modules.BufferedMessage.Sender;
import modules.Permissions.Permission;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageUpdateEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.MessageHistory;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RoleBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by Iggie on 9/15/2016.
 */
public class AdminListener {
    public static String prefix = "/";
    private String tableName = "admin";
    private String col1 = "field";
    private String col2 = "role";
    private String[] table1Cols = {col1, col2};

    private String table2Name = "strikes";
    private String table2col1 = "user";
    private String table2col2 = "strikes";
    private String[] table2Cols = {table2col1, table2col2};

    private String bannedField = "bannedwords";

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        IGuild guild = event.getGuild();
        Permission perms = PermissionsListener.getPermissionDB(guild);
        if (!perms.tableExists(tableName)) {
            perms.createTable(tableName, table1Cols, new String[]{"string", "string"}, false);
        }
        if (!perms.tableExists(table2Name)) {
            perms.createTable(table2Name, table2Cols, new String[]{"string", "string"}, false);
        }
        perms.close();
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            String message = event.getMessage().getContent();
            IUser author = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, AdminModule.name) && PermissionsListener.canModuleInChannel(guild, AdminModule.name, channel)) {
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

                    Permission perms = PermissionsListener.getPermissionDB(guild);
                    if (cmd.equalsIgnoreCase("kick")) {
                        if (userHasPerm(author, guild, Permissions.KICK)) {
                            //Kick from server
                            ArrayList<IUser> usersToKick = new ArrayList<>();
                            if (args.length == 2) {
                                IUser user = guild.getUserByID(parseUserID(argsconcat.trim()));
                                if (user != null) {
                                    usersToKick.add(user);
                                }
                            } else if (args.length > 2) {
                                for (int i = 1; i < args.length; i++) {
                                    IUser user = guild.getUserByID(parseUserID(args[i]));
                                    if (user != null) {
                                        usersToKick.add(user);
                                    }
                                }
                            }
//                            String reason = message.substring(message.indexOf("\""), message.lastIndexOf("\""));
                            for (int i = 0; i < usersToKick.size(); i++) {
                                guild.kickUser(usersToKick.get(i));
//                                    BufferedMessage.sendMessage(AdminModule.client, AdminModule.client.getOrCreatePMChannel(usersToKick.get(i)), "`You have been kicked because: `" + reason);
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("ban")) {
                        if (userHasPerm(author, guild, Permissions.BAN)) {
                            //Ban from server
                            ArrayList<IUser> usersToBan = new ArrayList<>();
                            if (args.length == 2) {
                                IUser user = guild.getUserByID(parseUserID(argsconcat.trim()));
                                if (user != null) {
                                    usersToBan.add(user);
                                }
                            } else if (args.length > 2) {
                                for (int i = 1; i < args.length; i++) {
                                    IUser user = guild.getUserByID(parseUserID(args[i]));
                                    if (user != null) {
                                        usersToBan.add(user);
                                    }
                                }
                            }
//                            String reason = message.substring(message.indexOf("\""), message.lastIndexOf("\""));
                            for (int i = 0; i < usersToBan.size(); i++) {
                                guild.banUser(usersToBan.get(i));
//                                    BufferedMessage.sendMessage(AdminModule.client, AdminModule.client.getOrCreatePMChannel(usersToBan.get(i)), "`You have been banned because: `" + reason);
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("prune")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES)) {
                            if (args.length >= 2) {
                                IUser user = guild.getUserByID(parseUserID(args[1]));
                                int msgCount;
                                try {
                                    msgCount = Integer.parseInt(args[2].trim());
                                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                                    msgCount = 10;
                                }
                                ArrayList<IMessage> msgsToDelete = new ArrayList<>();
                                if (user != null && msgCount > 0) {
                                    MessageHistory messages = channel.getMessageHistory();
                                    for (int i = 0; i < messages.size(); i++) {
                                        if (messages.get(i).getAuthor().equals(user)) {
                                            if (msgsToDelete.size() < msgCount) {
                                                msgsToDelete.add(messages.get(i));
                                            }
                                        }
                                    }
                                    for (int i = 0; i < msgsToDelete.size(); i++) {
                                        IMessage mesg = msgsToDelete.get(i);
                                        RequestBuffer.request(() -> {
                                            try {
                                                mesg.delete();
                                            } catch (RateLimitException e) {
                                                try {
                                                    Thread.sleep(e.getRetryDelay());
                                                    mesg.delete();
                                                } catch (InterruptedException e1) {
                                                    e1.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                    Sender.sendMessage(channel, "Deleted `" + msgsToDelete.size() + "` messages of user " + user.mention());
                                }
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("mute")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES)) {
                            if (args.length >= 2) {
                                IUser user = guild.getUserByID(parseUserID(args[1]));
                                if (user != null) {
                                    IRole muteRole = guild.getRoleByID(Long.parseLong(perms.getPerms(tableName, col1, "muterole", col2)));
                                    if (muteRole == null) {
                                        muteRole = new RoleBuilder(guild).withName("Muted").withColor(new Color(12, 0, 0)).build();
                                        perms.setPerms(tableName, col1, "muterole", col2, muteRole.getLongID() + "");
                                    }

                                    user.addRole(muteRole);
                                    List<IChannel> serverChannels = guild.getChannels();
                                    for (IChannel c : serverChannels) {
                                        c.overrideRolePermissions(muteRole, null, EnumSet.of(Permissions.READ_MESSAGES, Permissions.SEND_MESSAGES));
                                    }
                                    Sender.sendMessage(channel, user.mention() + " has been muted.");
                                } else {
                                    Sender.sendMessage(channel, "Invalid user");
                                }
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("unmute")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES)) {
                            if (args.length >= 2) {
                                IUser user = guild.getUserByID(parseUserID(args[1]));
                                if (user != null) {
                                    IRole muteRole = guild.getRoleByID(Long.parseLong(perms.getPerms(tableName, col1, "muterole", col2)));
                                    if (muteRole != null) {
                                        user.removeRole(muteRole);
                                        Sender.sendMessage(channel, user.mention() + " has been unmuted.");
                                    } else {
                                        Sender.sendMessage(channel, "There is no mute role.");
                                    }
                                } else {
                                    Sender.sendMessage(channel, "Invalid user");
                                }
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("muterole")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES)) {
                            if (args.length == 1) {
                                Sender.sendMessage(channel, "The current mute role is: " + guild.getRoleByID(Long.parseLong(perms.getPerms(tableName, col1, "muterole", col2))));
                            } else if (args.length >= 2) {
                                IRole newMuteRole = guild.getRolesByName(argsconcat.trim()).get(0);
                                IRole oldMuteRole = guild.getRoleByID(Long.parseLong(perms.getPerms(tableName, col1, "muterole", col2)));
                                if (oldMuteRole != null) {
                                    List<IChannel> serverChannels = guild.getChannels();
                                    for (IChannel c : serverChannels) {
                                        c.removePermissionsOverride(oldMuteRole);
                                        c.overrideRolePermissions(newMuteRole, null, EnumSet.of(Permissions.READ_MESSAGES, Permissions.SEND_MESSAGES));
                                    }
                                }
                                perms.setPerms(tableName, col1, "muterole", col2, newMuteRole.getLongID() + "");

                                //Not working for some reason, not removing roles, sometimes adds role.
//                                    for (IUser u : guild.getUsers()) {
//                                        List<IRole> userRoles = u.getRolesForGuild(guild);
//                                        for (IRole r : userRoles) {
//                                            if (r.getID().equals(oldMuteRole.getID())) {
//                                                u.removeRole(oldMuteRole);
//                                                u.addRole(newMuteRole);
//                                                break;
//                                            }
//                                        }
//                                    }

                                Sender.sendMessage(channel, "The mute role has been set to the role: " + newMuteRole.getName());
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("lock")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES)) {
                            Sender.sendMessage(channel, "Channel locked.");
                            channel.overrideRolePermissions(guild.getEveryoneRole(), null, EnumSet.of(Permissions.SEND_MESSAGES));
                        }
                    } else if (cmd.equalsIgnoreCase("unlock")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES)) {
                            channel.overrideRolePermissions(guild.getEveryoneRole(), EnumSet.of(Permissions.SEND_MESSAGES), null);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Sender.sendMessage(channel, "Channel unlocked.");
                        }
                    } else if (cmd.equalsIgnoreCase("warn") || cmd.equalsIgnoreCase("strike")) {
                        if (userHasPerm(author, guild, Permissions.BAN) || userHasPerm(author, guild, Permissions.KICK)) {
                            if (args.length > 1) {
                                int strikesToAdd = 1;
                                try {
                                    strikesToAdd = Integer.parseInt(args[2]);
                                } catch (Exception ignored) {
                                }
                                IUser user = guild.getUserByID(parseUserID(args[1]));
                                String userID = user.getStringID();
                                String strikes = perms.getPerms(table2Name, table2col1, userID, table2col2);
                                int numStrikes = 0;
                                if (strikes.length() > 0) {
                                    numStrikes = Integer.parseInt(strikes);
                                }
                                numStrikes += strikesToAdd;
                                perms.setPerms(table2Name, table2col1, userID, table2col2, numStrikes + "");
                                Sender.sendMessage(channel, user.mention() + " has been warned, and now has `" + numStrikes + "` strike(s).");
                                if (numStrikes >= 5) {
                                    guild.banUser(user);
                                } else if (numStrikes >= 3) {
                                    guild.kickUser(user);
                                }
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("warnings")) {
                        if (args.length == 1) {
                            String userID = author.getStringID();
                            String strikes = perms.getPerms(table2Name, table2col1, userID, table2col2);
                            int numStrikes = 0;
                            if (strikes.length() > 0) {
                                numStrikes = Integer.parseInt(strikes);
                            }
                            Sender.sendMessage(channel, "You have `" + numStrikes + "` strike(s).");
                        } else if (args.length == 2) {
                            if (userHasPerm(author, guild, Permissions.BAN) || userHasPerm(author, guild, Permissions.KICK)) {
                                IUser user = guild.getUserByID(parseUserID(args[1]));
                                String userID = user.getStringID();
                                String strikes = perms.getPerms(table2Name, table2col1, userID, table2col2);
                                int numStrikes = 0;
                                if (strikes.length() > 0) {
                                    numStrikes = Integer.parseInt(strikes);
                                }
                                Sender.sendMessage(channel, "__" + user.getName() + "__ has `" + numStrikes + "` strike(s).");
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("addbannedword") || cmd.equalsIgnoreCase("addbanword")
                            || cmd.equalsIgnoreCase("filter") || cmd.equalsIgnoreCase("abw")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES) || userHasPerm(author, guild, Permissions.MANAGE_ROLES)
                                || userHasPerm(author, guild, Permissions.MANAGE_CHANNEL) || userHasPerm(author, guild, Permissions.MANAGE_SERVER)) {
                            perms.addPerms(tableName, col1, bannedField, col2, argsconcat);
                            Sender.sendMessage(channel, "Banned word/phrase was added.");
                        }
                    } else if (cmd.equalsIgnoreCase("deletebannedword") || cmd.equalsIgnoreCase("delbanword")
                            || cmd.equalsIgnoreCase("unfilter") || cmd.equalsIgnoreCase("dbw")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES) || userHasPerm(author, guild, Permissions.MANAGE_ROLES)
                                || userHasPerm(author, guild, Permissions.MANAGE_CHANNEL) || userHasPerm(author, guild, Permissions.MANAGE_SERVER)) {
                            String wordToDelete = argsconcat;
                            String[] bannedWords = perms.getPerms(tableName, col1, bannedField, col2).split(";");
                            for (int i = 0; i < bannedWords.length; i++) {
                                if (bannedWords[i].equalsIgnoreCase(wordToDelete)) {
                                    bannedWords[i] = "";
                                    break;
                                }
                            }
                            perms.deletePerms(tableName, col1, bannedField);
                            for (int i = 0; i < bannedWords.length; i++) {
                                if (bannedWords[i].length() > 0) {
                                    perms.addPerms(tableName, col1, bannedField, col2, bannedWords[i]);
                                }
                            }
                            Sender.sendMessage(channel, "Banned word/phrase was deleted.");
                        }
                    } else if (cmd.equalsIgnoreCase("bannedwords") || cmd.equalsIgnoreCase("bw")) {
                        if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES) || userHasPerm(author, guild, Permissions.MANAGE_ROLES)
                                || userHasPerm(author, guild, Permissions.MANAGE_CHANNEL) || userHasPerm(author, guild, Permissions.MANAGE_SERVER)) {
                            String[] bannedWords = perms.getPerms(tableName, col1, bannedField, col2).split(";");
                            String output = "`Banned words:` ";
                            for (int i = 0; i < bannedWords.length; i++) {
                                output += bannedWords[i] + ", ";
                            }
                            if (output.contains(",")) {
                                output = output.substring(0, output.lastIndexOf(','));
                            }
                            Sender.sendMessage(channel, output);
                        }
                    }
                    perms.close();
                }
            }
        }
    }

    @EventSubscriber
    public void checkForBannedWord(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            String message = event.getMessage().getContent();
            IUser author = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, AdminModule.name) && PermissionsListener.canModuleInChannel(guild, AdminModule.name, channel)) {
                if (!message.startsWith(prefix)) {
                    Permission perms = PermissionsListener.getPermissionDB(guild);
                    String[] split = message.split(" ");
                    String[] bannedWords = perms.getPerms(tableName, col1, "bannedwords", col2).split(";");
                    for (int i = 0; i < split.length; i++) {
                        for (int j = 0; j < bannedWords.length; j++) {
                            if (split[i].equalsIgnoreCase(bannedWords[j].trim())) {
                                event.getMessage().delete();
                            }
                        }
                    }
                    perms.close();
                }
            }
        }
    }

    @EventSubscriber
    public void checkforEditedBannedWord(MessageUpdateEvent event) {
        String message = event.getNewMessage().getContent();
        IUser author = event.getNewMessage().getAuthor();
        IGuild guild = event.getNewMessage().getGuild();
        IChannel channel = event.getNewMessage().getChannel();
        if (PermissionsListener.isModuleOn(guild, AdminModule.name) && PermissionsListener.canModuleInChannel(guild, AdminModule.name, channel)) {
            if (!message.startsWith(prefix)) {
                Permission perms = PermissionsListener.getPermissionDB(guild);
                String[] split = message.split(" ");
                String[] bannedWords = perms.getPerms(tableName, col1, "bannedwords", col2).split(";");
                for (int i = 0; i < split.length; i++) {
                    for (int j = 0; j < bannedWords.length; j++) {
                        if (split[i].equalsIgnoreCase(bannedWords[j].trim())) {
                            event.getNewMessage().delete();
                        }
                    }
                }
                perms.close();
            }
        }
    }

    private boolean userHasPerm(IUser user, IGuild guild, Permissions perm) {
        List<IRole> roles = user.getRolesForGuild(guild);
        for (IRole r : roles) {
            if (r.getPermissions().contains(perm)) {
                return true;
            }
        }
        return false;
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
