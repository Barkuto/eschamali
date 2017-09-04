package modules.Music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.PermissionsListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Iggie on 2/23/2017.
 */
public class MusicListener {
    public static String prefix = "!";
    private AudioPlayerManager playerManager;
    private HashMap<String, GuildMusicManager> guildManagers;

    @EventSubscriber
    public void onJoin(GuildCreateEvent e) {
        playerManager = new DefaultAudioPlayerManager();
        guildManagers = new HashMap<>();
        AudioSourceManagers.registerLocalSource(playerManager);
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            String message = event.getMessage().getContent();
            IUser user = event.getMessage().getAuthor();
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, MusicModule.name)
                    && PermissionsListener.canModuleInChannel(guild, MusicModule.name, channel)) {
                if (event.getMessage().getContent().startsWith(prefix)) {
                    String[] args = message.split(" ");
                    args[0] = args[0].replace(prefix, "").trim();
                    String cmd = args[0];
                    String argsconcat;
                    try {
                        argsconcat = message.substring(cmd.length() + 2, message.length()).trim();
                    } catch (StringIndexOutOfBoundsException e) {
                        argsconcat = "";
                    }

                    GuildMusicManager guildManager = getGuildManager(guild);
                    if (cmd.equalsIgnoreCase("join") || cmd.equalsIgnoreCase("j")) {
                        joinVoiceChannel(user, guild, channel);
                    } else if (cmd.equalsIgnoreCase("leave") || cmd.equalsIgnoreCase("l")) {
                        leaveVoiceChannel(guild);
                    } else if (cmd.equalsIgnoreCase("queue") || cmd.equalsIgnoreCase("q")) {
                        if (args.length > 1) {
                            if (!argsconcat.contains("www.") || !argsconcat.contains(".com")
                                    || !argsconcat.contains("youtu.be") || !argsconcat.contains("/")) {
                                args[1] = youtubeVideoFromKeywords(argsconcat);
                            }
                            playerManager.loadItemOrdered(guildManager, args[1], new AudioLoadResultHandler() {
                                @Override
                                public void trackLoaded(AudioTrack audioTrack) {
                                    guildManager.scheduler.queue(audioTrack);
                                    BufferedMessage.sendMessage(MusicModule.client, event, "Queued `" + audioTrack.getInfo().title + "`");
                                }

                                @Override
                                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                                    //Load playlist handling
                                }

                                @Override
                                public void noMatches() {
                                    //Search by keyword
                                    BufferedMessage.sendMessage(MusicModule.client, event, "Nothing found from \"" + args[1] + "\"");
                                }

                                @Override
                                public void loadFailed(FriendlyException e) {
                                    BufferedMessage.sendMessage(MusicModule.client, event, "Loading failed.");
                                }
                            });
                            joinVoiceChannel(user, guild, channel);
                        } else {
                            AudioTrack currentTrack = guildManager.player.getPlayingTrack();
                            BlockingQueue<AudioTrack> queue = guildManager.scheduler.getQueue();
                            String out = "```NOW PLAYING: ";
                            out += currentTrack != null ? currentTrack.getInfo().title : "Nothing is playing, queue something!";
                            out += "\nQUEUED:\n";
                            int i = 0;
                            for (AudioTrack t : queue) {
                                if (i == 10) {
                                    break;
                                } else {
                                    AudioTrackInfo info = t.getInfo();
                                    int[] length = parseLength(info.length);
                                    out += (i + 1) + ". " + String.format("%02d:%02d:%02d - ", length[0], length[1], length[2]) + info.title + "\n";
                                    i++;
                                }
                            }
                            BufferedMessage.sendMessage(MusicModule.client, event, out + "```");
                        }
                    } else if (cmd.equalsIgnoreCase("queuerelated") || cmd.equalsIgnoreCase("qr")) {
                        BlockingQueue<AudioTrack> queue = guildManager.scheduler.getQueue();
                        AudioTrack lastInQueue = guildManager.player.getPlayingTrack();
                        for (AudioTrack t : queue) {
                            lastInQueue = t;
                        }
                        if (lastInQueue != null) {
                            String url = getRelatedYoutube("https://www.youtube.com/watch?v=" + lastInQueue.getInfo().identifier);
                            if (url != null) {
                                playerManager.loadItemOrdered(guildManager, url, new AudioLoadResultHandler() {
                                    @Override
                                    public void trackLoaded(AudioTrack audioTrack) {
                                        guildManager.scheduler.queue(audioTrack);
                                        BufferedMessage.sendMessage(MusicModule.client, event, "Queued `" + audioTrack.getInfo().title + "`");
                                    }

                                    @Override
                                    public void playlistLoaded(AudioPlaylist audioPlaylist) {
                                        //Load playlist handling
                                    }

                                    @Override
                                    public void noMatches() {
                                        //Search by keyword
                                        BufferedMessage.sendMessage(MusicModule.client, event, "Nothing found from \"" + args[1] + "\"");
                                    }

                                    @Override
                                    public void loadFailed(FriendlyException e) {
                                        BufferedMessage.sendMessage(MusicModule.client, event, "Loading failed.");
                                    }
                                });
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("skip") || cmd.equalsIgnoreCase("s")) {
                        if (guildManager.scheduler.isPlaying()) {
                            guildManager.scheduler.nextTrack();
                            BufferedMessage.sendMessage(MusicModule.client, event, "Current track was skipped.");
                        }
                    } else if (cmd.equalsIgnoreCase("pause") || cmd.equalsIgnoreCase("p")) {
                        if (guildManager.scheduler.isPlaying()) {
                            guildManager.player.setPaused(true);
                            BufferedMessage.sendMessage(MusicModule.client, event, "Player was paused.");
                        }
                    } else if (cmd.equalsIgnoreCase("unpause") || cmd.equalsIgnoreCase("up")
                            || cmd.equalsIgnoreCase("resume") || cmd.equalsIgnoreCase("r")) {
                        if (guildManager.player.isPaused()) {
                            guildManager.player.setPaused(false);
                            BufferedMessage.sendMessage(MusicModule.client, event, "Player was unpaused.");
                        }
                    } else if (cmd.equalsIgnoreCase("nowplaying") || cmd.equalsIgnoreCase("np")
                            || cmd.equalsIgnoreCase("currenttrack") || cmd.equalsIgnoreCase("ct")) {
                        AudioTrack currentTrack = guildManager.player.getPlayingTrack();
                        int[] currentTime = parseLength(currentTrack.getPosition());
                        int[] totalLength = parseLength(currentTrack.getDuration());
                        String current = String.format("%02d:%02d:%02d", currentTime[0], currentTime[1], currentTime[2]);
                        String end = String.format("%02d:%02d:%02d", totalLength[0], totalLength[1], totalLength[2]);
                        BufferedMessage.sendMessage(MusicModule.client, event, "Now Playing: **" + currentTrack.getInfo().title + "** `" + current + "/" + end + "`");
                    } else if (cmd.equalsIgnoreCase("stop")) {
                        while (guildManager.player.getPlayingTrack() != null) {
                            guildManager.scheduler.nextTrack();
                        }
                        leaveVoiceChannel(guild);
                        BufferedMessage.sendMessage(MusicModule.client, event, "Player was stopped.");
                    }
                }
            }
        }
    }

    private GuildMusicManager getGuildManager(IGuild guild) {
        GuildMusicManager guildManager = guildManagers.get(guild.getStringID());
        if (guildManager == null) {
            guildManager = new GuildMusicManager(playerManager);
            guildManager.player.setVolume(50);
            guildManagers.put(guild.getStringID(), guildManager);
        }
        guild.getAudioManager().setAudioProvider(guildManager.getAudioProvider());
        return guildManager;
    }

    private void connectToVoiceOf(IUser user, IGuild guild, IChannel channel) {
        IVoiceChannel userVC = user.getVoiceStateForGuild(guild).getChannel();
        if (userVC == null)
            BufferedMessage.sendMessage(MusicModule.client, channel, "You are not in a voice channel.");
        else userVC.join();
    }

    private boolean isConnectedToVC(IGuild guild) {
        for (IVoiceChannel vc : guild.getVoiceChannels()) {
            if (vc.isConnected()) {
                return true;
            }
        }
        return false;
    }

    private void joinVoiceChannel(IUser user, IGuild guild, IChannel channel) {
        if (!isConnectedToVC(guild)) {
            connectToVoiceOf(user, guild, channel);
        }
    }

    private void leaveVoiceChannel(IGuild guild) {
        for (IVoiceChannel vc : guild.getVoiceChannels()) {
            if (vc.isConnected()) {
                vc.leave();
                break;
            }
        }
    }

    private int[] parseLength(long longLength) {
        int sec = (int) (longLength / 1000);
        int min = sec / 60;
        int hr = min / 60;
        sec = sec - min * 60;
        min = min - hr * 60;
        return new int[]{hr, min, sec};
    }

    private String youtubeVideoFromKeywords(String keywords) {
        String query = keywords.replaceAll(" ", "+");
        try {
            URL page = new URL("https://www.youtube.com/results?search_query=" + query);
            Document doc = Jsoup.parse(page, 15000);
            Elements es = doc.select("a[aria-hidden]");
            int i = 0;
            Element e = es.get(i);
            while (e.toString().contains("googleads")) {
                i++;
                e = es.get(i);
            }
            String part = e.toString();
            String youtubeurl = "https://www.youtube.com" + part.substring(part.indexOf("href=") + 6, part.indexOf("class=") - 2);
            return youtubeurl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getRelatedYoutube(String url) {
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            try {
                URL page = new URL(url);
                Document d = Jsoup.parse(page, 15000);
                Elements elements = d.select("a[href]");
                Element e = elements.get(0);
                for (int i = 0; i < elements.size(); i++) {
                    if (elements.get(i).toString().contains("/watch")) {
                        e = elements.get(i);
                        break;
                    }
                }
                String part = e.toString();
                String watchid = part.substring(part.indexOf("href=") + 6, part.indexOf("class=") - 2);
                String youtubeurl = "https://www.youtube.com" + watchid;
                System.out.println(youtubeurl);
                return youtubeurl;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
