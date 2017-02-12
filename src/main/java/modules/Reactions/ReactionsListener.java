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

import java.awt.image.BufferedImage;

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
                if (message.replace("\'", "").toLowerCase().contains("dont quote me")) {
                    BufferedMessage.sendMessage(ReactionsModule.client, event, "\"" + message + "\" -" + author.getName());
                } else if (message.contains("alot")) {
                    BufferedMessage.sendMessage(ReactionsModule.client, event, "http://thewritepractice.com/wp-content/uploads/2012/05/Alot-vs-a-lot1-600x450.png");
                }
            }
        }
    }

    @EventSubscriber
    public void onMention(MentionEvent event) {

    }
}