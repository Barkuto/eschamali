package modules.BufferedMessage;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.*;

import java.io.File;

/**
 * Created by Iggie on 8/17/2016.
 */
public class Sender {

    public static void sendMessage(IChannel channel, String message) {
        RequestBuffer.request(() -> channel.sendMessage(message));
    }

    public static void sendEmbed(IChannel channel, EmbedObject embed) {
        RequestBuffer.request(() -> channel.sendMessage(embed));
    }
}
