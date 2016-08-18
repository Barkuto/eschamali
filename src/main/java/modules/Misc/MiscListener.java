package modules.Misc;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IUser;

/**
 * Created by Iggie on 8/17/2016.
 */
public class MiscListener {
    private String prefix = "!";

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        String msg = event.getMessage().getContent();
        IUser user = event.getMessage().getAuthor();
    }
}
