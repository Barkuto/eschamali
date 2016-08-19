import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

/**
 * Created by Iggie on 8/19/2016.
 */
public class GeneralModule implements IModule {
    static IDiscordClient client;
    private GeneralListener gl;

    @Override
    public boolean enable(IDiscordClient iDiscordClient) {
        client = iDiscordClient;
        gl = new GeneralListener();
        client.getDispatcher().registerListener(gl);
        return true;
    }

    @Override
    public void disable() {
        client.getDispatcher().unregisterListener(gl);
    }

    @Override
    public String getName() {
        return "Escha";
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
