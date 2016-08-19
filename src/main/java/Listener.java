import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.modules.IModule;

import java.util.Map;

/**
 * Created by Iggie on 8/19/2016.
 */
public class Listener {
    String prefix = "!";

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        String[] args = message.split(" ");
        args[0] = args[0].replace(prefix, "").trim();
        String cmd = args[0];
        System.out.println(cmd);
        if (message.startsWith(prefix)) {
            if (cmd.equalsIgnoreCase("m") || cmd.equalsIgnoreCase("mods") || cmd.equalsIgnoreCase("modules")) {
                String msg = "`List of modules: `\n";
                for (Map.Entry<IModule, Boolean> entry : Eschamali.currentmodules.entrySet()) {
                    msg += (entry.getValue() ? ":o: " : ":x: ") + entry.getKey().getName() + "\n";
                }
                BufferedMessage.sendMessage(Eschamali.client, event, msg);
            } else if (cmd.equalsIgnoreCase("ddm") || cmd.equalsIgnoreCase("dam")) {
                for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                    entry.getKey().disable();
                    Eschamali.currentmodules.put(entry.getKey(), false);
                }
                BufferedMessage.sendMessage(Eschamali.client, event, "All modules have been disabled.");

            } else if (cmd.equalsIgnoreCase("edm")) {
                for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                    if (entry.getValue()) {
                        entry.getKey().enable(Eschamali.client);
                        Eschamali.currentmodules.put(entry.getKey(), true);
                    } else {
                        entry.getKey().disable();
                        Eschamali.currentmodules.put(entry.getKey(), false);
                    }
                }
                BufferedMessage.sendMessage(Eschamali.client, event, "Default modules have been enabled.");
            } else if (cmd.equalsIgnoreCase("eam")) {
                for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                    entry.getKey().enable(Eschamali.client);
                    Eschamali.currentmodules.put(entry.getKey(), true);
                }
                BufferedMessage.sendMessage(Eschamali.client, event, "All modules have been enabled.");
            } else if (cmd.equalsIgnoreCase("em")) {
                if (args.length > 1) {
                    for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                        if (entry.getKey().getName().equalsIgnoreCase(args[1])) {
                            entry.getKey().enable(Eschamali.client);
                            Eschamali.currentmodules.put(entry.getKey(), true);
                            BufferedMessage.sendMessage(Eschamali.client, event, "The " + entry.getKey().getName() + " module has been enabled.");
                            return;
                        }
                    }
                }
            } else if (cmd.equalsIgnoreCase("dm")) {
                if (args.length > 1) {
                    for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                        if (entry.getKey().getName().equalsIgnoreCase(args[1])) {
                            entry.getKey().disable();
                            Eschamali.currentmodules.put(entry.getKey(), false);
                            BufferedMessage.sendMessage(Eschamali.client, event, "The " + entry.getKey().getName() + " module has been disabled.");
                            return;
                        }
                    }
                }
            }
        }
    }
}
