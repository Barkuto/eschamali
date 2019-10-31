package base;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import modules.ChannelPerms;
import reactor.core.publisher.Mono;

import java.util.List;

public class EschaUtil {
    public static Command createMessageCommand(String text) {
        return event -> event.getMessage().getChannel().flatMap(channel -> channel.createMessage(text).then());
    }

    public static Command createMessageCommandGen(String text) {
        return event -> {
            if (ChannelPerms.canTalkInChannel(event.getGuild().block(), event.getMessage().getChannel().block()))
                return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(text).then());
            else return Mono.empty();
        };
    }

    public static String getMessage(MessageCreateEvent event) {
        return event.getMessage().getContent().isPresent() ? event.getMessage().getContent().get() : "";
    }

    public static String getArgsConcat(MessageCreateEvent event) {
        String msg = getMessage(event);
        if (msg.contains(" ")) return msg.substring(msg.indexOf(" ")).trim();
        return "";
    }

    public static String[] getArgs(MessageCreateEvent event) {
        String msg = getMessage(event);
        if (msg.contains(" ")) return msg.substring(msg.indexOf(" ")).trim().split(" ");
        return new String[0];
    }

    public static Mono<Void> sendMessage(MessageCreateEvent event, String message) {
        return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(message).then());
    }

    public static Mono<Void> sendMessage(MessageChannel channel, String message) {
        return channel.createMessage(message).then();
    }

    public static Mono<Void> sendMessage(PrivateChannel channel, String message) {
        return channel.createMessage(message).then();
    }

    public static boolean hasPermOr(Member member, Permission... perms) {
        if (Eschamali.ownerIDs.contains(member.getId().asLong())) return true;
        PermissionSet permSet = member.getBasePermissions().block();
        if (permSet == null) return false;
        for (Permission p : perms) {
            if (permSet.contains(p)) return true;
        }
        return false;
    }

    public static boolean hasPermAnd(Member member, Permission... perms) {
        if (Eschamali.ownerIDs.contains(member.getId().asLong())) return true;
        PermissionSet permSet = member.getBasePermissions().block();
        if (permSet == null) return false;
        for (Permission p : perms) {
            if (!permSet.contains(p)) return false;
        }
        return true;
    }

    public static Role roleFromGuild(Guild guild, String roleName) {
        List<Role> roles = guild.getRoles().collectList().block();
        for (Role r : roles) {
            if (r.getName().equalsIgnoreCase(roleName)) {
                return r;
            }
        }
        return null;
    }
}
