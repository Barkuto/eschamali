package modules.Parrot;

import modules.BufferedMessage.BufferedMessage;
import modules.Parrot.ParrotModule;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * Created by Iggie on 8/14/2016.
 */
public class ParrotListener {

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        BufferedMessage.sendMessage(ParrotModule.client, event, event.getMessage().getContent());
    }
}
