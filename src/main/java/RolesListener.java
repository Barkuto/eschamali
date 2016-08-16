import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RolesListener {
    private String prefix = ".";

    @EventSubscriber
    public void messageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        if (message.startsWith(prefix)) {
            String[] args = message.split(" ");
            args[0] = args[0].substring(1, args[0].length());
            if (args[0].equalsIgnoreCase("iam")) {
                if (args.length > 1) {
                    String role = "";
                    for (int i = 1; i < args.length; i++) {
                        role += args[i] + " ";
                    }
                    role = role.trim();
                    Collection<IRole> roles = RolesModule.client.getRoles();
                    for (IRole r : roles) {
                        if (r.getName().equalsIgnoreCase(role)) {
                            try {
                                new MessageBuilder(RolesModule.client).withChannel(event.getMessage().getChannel()).withContent("That is a role").build();
                            } catch (RateLimitException e) {
                                e.printStackTrace();
                            } catch (DiscordException e) {
                                e.printStackTrace();
                            } catch (MissingPermissionsException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        }
    }
}
