package modules.Reactions;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MentionEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;

/**
 * Created by Iggie on 11/17/2016.
 */
public class ReactionsListener {

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            String message = event.getMessage().getContent();
            IUser author = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, ReactionsModule.name) && PermissionsListener.canModuleInChannel(guild, ReactionsModule.name, channel)) {
                System.out.println("Derp");
                if (message.replace("\'", "").toLowerCase().contains("dont quote me")) {
                    System.out.println(message.replace("\'", "").toLowerCase());
                    BufferedMessage.sendMessage(ReactionsModule.client, event, "\"" + message + "\" -" + author.getName());
                }
            }
        }
    }

    @EventSubscriber
    public void onMention(MentionEvent event) {

    }
}
