package modules.BufferedMessage;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.*;

/**
 * Created by Iggie on 8/17/2016.
 */
public class BufferedMessage {
    public static IMessage sendMessage(IDiscordClient client, MessageReceivedEvent event, String message) {
        final IMessage[] m = new IMessage[1];
        RequestBuffer.request(() -> m[0] = new MessageBuilder(client).withChannel(event.getMessage().getChannel()).withContent(message).build());
        return m[0];

    }

    public static IMessage sendMessage(IDiscordClient client, IChannel channel, String message) {
        final IMessage[] m = new IMessage[1];
        RequestBuffer.request(() -> m[0] = new MessageBuilder(client).withChannel(channel).withContent(message).build());
        return m[0];
    }

    public static IMessage sendEmbed(IDiscordClient client, MessageReceivedEvent event, EmbedObject embed) {
        final IMessage[] m = new IMessage[1];
        RequestBuffer.request(() -> m[0] = new MessageBuilder(client).withChannel(event.getMessage().getChannel()).withEmbed(embed).build());
        return m[0];
    }

    public static IMessage sendEmbed(IDiscordClient client, IChannel channel, EmbedObject embed) {
        final IMessage[] m = new IMessage[1];
        RequestBuffer.request(() -> m[0] = new MessageBuilder(client).withChannel(channel).withEmbed(embed).build());
        return m[0];
    }
}
