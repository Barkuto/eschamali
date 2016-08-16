import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.obj.Message;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RolesListener {
    private String prefix = ".";

    @EventSubscriber
    public void messageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        if (message.startsWith(prefix + "iam")) {
            String[] args = message.split(" ");
            if(args.length > 1){

            }
        }
    }
}
