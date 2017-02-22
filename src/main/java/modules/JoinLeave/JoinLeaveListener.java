package modules.JoinLeave;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.Permission;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.impl.events.UserLeaveEvent;
import sx.blah.discord.handle.impl.obj.User;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Iggie on 8/17/2016.
 */
public class JoinLeaveListener {
    public static String prefix = "";

    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        if (PermissionsListener.isModuleOn(event.getGuild(), JoinLeaveModule.name)) {
            IGuild guild = event.getGuild();
            IUser user = event.getUser();
            Permission perms = PermissionsListener.getPermissionDB(guild);
            String channel = perms.getPerms("channels", "module", JoinLeaveModule.name, "channels");
            if (channel != null && channel.length() > 0) {
                IChannel chan = guild.getChannelByID(channel);
                BufferedMessage.sendMessage(JoinLeaveModule.client, chan, user.mention() +" has joined.");
            }
            perms.close();
        }
    }

    @EventSubscriber
    public void onUserLeave(UserLeaveEvent event) {
        if (PermissionsListener.isModuleOn(event.getGuild(), JoinLeaveModule.name)) {
            IGuild guild = event.getGuild();
            IUser user = event.getUser();
            Permission perms = PermissionsListener.getPermissionDB(guild);
            String channel = perms.getPerms("channels", "module", JoinLeaveModule.name, "channels");
            if (channel != null && channel.length() > 0) {
                IChannel chan = guild.getChannelByID(channel);
                BufferedMessage.sendMessage(JoinLeaveModule.client, chan, user.mention() +" has left.");
            }
            perms.close();
        }
    }
}
