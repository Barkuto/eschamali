import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.obj.Role;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.handle.obj.Status;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.*;

import java.awt.*;
import java.io.*;
import java.nio.Buffer;
import java.util.*;

/**
 * Created by Iggie on 8/14/2016.
 */
public class GeneralListener {
    private boolean ayy = false;

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        Eschamali.client.changeStatus(Status.game("with REM rates"));
    }

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
//        BufferedMessage.sendMessage(Eschamali.client, event, "Online.");
//        IGuild guild = event.getGuild();
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        String msg = event.getMessage().getContent();
        if (msg.equalsIgnoreCase("!donate")) {
            BufferedMessage.sendMessage(Eschamali.client, event, "Donate for server/development funds at: https://www.twitchalerts.com/donate/barkuto");
        } else if (msg.equalsIgnoreCase("!maker")) {
            BufferedMessage.sendMessage(Eschamali.client, event, "Made by **Barkuto**#2315 specifically for the PAD w/ MZeus server. Down with A.LB!");
        } else if (msg.equalsIgnoreCase("!ayy")) {
            java.util.List<IRole> roles = event.getMessage().getAuthor().getRolesForGuild(event.getMessage().getGuild());
            for (IRole r : roles) {
                if (r.getPermissions().contains(Permissions.ADMINISTRATOR) || r.getPermissions().contains(Permissions.MANAGE_SERVER)) {
                    ayy = !ayy;
                }
            }
        } else if (msg.equalsIgnoreCase("tilt")) {
            BufferedMessage.sendMessage(Eschamali.client, event, "*T* *I* *L* *T*");
        } else if (msg.equalsIgnoreCase("riot")) {
            BufferedMessage.sendMessage(Eschamali.client, event, "ヽ༼ຈل͜ຈ༽ﾉ RIOT ヽ༼ຈل͜ຈ༽ﾉ");
        } else if (msg.equalsIgnoreCase("ayy") && ayy) {
            BufferedMessage.sendMessage(Eschamali.client, event, "lmao");
        }


    }
}
