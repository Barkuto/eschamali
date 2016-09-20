package modules.CustomCommands;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.Permission;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

/**
 * Created by Iggie on 9/18/2016.
 */
public class CustomCommandsListener {
    public static String prefix = "!";
    private String tableName = "customcommands";
    private String col1 = "command";
    private String col2 = "message";

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        IGuild guild = event.getGuild();
        Permission perms = PermissionsListener.getPermissionDB(guild);
        if (!perms.tableExists("customcommands")) {
            perms.createTable(tableName, col1, "string", col2, "string");
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
                    if (commands.containsKey(cmd)) {
                        BufferedMessage.sendMessage(CustomCommandsModule.client, event, commands.get(cmd));
                    }
                    perms.close();
                }
            }
        }
    }
}
