package base;

import modules.Channels.ChannelsListener;
import modules.Channels.ChannelsModule;
import modules.Games.GamesModule;
import modules.Parrot.ParrotModule;
import modules.Roles.RolesModule;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by Iggie on 8/14/2016.
 */
public class Eschamali {
    private String clientID = "214442111720751104";
    public static ArrayList<IModule> modules;
    public static TreeMap<IModule, Boolean> defaultmodules;
    public static IDiscordClient client;
    static String ownerID = "85844964633747456";

    public Eschamali() {
        Comparator<IModule> cmpr = new Comparator<IModule>() {
            @Override
            public int compare(IModule o1, IModule o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
        defaultmodules = new TreeMap<IModule, Boolean>(cmpr);
        RolesModule roles = new RolesModule();
        GamesModule games = new GamesModule();
        ParrotModule parrot = new ParrotModule();
        ChannelsModule channels = new ChannelsModule();

        defaultmodules.put(roles, true);
        defaultmodules.put(games, true);
        defaultmodules.put(parrot, false);
        defaultmodules.put(channels, true);

        modules = new ArrayList<IModule>();
        modules.add(roles);
        modules.add(games);
        modules.add(parrot);
        modules.add(channels);
    }

    public void run(String token) {
        try {
            client = new ClientBuilder().withToken(token).login();
            client.getDispatcher().registerListener(new ModuleListener());
            client.getDispatcher().registerListener(new GeneralListener());

            for (IModule m : modules) {
                m.enable(client);
            }

        } catch (DiscordException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Eschamali e = new Eschamali();
        e.run(args[0]);
    }
}
