package modules.Parrot;

import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

/**
 * Created by Iggie on 8/14/2016.
 */
public class ParrotListener {

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        BufferedMessage.sendMessage(ParrotModule.client, event, event.getMessage().getContent());
    }
}
