package modules.Roles;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/14/2016.
 */
public class RolesModule implements IModule {
    public static IDiscordClient client;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        RolesModule.client = iDiscordClient;
        iDiscordClient.getDispatcher().registerListener(new RolesListener());
        return true;
    }

    @Override
    public void disable() {

    }

    @Override
    public String getName() {
        return "Roles";
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
