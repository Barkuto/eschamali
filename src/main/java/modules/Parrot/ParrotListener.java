package modules.Parrot;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IPrivateChannel;

/**
 * Created by Iggie on 8/14/2016.
 */
public class ParrotListener {

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            if (PermissionsListener.isModuleOn(event.getMessage().getGuild(), ParrotModule.name)
                    && PermissionsListener.canModuleInChannel(event.getMessage().getGuild(), ParrotModule.name, event.getMessage().getChannel())) {
                BufferedMessage.sendMessage(ParrotModule.client, event, event.getMessage().getContent());
            }
        }
    }
}
