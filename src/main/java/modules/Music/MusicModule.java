package modules.Music;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/21/2016.
 */
public class MusicModule implements IModule {
    static IDiscordClient client;
    static String name = "Music";
    private MusicListener ml;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        ml = new MusicListener();
        client.getDispatcher().registerListener(ml);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(ml);
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
