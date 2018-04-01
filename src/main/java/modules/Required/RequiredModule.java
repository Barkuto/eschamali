package modules.Required;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RequiredModule implements IModule {
    static IDiscordClient client;
    public static final String name = "Required";
    private modules.Required.RequiredListener pl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        pl = new modules.Required.RequiredListener();
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
