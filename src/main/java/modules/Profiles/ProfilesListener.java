package modules.Profiles;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.Db;
import modules.Permissions.Permission;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Iggie on 5/23/2017.
 */
public class ProfilesListener {
    public static String prefix = "!";

    private String tableName = "profiles";
    private String col1 = "userID";
    private String col2 = "profile";
    private String[] tableCols = {col1, col2};
    private String output = "modules/Profiles/";
    private String dbFile = "profiles.db";

    private String profileSaveErrorMessage = "Something went wrong when saving your profile.";

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        try {
            new File(output).mkdirs();
            new File(output + dbFile).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Permission perms = new Permission(new Db(output + dbFile));
        if (!perms.tableExists(tableName)) {
            perms.createTable(tableName, tableCols, new String[]{"string", "object"}, false);
        }
        perms.close();
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, ProfilesModule.name)
                    && PermissionsListener.canModuleInChannel(guild, ProfilesModule.name, channel)) {
                if (event.getMessage().getContent().startsWith(prefix)) {
                    String msg = event.getMessage().getContent();
                    String[] split = msg.split(" ");
                    String cmd = split[0].replace(prefix, "");
                    IUser user = event.getMessage().getAuthor();
                    String userID = user.getID();
                    Db db = new Db(output + dbFile);
                    Profile profile = null;

                    if (cmd.startsWith("p"))
                        profile = getProfile(user, db);

                    if (cmd.equalsIgnoreCase("profile")
                            || cmd.equalsIgnoreCase("p")) {
                        if (split.length == 1) {
                            if (profile != null)
                                BufferedMessage.sendEmbed(ProfilesModule.client, event, profile.getAsEmbed());
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Something went wrong trying to get your profile!");
                        } else {
                            IUser otherUser = null;
                            String arg = msg.substring(msg.indexOf(" ") + 1).trim();
                            if (arg.startsWith("<@")) {
                                String id = "";
                                int startIndex = 2;
                                if (arg.startsWith("<@!")) {
                                    startIndex++;
                                }
                                id += arg.substring(startIndex, arg.length() - 1);
                                otherUser = guild.getUserByID(id);
                            } else {
                                arg = msg.substring(msg.indexOf(" ") + 1).trim();
                                for (IUser aUser : guild.getUsers()) {
                                    if (aUser.getName().toLowerCase().contains(arg.toLowerCase())) {
                                        otherUser = aUser;
                                        break;
                                    }
                                }
                            }
                            java.util.List<IUser> users = guild.getUsersByName(arg);
                            if (users.size() > 0)
                                otherUser = users.get(0);
                            if (otherUser != null) {
                                Profile otherProfile = getProfile(otherUser, db);
                                if (otherProfile != null)
                                    BufferedMessage.sendEmbed(ProfilesModule.client, event, otherProfile.getAsEmbed());
                                else
                                    BufferedMessage.sendMessage(ProfilesModule.client, event, "Something went wrong trying to get that profile!");
                            } else {
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Could not find that user.");
                            }
                        }
                    } else if (profile != null) {
                        /* Profile Set Commands */
                        if (cmd.equalsIgnoreCase("profilesetname")
                                || cmd.equalsIgnoreCase("psn")) {
                            String newName = msg.substring(cmd.length() + 1).trim();
                            profile.setName(newName);
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile name has been set to \"" + newName + "\".");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Something went wrong when saving your profile.");
                        } else if (cmd.equalsIgnoreCase("profilesetnickname") || cmd.equalsIgnoreCase("profilesetnick")
                                || cmd.equalsIgnoreCase("psnn")) {
                            String newNick = msg.substring(cmd.length() + 1).trim();
                            profile.setNickname(newNick);
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile nickname has been set to \"" + newNick + "\".");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profilesetbio")
                                || cmd.equalsIgnoreCase("psb")) {
                            String newBio = msg.substring(cmd.length() + 1).trim();
                            profile.setBio(newBio);
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile bio has been set.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profilesetpicture")
                                || cmd.equalsIgnoreCase("psp")) {
                            String newPic = msg.substring(cmd.length() + 1).trim();
                            profile.setPictureURL(newPic);
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile picture has been set.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profilesetcolor")
                                || cmd.equalsIgnoreCase("psc")) {
                            Color newColor = null;
                            if (split.length == 2) {
                                String s = msg.substring(cmd.length() + 1).trim();
                                try {
                                    newColor = (Color) Color.class.getField(s.toUpperCase()).get(null);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (NoSuchFieldException e) {
                                    BufferedMessage.sendMessage(ProfilesModule.client, event, "Invalid color given. Provide a valid color name, or enter RGB values(0-255) separated by spaces.");
                                }
                            } else if (split.length == 4) {
                                try {
                                    int r = Integer.parseInt(split[1]);
                                    int g = Integer.parseInt(split[2]);
                                    int b = Integer.parseInt(split[3]);
                                    newColor = new Color(r, g, b);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (newColor != null) {
                                profile.setColor(newColor);
                                if (saveProfile(user, profile, db))
                                    BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile color has been set.");
                                else
                                    BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                            }
                        } else if (cmd.equalsIgnoreCase("profilesetfootericon")
                                || cmd.equalsIgnoreCase("psfi")) {
                            String newFooterIcon = msg.substring(cmd.length() + 1).trim();
                            profile.setFooterIcon(newFooterIcon);
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile footer icon has been set.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profilesetfooter")
                                || cmd.equalsIgnoreCase("psf")) {
                            String newFooter = msg.substring(cmd.length() + 1).trim();
                            profile.setFooterText(newFooter);
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile footer has been set.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profileaddfield")
                                || cmd.equalsIgnoreCase("paf")) {
                            if (split.length > 1) {
                                String title = msg.substring(cmd.length() + 1, msg.indexOf(';')).trim();
                                String content = msg.substring(msg.indexOf(';') + 1).trim();
                                profile.appendField(title, content);
                                if (saveProfile(user, profile, db))
                                    BufferedMessage.sendMessage(ProfilesModule.client, event, "The field \"" + title + "\" has been added.");
                                else
                                    BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                            } else {
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Invalid command format. Use: &" + cmd + " title;content");
                            }
                        } else if (cmd.equalsIgnoreCase("profileremovefield")
                                || cmd.equalsIgnoreCase("prf") || cmd.equalsIgnoreCase("pdf")) {
                            String title = msg.substring(cmd.length() + 1).trim();
                            if (profile.removeField(title) != null) {
                                if (saveProfile(user, profile, db))
                                    BufferedMessage.sendMessage(ProfilesModule.client, event, "The field \"" + title + "\" has been removed.");
                                else
                                    BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                            } else {
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Invalid field.");
                            }
                        }
                        /* Profile Reset Commands */
                        else if (cmd.equalsIgnoreCase("profilereset")) {
                            profile = Profile.getDefaultProfile(user);
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile has been reset.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profileresetname")) {
                            profile.setName(user.getName());
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile name has been reset.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profileresetnickname") || cmd.equalsIgnoreCase("profileresetnick")) {
                            profile.setNickname("");
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile nickname has been reset.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profileresetbio")) {
                            profile.setBio("");
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile bio has been reset.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profileresetpicture")) {
                            profile.setPictureURL(user.getAvatarURL());
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile picture has been reset.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profileresetcolor")) {
                            profile.setColor(Color.GRAY);
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile color has been reset.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profileresetfootericon")) {
                            profile.setFooterIcon("");
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile footer icon has been reset.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        } else if (cmd.equalsIgnoreCase("profileresetfooter")) {
                            profile.setFooterText("");
                            if (saveProfile(user, profile, db))
                                BufferedMessage.sendMessage(ProfilesModule.client, event, "Your profile footer has been reset.");
                            else
                                BufferedMessage.sendMessage(ProfilesModule.client, event, profileSaveErrorMessage);
                        }
                    }

                    db.close();
                }
            }
        }
    }

    public Profile getProfile(IUser user, Db db) {
        Profile profile = null;
        PreparedStatement read = db.getPreparedStatement("SELECT " + col2 + " FROM " + tableName + " WHERE " + col1 + " = ?");
        try {
            read.setString(1, user.getID());
            ResultSet rs = read.executeQuery();
            if (rs.next())
                profile = Profile.getFromBytes(rs.getBytes(col2));
            else
                profile = Profile.getDefaultProfile(user);
            rs.close();
            read.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return profile;
    }

    public boolean saveProfile(IUser user, Profile profile, Db db) {
        byte[] bytes = profile.getAsBytes();
        String id = user.getID();

        PreparedStatement ps = null;
        ResultSet rs = db.executeQuery("SELECT " + "*" + " FROM " + tableName + " WHERE " + col1 + "='" + id + "'");
        try {
            if (rs.next()) {
                ps = db.getPreparedStatement("UPDATE " + tableName + " SET " + col2 + "=? WHERE " + col1 + "=?");
                ps.setObject(1, bytes);
                ps.setString(2, id);
            } else {
                ps = db.getPreparedStatement("INSERT INTO " + tableName + " (" + col1 + "," + col2 + ") VALUES (?,?)");
                ps.setString(1, id);
                ps.setObject(2, bytes);
            }
            rs.close();
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
