package modules.Parrot;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/14/2016.
 */
public class ParrotModule implements IModule {
    static IDiscordClient client;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        ParrotModule.client = iDiscordClient;
        iDiscordClient.getDispatcher().registerListener(new ParrotListener());
        return true;
    }

    @Override
    public void disable() {

    }

    @Override
    public String getName() {
        return "Parrot";
    }

    @Override
    public String getAuthor() {
        return "Panda";
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