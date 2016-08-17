package modules.BufferedMessage;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.util.*;

/**
 * Created by Iggie on 8/17/2016.
 */
public class BufferedMessage {
    public static void sendMessage(IDiscordClient client, MessageReceivedEvent event, String message) {
        RequestBuffer.request(() -> {
            try {
                new MessageBuilder(client).withChannel(event.getMessage().getChannel()).withContent(message).build();
            } catch (RateLimitException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
                e.printStackTrace();
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendMessage(IDiscordClient client, GuildCreateEvent event, String message) {
        RequestBuffer.request(() -> {
            try {
                new MessageBuilder(client).withChannel(event.getGuild().getChannels().get(0)).withContent(message).build();
            } catch (RateLimitException e) {
                e.printStackTrace();
            } catch (DiscordException e) {
                e.printStackTrace();
            } catch (MissingPermissionsException e) {
                e.printStackTrace();
            }
        });
    }
}
