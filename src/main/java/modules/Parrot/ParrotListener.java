package modules.Parrot;

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
        try {
            new MessageBuilder(ParrotModule.client).withChannel(event.getMessage().getChannel()).withContent(event.getMessage().getContent()).build();
        } catch (RateLimitException e) {
            e.printStackTrace();
        } catch (DiscordException e) {
            e.printStackTrace();
        } catch (MissingPermissionsException e) {
            e.printStackTrace();
        }
    }
}
