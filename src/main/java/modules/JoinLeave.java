package modules;

import base.Command;
import base.EschaUtil;
import base.Module;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

public class JoinLeave extends Module {

    public JoinLeave(DiscordClient client) {
        super(client, "");

        client.getEventDispatcher().on(MemberJoinEvent.class).flatMap(event -> {
            Guild guild = event.getGuild().block();
            if (ChannelPerms.isModuleOn(guild, getName())) {
                Member user = event.getMember();
                DBDriver driver = ChannelPerms.getPermissionDB(guild);
                String channel = driver.getPerms(ChannelPerms.channelsTableName, ChannelPerms.channelsCol1, getName(), ChannelPerms.channelsCol2);
                if (channel != null && channel.length() > 0) {
                    return EschaUtil.sendMessage((MessageChannel) guild.getChannelById(Snowflake.of(Long.parseLong(channel))).block(),
                            user.getMention() + " : " + user.getUsername() + "#" + user.getDiscriminator() + " has joined.");
                }
                driver.close();
            }
            return Mono.empty();
        }).subscribe();

        client.getEventDispatcher().on(MemberLeaveEvent.class).flatMap(event -> {
            Guild guild = event.getGuild().block();
            if (ChannelPerms.isModuleOn(guild, getName())) {
                User user = event.getUser();
                DBDriver driver = ChannelPerms.getPermissionDB(guild);
                String channel = driver.getPerms(ChannelPerms.channelsTableName, ChannelPerms.channelsCol1, getName(), ChannelPerms.channelsCol2);
                if (channel != null && channel.length() > 0) {
                    return EschaUtil.sendMessage((MessageChannel) guild.getChannelById(Snowflake.of(Long.parseLong(channel))).block(),
                            user.getMention() + " : " + user.getUsername() + "#" + user.getDiscriminator() + " has left.");
                }
                driver.close();
            }
            return Mono.empty();
        }).subscribe();

        client.getEventDispatcher().on(BanEvent.class).flatMap(event -> {
            Guild guild = event.getGuild().block();
            if (ChannelPerms.isModuleOn(guild, getName())) {
                User user = event.getUser();
                DBDriver driver = ChannelPerms.getPermissionDB(guild);
                String channel = driver.getPerms(ChannelPerms.channelsTableName, ChannelPerms.channelsCol1, getName(), ChannelPerms.channelsCol2);
                if (channel != null && channel.length() > 0) {
                    return EschaUtil.sendMessage((MessageChannel) guild.getChannelById(Snowflake.of(Long.parseLong(channel))).block(),
                            user.getMention() + " : " + user.getUsername() + "#" + user.getDiscriminator() + " has been banned.");
                }
                driver.close();
            }
            return Mono.empty();
        }).subscribe();
    }

    @Override
    protected Map<String, Command> makeCommands() {
        return Collections.emptyMap();
    }

    @Override
    public String getName() {
        return "JoinLeave";
    }
}
