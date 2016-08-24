package modules.Music;

import base.ICommands;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/21/2016.
 */
public class MusicModule implements IModule, ICommands {
    static IDiscordClient client;
    public static final String name = "Music";
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

    @Override
    public ArrayList<String> commands() {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(MusicListener.prefix);
        cmds.add("`join`: Makes the bot join your voice channel. **USAGE**: join");
        cmds.add("`leave`: Makes the bot leave its current voice channel. **USAGE**: leave");
        cmds.add("`play`: Resumes playback if paused. **USAGE**: play");
        cmds.add("`stop`: Stops playback if playing. **USAGE**: stop");
        cmds.add("`pause`: Pauses playback if playing. **USAGE**: pause");
        cmds.add("`reset`: Resets the music player. **USAGE**: reset");
        cmds.add("`skip`: Skips the current playing song. **USAGE**: skip");
        cmds.add("`next`: Shows what the next song in queue is. **USAGE**: next");
        cmds.add("`nowplaying`: Shows what is currently playing. **USAGE**: nowplaying");
        cmds.add("`queue`: Queues a video/song from a url. **USAGE**: queue \"youtube link\"|\"youtube keywords\"");
        cmds.add("`queuerelated`: Queues a related video from the most recently queued youtube video. **USAGE**: queuerelated");
        return cmds;
    }
}
