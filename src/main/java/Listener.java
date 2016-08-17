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
import sx.blah.discord.util.*;

import java.awt.*;
import java.nio.Buffer;
import java.util.EnumSet;

/**
 * Created by Iggie on 8/14/2016.
 */
public class Listener {

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        Eschamali.client.changeStatus(Status.game("with REM rates"));
        Thread t = new Thread("Pinger") {
            public void run() {
                while (true) {
                    System.out.println("ping");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        };
        t.start();
    }

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
//        BufferedMessage.sendMessage(Eschamali.client, event, "Hello!");
    }

//    @EventSubscriber
//    public void onMessage(MessageReceivedEvent event){
//        try {
//            new MessageBuilder(Eschamali.client).withChannel(event.getMessage().getChannel()).withContent(event.getMessage().getContent()).build();
//        } catch (RateLimitException e) {
//            e.printStackTrace();
//        } catch (DiscordException e) {
//            e.printStackTrace();
//        } catch (MissingPermissionsException e) {
//            e.printStackTrace();
//        }
//    }
}
