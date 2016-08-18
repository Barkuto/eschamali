package modules.Misc;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/17/2016.
 */
public class MiscModule implements IModule {
    static IDiscordClient client;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        MiscModule.client = iDiscordClient;
        iDiscordClient.getDispatcher().registerListener(new MiscListener());
        return true;
    }

    @Override
    public void disable() {

    }

    @Override
    public String getName() {
        return "Misc";
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
