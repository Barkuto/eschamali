package modules.Admin;

import base.Eschamali;
import modules.BufferedMessage.BufferedMessage;
import modules.Games.GamesModule;
import modules.Permissions.Permission;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by Iggie on 9/15/2016.
 */
public class AdminListener {
    public static String prefix = "/";

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
                                try {
                                    guild.kickUser(usersToKick.get(i));
//                                    BufferedMessage.sendMessage(AdminModule.client, AdminModule.client.getOrCreatePMChannel(usersToKick.get(i)), "`You have been kicked because: `" + reason);
                                } catch (MissingPermissionsException e) {
                                    e.printStackTrace();
                                } catch (RateLimitException e) {
                                    e.printStackTrace();
                                } catch (DiscordException e) {
                                    e.printStackTrace();
                                }
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
                                try {
                                    guild.banUser(usersToBan.get(i));
//                                    BufferedMessage.sendMessage(AdminModule.client, AdminModule.client.getOrCreatePMChannel(usersToBan.get(i)), "`You have been banned because: `" + reason);
                                } catch (MissingPermissionsException e) {
                                    e.printStackTrace();
                                } catch (RateLimitException e) {
                                    e.printStackTrace();
                                } catch (DiscordException e) {
                                    e.printStackTrace();
                                }
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
                                    MessageList messages = channel.getMessages();
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
                                                    try {
                                                        mesg.delete();
                                                    } catch (MissingPermissionsException e1) {
                                                        e1.printStackTrace();
                                                    } catch (DiscordException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                } catch (InterruptedException e1) {
                                                    e1.printStackTrace();
                                                }
                                            } catch (MissingPermissionsException e) {
                                                e.printStackTrace();
                                            } catch (DiscordException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                    }
                                    BufferedMessage.sendMessage(AdminModule.client, event, "Deleted `" + msgsToDelete.size() + "` messages of user " + user.mention());
                                }
                            }
                        }
                    }
                    perms.close();
                }
            }
        }
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

    public String parseUserID(String arg) {
        String id = "";
        int startIndex = 2;
        if (arg.startsWith("<@!")) {
            startIndex++;
        }
        id += arg.substring(startIndex, arg.length() - 1);
        return id;
    }
}
