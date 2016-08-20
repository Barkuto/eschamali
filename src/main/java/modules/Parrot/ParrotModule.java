package modules.Parrot;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/14/2016.
 */
public class ParrotModule implements IModule {
    static IDiscordClient client;
    static String name = "Parrot";
    private ParrotListener pl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        pl = new ParrotListener();
        client.getDispatcher().registerListener(pl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(pl);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAuthor() {
        return "Barkuto";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getMinimumDiscord4JVersion() {
        return "2.5.2";
    }
}
