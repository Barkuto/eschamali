import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

/**
 * Created by Iggie on 8/14/2016.
 */
public class Eschamali {
    private String clientID = "214442111720751104";
    public static IDiscordClient client;

    public Eschamali() {
        //Stuff
    }

    public void run() {
        try {
            System.out.println("Logging in...");
            client = new ClientBuilder().withToken("MjE0NDQyMTExNzIwNzUxMTA0.CpJCxA.AJ98RRi5B5VyP7lNjTBmR4ynPO0").login();
            System.out.println("Logged in!");
            client.getDispatcher().registerListener(new Listener());
            System.out.println("Listener added.");

            RolesModule rm = new RolesModule();
            rm.enable(client);

        } catch (DiscordException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Eschamali e = new Eschamali();
        e.run();
    }
}
