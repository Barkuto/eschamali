package modules.BufferedMessage;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.*;

/**
 * Created by Iggie on 8/17/2016.
 */
public class BufferedMessage {
    public static IMessage sendMessage(IDiscordClient client, MessageReceivedEvent event, String message) {
        final IMessage[] m = new IMessage[1];
        RequestBuffer.request(() -> {
            try {
                m[0] = new MessageBuilder(client).withChannel(event.getMessage().getChannel()).withContent(message).build();
            } catch (RateLimitException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
                e.printStackTrace();
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
            }
        });
        return m[0];

    }

//    public static IMessage sendMessage(IDiscordClient client, GuildCreateEvent event, String message) {
//        final IMessage[] m = new IMessage[1];
//        RequestBuffer.request(() -> {
//            try {
//                m[0] = new MessageBuilder(client).withChannel(event.getGuild().getChannels().get(0)).withContent(message).build();
//            } catch (RateLimitException e) {
//                e.printStackTrace();
//            } catch (DiscordException e) {
//                e.printStackTrace();
//            } catch (MissingPermissionsException e) {
//                e.printStackTrace();
//            }
//        });
//        return m[0];
//    }

    public static IMessage sendMessage(IDiscordClient client, IChannel channel, String message) {
        final IMessage[] m = new IMessage[1];
        RequestBuffer.request(() -> {
            try {
                m[0] = new MessageBuilder(client).withChannel(channel).withContent(message).build();
            } catch (RateLimitException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
                e.printStackTrace();
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
            }
        });
        return m[0];
    }

    public static IMessage sendEmbed(IDiscordClient client, MessageReceivedEvent event, EmbedObject embed) {
        final IMessage[] m = new IMessage[1];
        RequestBuffer.request(() -> {
            try {
                m[0] = new MessageBuilder(client).withChannel(event.getMessage().getChannel()).withEmbed(embed).build();
            } catch (RateLimitException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
                e.printStackTrace();
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
            }
        });
        return m[0];
    }

    public static IMessage sendEmbed(IDiscordClient client, IChannel channel, EmbedObject embed) {
        final IMessage[] m = new IMessage[1];
        RequestBuffer.request(() -> {
            try {
                m[0] = new MessageBuilder(client).withChannel(channel).withEmbed(embed).build();
            } catch (RateLimitException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
                e.printStackTrace();
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
            }
        });
        return m[0];
    }
}
