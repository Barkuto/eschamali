import modules.Games.GamesModule;
import modules.Roles.RolesModule;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/14/2016.
 */
public class Eschamali {
    private String clientID = "214442111720751104";
    private ArrayList<IModule> modules;
    public static IDiscordClient client;

    public Eschamali() {
        //Stuff
    }

    public void run() {
        try {
            client = new ClientBuilder().withToken("MjE0NDQyMTExNzIwNzUxMTA0.CpJCxA.AJ98RRi5B5VyP7lNjTBmR4ynPO0").login();
            client.getDispatcher().registerListener(new Listener());

            modules = new ArrayList<IModule>();
            RolesModule roles = new RolesModule();
            GamesModule games = new GamesModule();
            modules.add(roles);
            modules.add(games);

            roles.enable(client);
            games.enable(client);

        } catch (DiscordException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Eschamali e = new Eschamali();
        e.run();
    }
}
