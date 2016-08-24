package modules.Channels;

import base.ModuleListener;
import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Iggie on 8/20/2016.
 */
public class ChannelsListener {
    public static String prefix = ";";
    private String ownerID = "85844964633747456";

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        File channelsF = new File("servers/" + event.getGuild().getName() + "-" + event.getGuild().getID() + "/channels.txt");
        if (!channelsF.exists()) {
            channelsF.getParentFile().mkdirs();
            try {
                channelsF.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            if (ModuleListener.isModuleOn(event.getMessage().getGuild(), ChannelsModule.name) && canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel().getName())) {
                if (userHasPerm(event.getMessage().getAuthor(), event.getMessage().getGuild(), Permissions.MANAGE_ROLES) || event.getMessage().getAuthor().getID().equals(ownerID)) {
                    String msg = event.getMessage().getContent();
                    IUser user = event.getMessage().getAuthor();
                    IGuild guild = event.getMessage().getGuild();
                    if (msg.startsWith(prefix)) {
                        String[] split = msg.split(" ");
                        String cmd = split[0].replace(prefix, "");
                        String argsconcat = "";
                        for (int i = 1; i < split.length; i++) {
                            argsconcat += split[i] + " ";
                        }
                        argsconcat = argsconcat.trim();
                        File channelsF = new File("servers/" + guild.getName() + "-" + guild.getID() + "/channels.txt");
                        if (cmd.equalsIgnoreCase("atc") || cmd.equalsIgnoreCase("addtalkchannel")) {
                            ArrayList<IChannel> channelsToAdd = new ArrayList<IChannel>();
                            for (int i = 1; i < split.length; i++) {
                                IChannel channel = null;
                                if (split[i].contains("#")) {
                                    channel = guild.getChannelByID(split[i].replace("<#", "").replace(">", ""));
                                    if (channel != null) {
                                        if (isFileEmpty(guild) || !canTalkInChannel(guild, channel.getName())) {
                                            channelsToAdd.add(channel);
                                        }
                                    }
                                }
                            }
                            String output = "I can now talk in ";
                            for (int i = 0; i < channelsToAdd.size(); i++) {
                                try {
                                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(channelsF, true)));
                                    pw.println(channelsToAdd.get(i).getName());
                                    output += channelsToAdd.get(i).mention() + " ";
                                    pw.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (channelsToAdd.size() >= 1) {
                                BufferedMessage.sendMessage(ChannelsModule.client, event, output);
                            } else {
                                BufferedMessage.sendMessage(ChannelsModule.client, event, "No new talk channels added.");
                            }
                        } else if (cmd.equalsIgnoreCase("dtc") || cmd.equalsIgnoreCase("deletetalkchannel")) {
                            ArrayList<IChannel> currentTalkChannels = new ArrayList<IChannel>();
                            ArrayList<IChannel> deleteTalkChannels = new ArrayList<IChannel>();
                            ArrayList<IChannel> newTalkChannels = new ArrayList<IChannel>();
                            try {
                                Scanner s = new Scanner(channelsF);
                                while (s.hasNextLine()) {
                                    IChannel channel = guild.getChannelsByName(s.nextLine()).get(0);
                                    if (channel != null) {
                                        currentTalkChannels.add(channel);
                                    }
                                }

                                for (int i = 1; i < split.length; i++) {
                                    if (split[i].contains("#")) {
                                        IChannel channel = guild.getChannelByID(split[i].replace("<#", "").replace(">", ""));
                                        if (channel != null && canTalkInChannel(guild, channel.getName())) {
                                            deleteTalkChannels.add(channel);
                                        }
                                    }
                                }

                                for (int i = 0; i < currentTalkChannels.size(); i++) {
                                    for (int j = 0; j < deleteTalkChannels.size(); j++) {
                                        if (!currentTalkChannels.get(i).getName().equalsIgnoreCase(deleteTalkChannels.get(j).getName())) {
                                            newTalkChannels.add(currentTalkChannels.get(i));
                                        }
                                    }
                                }
                                //Here
                                System.out.println(currentTalkChannels);
                                System.out.println(deleteTalkChannels);
                                System.out.println(newTalkChannels);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else if (cmd.equalsIgnoreCase("rtc") || cmd.equalsIgnoreCase("resettalkchannel")) {
                            try {
                                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(channelsF, false)));
                                pw.close();
                                BufferedMessage.sendMessage(ChannelsModule.client, event, "Talk channels have been reset.");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (cmd.equalsIgnoreCase("tc")) {
                            try {
                                String output = "Here is a list of channels I can talk in: ";
                                Scanner s = new Scanner(channelsF);
                                if (!s.hasNextLine()) {
                                    output = "There are no designated talk channels. I can talk freely.";
                                } else {
                                    while (s.hasNextLine()) {
                                        output += guild.getChannelsByName(s.nextLine()).get(0).mention() + " ";
                                    }
                                }
                                BufferedMessage.sendMessage(ChannelsModule.client, event, output);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }
        }
    }

    public boolean userHasPerm(IUser user, IGuild guild, Permissions perm) {
        List<IRole> roles = user.getRolesForGuild(guild);
        for (IRole r : roles) {
            if (r.getPermissions().contains(perm)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canTalkInChannel(IGuild guild, String channel) {
        if (ModuleListener.isModuleOn(guild, ChannelsModule.name)) {
            File channelsF = new File("servers/" + guild.getName() + "-" + guild.getID() + "/channels.txt");
            try {
                Scanner s = new Scanner(channelsF);
                if (!s.hasNextLine()) {
                    return true;
                } else {
                    while (s.hasNextLine()) {
                        if (s.nextLine().equalsIgnoreCase(channel)) {
                            return true;
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    public boolean isFileEmpty(IGuild guild) {
        File channelsF = new File("servers/" + guild.getName() + "-" + guild.getID() + "/channels.txt");
        try {
            Scanner s = new Scanner(channelsF);
            if (s.hasNextLine()) {
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }
}
