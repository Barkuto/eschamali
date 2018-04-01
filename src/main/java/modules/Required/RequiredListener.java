package modules.Required;

import modules.BufferedMessage.Sender;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RequiredListener {
    private String prefix = "[";
    private ArrayList<String> words = new ArrayList<>();

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            IMessage m = event.getMessage();
            String message = event.getMessage().getContent();
            IUser author = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, RequiredModule.name)
                    && PermissionsListener.canModuleInChannel(guild, RequiredModule.name, channel)) {
                if (message.startsWith(prefix)) {
                    String[] args = message.split(" ");
                    args[0] = args[0].replace(prefix, "").trim();
                    String cmd = args[0];
                    String argsconcat;
                    try {
                        argsconcat = message.substring(cmd.length() + 2, message.length()).trim();
                    } catch (StringIndexOutOfBoundsException e) {
                        argsconcat = "";
                    }

                    if (userHasPerm(author, guild, Permissions.MANAGE_MESSAGES)) {
                        if (cmd.equalsIgnoreCase("arw")) {
                            for (int i = 1; i < args.length; i++) {
                                if (!words.contains(args[i].toLowerCase()))
                                    words.add(args[i]);
                                Sender.sendMessage(channel, "Added: " + argsconcat);
                            }
                        } else if (cmd.equalsIgnoreCase("drw")) {
                            for (int i = 1; i < args.length; i++) {
                                if (words.contains(args[i].toLowerCase()))
                                    words.remove(args[i]);
                                Sender.sendMessage(channel, "Removed: " + argsconcat);
                            }
                        }
                    }
                } else {
                    for (String s : words) {
                        if (message.contains(s))
                            return;
                    }
                    m.delete();
                }
            }
        }
    }

    private boolean userHasPerm(IUser user, IGuild guild, Permissions perm) {
        List<IRole> roles = user.getRolesForGuild(guild);
        for (IRole r : roles) {
            if (r.getPermissions().contains(perm)) {
                return true;
            }
        }
        return false;
    }
}
