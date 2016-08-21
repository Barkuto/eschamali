package modules.Channels;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/20/2016.
 */
public class ChannelsModule implements IModule {

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        return false;
    }

    @Override
    public void disable() {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getMinimumDiscord4JVersion() {
        return null;
    }
}
