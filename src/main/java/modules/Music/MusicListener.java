package modules.Music;

import base.ModuleListener;
import modules.BufferedMessage.BufferedMessage;
import modules.Channels.ChannelsListener;
import net.dv8tion.d4j.player.MusicPlayer;
import net.dv8tion.jda.player.Playlist;
import net.dv8tion.jda.player.source.AudioInfo;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.AudioTimestamp;
import org.apache.commons.io.IOUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.audio.IAudioManager;
import sx.blah.discord.handle.audio.impl.DefaultProvider;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Iggie on 8/21/2016.
 */
public class MusicListener {
    private String prefix = "!";
    private final float DEFAULT_VOLUME = 0.5f;

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (ModuleListener.isModuleOn(event.getMessage().getGuild(), MusicModule.name) && ChannelsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel().getName())) {
            String msg = event.getMessage().getContent();
            String[] split = msg.split(" ");
            String cmd = split[0].replace(prefix, "");
            IUser user = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();

            IAudioManager manager = guild.getAudioManager();
            MusicPlayer player;
            if (manager.getAudioProvider() instanceof DefaultProvider) {
                player = new MusicPlayer();
                player.setVolume(DEFAULT_VOLUME);
                manager.setAudioProvider(player);
            } else {
                player = (MusicPlayer) manager.getAudioProvider();
            }

            if (cmd.equalsIgnoreCase("join")) {
                List<IVoiceChannel> vChannels = user.getConnectedVoiceChannels();
                if (vChannels.size() > 0) {
                    try {
                        vChannels.get(0).join();
                    } catch (MissingPermissionsException e) {
                        e.printStackTrace();
                    }
                } else {
                    BufferedMessage.sendMessage(MusicModule.client, event, "You must join a voice channel first.");
                }
            } else if (cmd.equalsIgnoreCase("leave")) {
                List<IVoiceChannel> connectedVC = MusicModule.client.getConnectedVoiceChannels();
                for (IVoiceChannel vc : connectedVC) {
                    if (vc.getGuild().equals(guild)) {
                        vc.leave();
                    }
                }
            } else if (cmd.equalsIgnoreCase("play")) {
                if (player.isPlaying()) {
                    BufferedMessage.sendMessage(MusicModule.client, event, "Player is already playing!");
                    return;
                } else if (player.isPaused()) {
                    player.play();
                    BufferedMessage.sendMessage(MusicModule.client, event, "Playback has been **RESUMED**.");
                } else {
                    if (player.getAudioQueue().isEmpty())
                        BufferedMessage.sendMessage(MusicModule.client, event, "The current audio queue is empty! Add something to the queue first!");
                    else {
                        player.play();
                        BufferedMessage.sendMessage(MusicModule.client, event, "Player has started playing!");
                    }
                }
            } else if (cmd.equalsIgnoreCase("stop")) {
                player.stop();
                BufferedMessage.sendMessage(MusicModule.client, event, "Playback has been **STOPPED**.");
            } else if (cmd.equalsIgnoreCase("pause")) {
                player.pause();
                BufferedMessage.sendMessage(MusicModule.client, event, "Playback has been **PAUSED**.");
            } else if (cmd.equalsIgnoreCase("reset")) {
                player.stop();
                player = new MusicPlayer();
                player.setVolume(DEFAULT_VOLUME);
                manager.setAudioProvider(player);
                BufferedMessage.sendMessage(MusicModule.client, event, "Playback has been **RESET**.");
            } else if (cmd.equalsIgnoreCase("skip")) {
                player.skipToNext();
                BufferedMessage.sendMessage(MusicModule.client, event, "Skipped current song.");
            } else if (cmd.equalsIgnoreCase("next")) {
            } else if (cmd.equals("nowplaying") || cmd.equalsIgnoreCase("np") || cmd.equalsIgnoreCase("playing")) {
                if (player.isPlaying()) {
                    AudioTimestamp currentTime = player.getCurrentTimestamp();
                    AudioInfo info = player.getCurrentAudioSource().getInfo();
                    if (info.getError() == null) {
                        BufferedMessage.sendMessage(MusicModule.client, event, ":notes: **NOW PLAYING** :notes:\n`" + currentTime.getTimestamp() + "/" + info.getDuration().getTimestamp() + "` __" + info.getTitle() + "__");
                    } else {
                        BufferedMessage.sendMessage(MusicModule.client, event, ":notes: **NOW PLAYING** :notes:\n`" + currentTime.getTimestamp() + "/(N/A)" + "` __" + player.getCurrentAudioSource().getSource() + "__");
                    }
                } else {
                    BufferedMessage.sendMessage(MusicModule.client, event, "There is nothing playing!");
                }
                String output = "__Now playing:__";
            } else if (cmd.equalsIgnoreCase("queue") || cmd.equalsIgnoreCase("q")) {
                if (split.length > 1) {
                    if (split[1].contains("youtu")) {//URL load
                        String url = split[1];
                        if (url.contains("list=")) {
                            url = url.substring(0, url.indexOf("list="));
                        }
                        Playlist playlist = Playlist.getPlaylist(url);
                        List<AudioSource> sources = new LinkedList<AudioSource>(playlist.getSources());

                        if (sources.size() > 1) {
                            //future playlist handling?
                        } else {
                            AudioSource source = sources.get(0);
                            AudioInfo info = source.getInfo();
                            if (info.getError() == null) {
                                player.getAudioQueue().add(source);
                                BufferedMessage.sendMessage(MusicModule.client, event, "Queued.");
                                if (player.isStopped()) {
                                    player.play();
                                }
                            } else {
                                BufferedMessage.sendMessage(MusicModule.client, event, "There was a problem while loading that video.");
                            }
                        }
                    } else {//keyword search
                        String query = "";
                        for (int i = 1; i < split.length; i++) {
                            query += split[i] + "+";
                        }
                        query = query.substring(0, query.lastIndexOf('+'));
                        try {
                            URL page = new URL("https://www.youtube.com/results?search_query=" + query);
                            String html = IOUtils.toString(page.openStream(), "utf-8");
                            html = html.substring(html.indexOf("<li><div class=\"yt"));
                            html = html.substring(html.indexOf("href"));
                            html = html.substring(0, html.indexOf("class="));
                            html = html.substring(html.indexOf('"') + 1, html.lastIndexOf('"'));
                            String youtubeurl = "https://www.youtube.com" + html;
                            System.out.println(youtubeurl);

                            AudioSource source = Playlist.getPlaylist(youtubeurl).getSources().get(0);
                            player.getAudioQueue().add(source);
                            BufferedMessage.sendMessage(MusicModule.client, event, "Queued.");
                            if (player.isStopped()) {
                                player.play();
                            }
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
