package modules.Parrot;

import base.ModuleListener;
import modules.BufferedMessage.BufferedMessage;
import modules.Channels.ChannelsListener;
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
            if (ModuleListener.isModuleOn(event.getMessage().getGuild(), ParrotModule.name) && ChannelsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel().getName())) {
                BufferedMessage.sendMessage(ParrotModule.client, event, event.getMessage().getContent());
            }
        }
    }
}
