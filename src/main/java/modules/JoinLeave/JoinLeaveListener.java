package modules.JoinLeave;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.Permission;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

/**
 * Created by Iggie on 8/17/2016.
 */
public class JoinLeaveListener {

    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        if (PermissionsListener.isModuleOn(event.getGuild(), JoinLeaveModule.name)) {
            IGuild guild = event.getGuild();
            IUser user = event.getUser();
            Permission perms = PermissionsListener.getPermissionDB(guild);
            String channel = perms.getPerms("channels", "module", JoinLeaveModule.name, "channels");
            if (channel != null && channel.length() > 0) {
                IChannel chan = guild.getChannelByID(Long.parseLong(channel));
                BufferedMessage.sendMessage(JoinLeaveModule.client, chan, user.mention() + " : " + user.getName() + "#" + user.getDiscriminator() + " has joined.");
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
                IChannel chan = guild.getChannelByID(Long.parseLong(channel));
                BufferedMessage.sendMessage(JoinLeaveModule.client, chan, user.mention() + " : " + user.getName() + "#" + user.getDiscriminator() + " has left.");
            }
            perms.close();
        }
    }

    @EventSubscriber
    public void onUserLeave(UserBanEvent event) {
        if (PermissionsListener.isModuleOn(event.getGuild(), JoinLeaveModule.name)) {
            IGuild guild = event.getGuild();
            IUser user = event.getUser();
            Permission perms = PermissionsListener.getPermissionDB(guild);
            String channel = perms.getPerms("channels", "module", JoinLeaveModule.name, "channels");
            if (channel != null && channel.length() > 0) {
                IChannel chan = guild.getChannelByID(Long.parseLong(channel));
                BufferedMessage.sendMessage(JoinLeaveModule.client, chan, user.mention() + " : " + user.getName() + "#" + user.getDiscriminator() + " has been banned.");
            }
            perms.close();
        }
    }
}
