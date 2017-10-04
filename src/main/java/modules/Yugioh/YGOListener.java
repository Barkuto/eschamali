package modules.Yugioh;

import com.google.gson.*;
import modules.BufferedMessage.Sender;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Iggie on 4/8/2017.
 */
public class YGOListener {
    public static String prefix = "*";
    private String databaseOutput = "modules/YGO/";
    private String databaseFile = "cards.json";
    private int maxPages = 0;

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, YGOModule.name)
                    && PermissionsListener.canModuleInChannel(guild, YGOModule.name, channel)) {
                if (event.getMessage().getContent().startsWith(prefix)) {
                    String msg = event.getMessage().getContent().toLowerCase();
                    String[] split = msg.split(" ");
                    String cmd = split[0].replace(prefix, "");
                    IUser user = event.getMessage().getAuthor();
                    if (cmd.equalsIgnoreCase("update") || cmd.equalsIgnoreCase("u")) {
                        Sender.sendMessage(channel, "YGO Database updated. Card total = " + updateCards());
                    } else if (cmd.equalsIgnoreCase("info") || cmd.equalsIgnoreCase("i")) {
                        try {
                            JsonObject obj = getCard(msg.toLowerCase().substring(cmd.length() + 1).trim());
                            if (obj != null) {
                                EmbedBuilder eb = new EmbedBuilder();
                                String name = obj.get("name").getAsString();
                                String desc = obj.get("description").getAsString();
                                String image = obj.get("image").getAsString();
                                String rarity = obj.get("rarity").getAsString();
                                String source = "";
                                if (obj.get("source") != null) {
                                    source = obj.get("source").getAsString();
                                }
                                String slug = obj.get("slug").getAsString();
                                String imgURL = "https://yugidecks.com/assets/card_images/" + image;

                                eb.withTitle(name + " [" + rarity + "]");
                                eb.withThumbnail(imgURL);
                                eb.withUrl("https://yugidecks.com/cards/" + slug);
                                if (obj.get("atk") != null) {
                                    //Monster
                                    String atk = obj.get("atk").getAsString();
                                    String def = obj.get("def").getAsString();
                                    String level = obj.get("level").getAsString();
                                    String monster_type = obj.get("monster_type").getAsString();
                                    String card_attribute = obj.get("card_attribute").getAsString();
                                    String card_type = obj.get("card_type").getAsString();
                                    eb.withDesc("**Level:** " + level + " **Att:** " + card_attribute + "\n" +
                                            "**[" + monster_type + "/" + card_type + "]**\n\n" +
                                            desc + "\n\n" +
                                            "**ATK** " + atk + " **DEF** " + def +
                                            (source.length() > 0 ? "\n\n" + "**Source:** " + source : ""));
                                } else {
                                    //Spell or Trap
                                    String property = obj.get("property").getAsString();
                                    eb.withDesc("**[" + property + "]**" + "\n\n" +
                                            desc +
                                            (source.length() > 0 ? "\n\n" + "**Source:** " + source : ""));
                                }
                                Sender.sendEmbed(channel, eb.build());
                            } else {
                                Sender.sendMessage(channel, "Card not found.");
                            }
                        } catch (FileNotFoundException e) {
                            updateCards();
                            Sender.sendMessage(channel, "Card database was missing, but has been updated. Please try again.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private JsonObject getCard(String keywords) throws IOException {
        JsonParser jsonParser = new JsonParser();
        FileReader fr = new FileReader(new File(databaseOutput + databaseFile));
        JsonArray cards = jsonParser.parse(fr).getAsJsonArray();
        for (JsonElement card : cards) {
            JsonObject obj = card.getAsJsonObject();
            String name = obj.get("name").getAsString().toLowerCase();
            String[] keywordSplit = keywords.toLowerCase().split(" ");
            int matches = 0;
            for (int i = 0; i < keywordSplit.length; i++) {
                if (name.contains(keywordSplit[i]))
                    matches++;
            }
            if (matches == keywordSplit.length) {
                fr.close();
                return obj;
            }
        }
        fr.close();
        return null;
    }

    private int updateCards() {
        JsonArray cards = new JsonArray();
        int page = 1;
        int totalCards = 0;
        for (; ; page++) {
            try {
                URL url = new URL("https://yugidecks.com/cards.json?page=" + page);
                JsonParser jsonParser = new JsonParser();
                JsonElement jsonElement = jsonParser.parse(new BufferedReader(new InputStreamReader(url.openStream())));
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                if (jsonArray.size() > 0) {
                    totalCards += jsonArray.size();
                    cards.addAll(jsonArray);
                } else {
                    File f = new File(databaseOutput + databaseFile);
                    if (!f.exists()) {
                        f.getParentFile().mkdirs();
                        f.createNewFile();
                    }
                    Writer writer = new FileWriter(f);
                    Gson gson = new GsonBuilder().create();
                    gson.toJson(cards, writer);
                    writer.close();
                    break;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        maxPages = page;
        return totalCards;
    }

}
