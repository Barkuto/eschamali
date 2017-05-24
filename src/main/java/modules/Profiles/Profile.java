package modules.Profiles;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Iggie on 5/23/2017.
 */
public class Profile implements Serializable {
    private String name;
    private String nickname;
    private String bio;
    private String pictureURL;
    private Color color;
    private String footerIcon;
    private String footerText;

    private HashMap<String, String> contentFields;//title, content

    public Profile(String name, String nickname, String bio, String pictureURL, Color color, String footerIcon, String footerText, HashMap<String, String> contentFields) {
        this.name = name;
        this.nickname = nickname;
        this.bio = bio;
        this.pictureURL = pictureURL;
        this.color = color;
        this.footerIcon = footerIcon;
        this.footerText = footerText;
        this.contentFields = contentFields;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setPictureURL(String url) {
        this.pictureURL = url;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setFooterIcon(String url) {
        this.footerIcon = url;
    }

    public void setFooterText(String text) {
        this.footerText = text;
    }

    public void appendField(String title, String content) {
        contentFields.put(title, content);
    }

    public String removeField(String title) {
        return contentFields.remove(title);
    }

    public EmbedObject getAsEmbed() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.ignoreNullEmptyFields();
        eb.withTitle(name + (nickname.length() > 0 ? " AKA " + nickname : ""));
        eb.withDesc(bio);
        eb.withThumbnail(pictureURL);
        eb.withColor(color);
        eb.withFooterIcon(footerIcon);
        eb.withFooterText(footerText);
        for (Map.Entry<String, String> entry : contentFields.entrySet()) {
            eb.appendField(entry.getKey(), entry.getValue(), false);
        }
        return eb.build();
    }

    public byte[] getAsBytes() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            bos.close();

            byte[] objData = bos.toByteArray();
            return objData;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Profile getFromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);

            Profile p = (Profile) ois.readObject();
            return p;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Profile getDefaultProfile(IUser user) {
        return new Profile(user.getName(), "", "", user.getAvatarURL(), Color.GRAY, "", "", new HashMap<>());
    }
}
