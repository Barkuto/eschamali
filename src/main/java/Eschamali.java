import modules.Games.GamesModule;
import modules.Parrot.ParrotModule;
import modules.Roles.RolesModule;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Iggie on 8/14/2016.
 */
public class Eschamali {
    private String clientID = "214442111720751104";
    public static TreeMap<IModule, Boolean> defaultmodules;
    public static TreeMap<IModule, Boolean> currentmodules;
    public static IDiscordClient client;

    public Eschamali() {
        Comparator<IModule> cmpr = new Comparator<IModule>() {

            @Override
            public int compare(IModule o1, IModule o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
        defaultmodules = new TreeMap<IModule, Boolean>(cmpr);
        currentmodules = new TreeMap<IModule, Boolean>(cmpr);
        GeneralModule general = new GeneralModule();
        RolesModule roles = new RolesModule();
        GamesModule games = new GamesModule();
        ParrotModule parrot = new ParrotModule();

        defaultmodules.put(general, true);
        defaultmodules.put(roles, true);
        defaultmodules.put(games, true);
        defaultmodules.put(parrot, false);

        currentmodules.put(general, true);
        currentmodules.put(roles, true);
        currentmodules.put(games, true);
        currentmodules.put(parrot, false);
    }

    public void run(String token) {
        try {
            client = new ClientBuilder().withToken(token).login();
            client.getDispatcher().registerListener(new Listener());

            for (Map.Entry<IModule, Boolean> entry : defaultmodules.entrySet()) {
                if (entry.getValue()) {
                    entry.getKey().enable(client);
                }
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
