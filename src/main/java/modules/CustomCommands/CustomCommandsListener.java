package modules.CustomCommands;

import base.Eschamali;
import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.Permission;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by Iggie on 9/18/2016.
 */
public class CustomCommandsListener {
    public static String prefix = "!";
    private String tableName = "customcommands";
    private String col1 = "command";
    private String col2 = "message";
    private String[] tableCols = new String[]{col1, col2};
    private String ownerID = "85844964633747456";

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        IGuild guild = event.getGuild();
        Permission perms = PermissionsListener.getPermissionDB(guild);
        if (!perms.tableExists(tableName)) {
            perms.createTable(tableName, tableCols, new String[]{"string", "string"}, false);
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
            if (PermissionsListener.isModuleOn(guild, CustomCommandsModule.name)
                    && PermissionsListener.canModuleInChannel(guild, CustomCommandsModule.name, channel)) {
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

                    TreeMap<String, String> commands = new TreeMap<>();
                    Permission perms = PermissionsListener.getPermissionDB(guild);
                    try {
                        ResultSet rs = perms.selectAllFrom(tableName);
                        while (rs.next()) {
                            commands.put(rs.getString(col1), rs.getString(col2));
                        }
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    if (commands.containsKey(cmd.toLowerCase())) {
                        BufferedMessage.sendMessage(CustomCommandsModule.client, event, commands.get(cmd.toLowerCase()));
                    } else {
                        if (userHasPerm(author, guild, Permissions.MANAGE_SERVER) || author.getID().equals(ownerID) || userHasPerm(author, guild, Permissions.MANAGE_ROLES)) {
                            String commandName = "";
                            String commandText = "";
                            if (args.length > 1) {
                                commandName = args[1].trim().toLowerCase();
                            }
                            if (args.length > 2) {
                                commandText = argsconcat.substring(argsconcat.indexOf(" ")).trim();
                            }
                            if (commandName.length() > 0) {
                                if (cmd.equals("addcustomcommand") || cmd.equalsIgnoreCase("acc")) {
                                    if (perms.getPerms(tableName, col1, commandName, col2).equals("")) {
                                        perms.setPerms(tableName, col1, commandName, col2, commandText);
                                        BufferedMessage.sendMessage(CustomCommandsModule.client, event, "Added custom command \"" + commandName + "\"");
                                    } else {
                                        BufferedMessage.sendMessage(CustomCommandsModule.client, event, "That is already a custom command!");
                                    }
                                } else if (cmd.equals("deletecustomcommand") || cmd.equals("dcc") || cmd.equals("rcc")) {
                                    if (!perms.getPerms(tableName, col1, commandName, col2).equals("")) {
                                        perms.deletePerms(tableName, col1, commandName);
                                        BufferedMessage.sendMessage(CustomCommandsModule.client, event, "Deleted custom command \"" + commandName + "\"");
                                    } else {
                                        BufferedMessage.sendMessage(CustomCommandsModule.client, event, "That is not a custom command!");
                                    }
                                } else if (cmd.equals("editcustomcommand") || cmd.equals("ecc")) {
                                    if (!perms.getPerms(tableName, col1, commandName, col2).equals("")) {
                                        perms.setPerms(tableName, col1, commandName, col2, commandText);
                                        BufferedMessage.sendMessage(CustomCommandsModule.client, event, "Edited custom command \"" + commandName + "\"");
                                    } else {
                                        BufferedMessage.sendMessage(CustomCommandsModule.client, event, "That is not a custom command!");
                                    }
                                }
                            }
                        }
                        if (cmd.equalsIgnoreCase("customcommands") || cmd.equalsIgnoreCase("cc")) {
                            ResultSet rs = perms.selectAllFrom(tableName);
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
                            BufferedMessage.sendMessage(CustomCommandsModule.client, event, output);
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
}
