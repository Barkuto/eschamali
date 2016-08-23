package modules.Channels;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/20/2016.
 */
public class ChannelsModule implements IModule {
    static IDiscordClient client;
    public static final String name = "Channels";
    private ChannelsListener cl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        cl = new ChannelsListener();
        client.getDispatcher().registerListener(cl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(cl);
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
