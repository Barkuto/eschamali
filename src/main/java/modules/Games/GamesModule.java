package modules.Games;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/17/2016.
 */
public class GamesModule implements IModule {
    static IDiscordClient client;
    public static final String name = "Games";
    private GamesListener gl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        gl = new GamesListener();
        client.getDispatcher().registerListener(gl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(gl);
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
        return "2.5.3";
    }
}
