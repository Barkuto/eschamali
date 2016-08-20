package modules.Roles;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RolesModule implements IModule {
    static IDiscordClient client;
    static String name = "Roles";
    private RolesListener rl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        rl = new RolesListener();
        client.getDispatcher().registerListener(rl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(rl);
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
        return "2.5.4";
    }
}
