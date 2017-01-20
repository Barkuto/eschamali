package modules.PAD;

import modules.BufferedMessage.BufferedMessage;
import modules.PAD.PADHerderAPI.*;
import modules.Permissions.PermissionsListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

/**
 * Created by Iggie on 8/25/2016.
 */
public class PADListener {
    public static String prefix = "&";
    private TreeMap<String, String> abbrMon = new TreeMap<String, String>();
    private TreeMap<String, String> abbrDun = new TreeMap<String, String>();
    private int maxMonNum = 3252;
    private String guerillaOutput = "modules/PAD/";

    public PADListener() {
        super();
        abbrMon.put("mzeus", "awoken machine zeus");
        abbrMon.put("mhera", "awoken machine hera");
        abbrMon.put("miru", "star myr");
        abbrMon.put("myr", "star myr");
        abbrMon.put("z8", "maleficent phantom zaerog");
        abbrMon.put("radra", "sun god ra dragon");
        abbrMon.put("ra dragon", "sun god ra dragon");
        abbrMon.put("ragdrag", "anti god machine ragnarok");
        abbrMon.put("rag drag", "anti god machine ragnarok");
        abbrMon.put("rag dragon", "anti god machine ragnarok");

        abbrDun.put("sudr", "super ultimate dragon rush");
        abbrDun.put("mhera", "machine hera descended");
        abbrDun.put("mzeus", "machine zeus descended");
        abbrDun.put("z8", "zaerog descended");
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, PADModule.name)
                    && PermissionsListener.canModuleInChannel(guild, PADModule.name, channel)) {
                if (event.getMessage().getContent().startsWith(prefix)) {
                    String msg = event.getMessage().getContent();
                    String[] split = msg.split(" ");
                    String cmd = split[0].replace(prefix, "");
                    IUser user = event.getMessage().getAuthor();

                    if (cmd.equalsIgnoreCase("monster") || cmd.equalsIgnoreCase("mon") || cmd.equalsIgnoreCase("m")) {
                        BufferedMessage.sendMessage(PADModule.client, event, searchMonster(msg.substring(msg.indexOf(cmd) + cmd.length() + 1)));
                    } else if (cmd.equalsIgnoreCase("info") || cmd.equalsIgnoreCase("i")) {
                        if (split.length == 1) {
                            BufferedMessage.sendMessage(PADModule.client, event, getInfo(PADHerderAPI.getMonster(new Random().nextInt(maxMonNum) + 1 + "")));
                        } else {
                            String query = msg.substring(msg.indexOf(cmd) + cmd.length() + 1);
                            Monster m = PADHerderAPI.getMonster(query);
                            if (m != null) {
//                                BufferedMessage.sendMessage(PADModule.client, event, getInfo(m));
                                BufferedMessage.sendEmbed(PADModule.client, event, getInfoEmbed(m, query));
                            } else {
                                BufferedMessage.sendMessage(PADModule.client, event, "Monster not found.");
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("dungeon") || cmd.equalsIgnoreCase("dun") || cmd.equalsIgnoreCase("d")) {
                        BufferedMessage.sendMessage(PADModule.client, event, searchDungeon(msg.substring(msg.indexOf(cmd) + cmd.length() + 1)));
                    } else if (cmd.equalsIgnoreCase("as")) {
                        //List monsters with keyword in active?
                        //List monsters with that active "type"?
                    } else if (cmd.equalsIgnoreCase("reloadabbr") || cmd.equalsIgnoreCase("ra")) {

                    } else if (cmd.equalsIgnoreCase("addabbr") || cmd.equalsIgnoreCase("aa")) {

                    } else if (cmd.equalsIgnoreCase("guerilla") || cmd.equalsIgnoreCase("g")) {
                        LocalDate ld = LocalDate.now();
                        Guerilla g = Guerilla.getTodayGuerilla(guerillaOutput);
                        if (split.length == 1) {
                            try {
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                ImageIO.write(g.getTodayGuerillaImage("pst"), "png", os);
                                InputStream is = new ByteArrayInputStream(os.toByteArray());
                                channel.sendFile("", false, is, "img.png");
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (DiscordException e) {
                                e.printStackTrace();
                            } catch (RateLimitException e) {
                                e.printStackTrace();
                            } catch (MissingPermissionsException e) {
                                e.printStackTrace();
                            }
                        } else if (split.length == 2) {
                            try {
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                ImageIO.write(g.getTodayGuerillaImage(split[1].trim()), "png", os);
                                InputStream is = new ByteArrayInputStream(os.toByteArray());
                                channel.sendFile("", false, is, "img.png");
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (DiscordException e) {
                                e.printStackTrace();
                            } catch (RateLimitException e) {
                                e.printStackTrace();
                            } catch (MissingPermissionsException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("ga") || cmd.equalsIgnoreCase("guerillaall")) {
                        try {
                            event.getMessage().delete();
                        } catch (MissingPermissionsException e) {
                        } catch (RateLimitException e) {
                        } catch (DiscordException e) {
                        }
                        Guerilla g = Guerilla.getTodayGuerilla(guerillaOutput);
                        BufferedImage estImg = g.getTodayGuerillaImage("est");
                        BufferedImage pstImg = g.getTodayGuerillaImage("pst");
                        BufferedImage cstImg = g.getTodayGuerillaImage("cst");
                        BufferedImage mstImg = g.getTodayGuerillaImage("mst");
                        ArrayList<BufferedImage> images = new ArrayList<>();
                        images.add(estImg);
                        images.add(pstImg);
                        images.add(cstImg);
                        images.add(mstImg);

                        for (BufferedImage bi : images) {
                            try {
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                ImageIO.write(bi, "png", os);
                                InputStream is = new ByteArrayInputStream(os.toByteArray());
                                channel.sendFile("", false, is, "img.png");
                            } catch (DiscordException e) {
                                e.printStackTrace();
                            } catch (RateLimitException e) {
                                e.printStackTrace();
                            } catch (MissingPermissionsException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (cmd.equalsIgnoreCase("updateguerilla") || cmd.equalsIgnoreCase("ug") || cmd.equalsIgnoreCase("gu")) {
                        if (Guerilla.updateGuerilla(guerillaOutput)) {
                            BufferedMessage.sendMessage(PADModule.client, event, "Guerillas have been updated for today.");
                        }
                    } else if (cmd.equalsIgnoreCase("pic")) {
                        if (split[1].contains("sheen")) {
                            int roll = new Random().nextInt(100);
                            System.out.println(roll);
                            if (roll >= 95) {
                                BufferedMessage.sendMessage(PADModule.client, event, "http://i.imgur.com/oicGMFu.png");
                                return;
                            }
                        }
                        String found = "";
                        if (split.length == 1) {
                            found = searchMonster((new Random().nextInt(maxMonNum) + 1) + "");
                        } else {
                            found = searchMonster(msg.substring(msg.indexOf(cmd) + cmd.length() + 1));
                        }
                        if (found.contains("n=")) {
                            found = found.substring(found.indexOf("=") + 1);
                            BufferedMessage.sendMessage(PADModule.client, event, "http://puzzledragonx.com/en/img/monster/MONS_" + found + ".jpg");
                        } else {
                            BufferedMessage.sendMessage(PADModule.client, event, found);
                        }
                    } else if (cmd.equalsIgnoreCase("updatejson")) {
                        PADHerderAPI.updateJSON();
                        BufferedMessage.sendMessage(PADModule.client, event, "JSON updated.");
                    }
                }
            }
        }
    }

    public void loadAbbr() {

    }

    public String searchMonster(String keyword) {
        Monster m = PADHerderAPI.getMonster(keyword);
        if (m != null) {
            return "http://puzzledragonx.com/en/monster.asp?n=" + m.getId();
        } else {
            return "Nothing was found.";
        }
    }

    public String searchDungeon(String keyword) {
        if (abbrDun.containsKey(keyword)) {
            keyword = abbrDun.get(keyword);
        }
        try {
            keyword = keyword.replace(" ", "+").trim();
            URL url = new URL("http://puzzledragonx.com/en/search.asp?q=" + keyword);
            Document doc = Jsoup.parse(url, 15000);
            if (url.toString().equals(doc.location())) {
                Elements search = doc.select("div#searchresult2").select("tbody").select("tr");
                String linkID = search.get(0).getElementsByClass("sname").select("a[href]").attr("href");
                Document dunDoc = Jsoup.parse(new URL("http://puzzledragonx.com/en/" + linkID), 15000);
                String dunID = dunDoc.select("td.value-end").select("a[href]").attr("href");
                return "http://puzzledragonx.com/en/" + dunID;
            } else {
                if (doc.location().contains("mission")) {
                    return doc.location();
                } else {
                    return searchDungeon(keyword.replace("+", " ") + " descended");
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            return "Keyword did not find a dungeon, try different or more keywords.";
        }
        return "Nothing could be found.";
    }

    public String getInfo(Monster m) {
        String output = "```\n";
        output += "http://puzzledragonx.com/en/monster.asp?n=" + m.getId() + "\n";
        output += "NAME: " + m.getName() + "\n";
        output += "JP NAME: " + m.getName_jp() + "\n";

        Type type = m.getType().equals("null") ? null : Type.getType(Integer.parseInt(m.getType()));
        Type type2 = m.getType2().equals("null") ? null : Type.getType(Integer.parseInt(m.getType2()));
        Type type3 = m.getType3().equals("null") ? null : Type.getType(Integer.parseInt(m.getType3()));
        output += "TYPING: " + type.getName() + (type2 == null ? "" : "/" + type2.getName()) + (type3 == null ? "" : "/" + type3.getName()) + "\n";

        Attribute element = m.getElement().equals("null") ? null : Attribute.values()[Integer.parseInt(m.getElement())];
        Attribute element2 = m.getElement2().equals("null") ? null : Attribute.values()[Integer.parseInt(m.getElement2())];
        output += "ATTR: " + element.getName() + (element2 == null ? "" : "/" + element2.getName()) + "\n";

        output += String.format("RARITY: %-8s COST: %-3s MP: %-6s", m.getRarity() + " stars", m.getTeam_cost(), m.getMonster_points()) + "\n";
        ActiveSkill active = m.getActive_skill();
        LeaderSkill leader = m.getLeader_skill();
        output += "ACTIVE: " + (active == null ? "None.\n" : "(" + active.getMaxCD() + "->" + active.getMinCD() + "), " + active.getName() + ": " + active.getEffect() + "\n");
        output += "LEADER: " + (leader == null ? "None.\n" : leader.getName() + ": " + leader.getEffect() + "\n");
        output += "AWAKENINGS: ";
        AwokenSkill[] awakenings = m.getAwoken_skills();
        for (int i = 0; i < awakenings.length; i++) {
            output += "[" + Awakening.getAwakening(PADHerderAPI.getAwokenSkill(awakenings[i].getId()).getName()).getShortName() + "]";
        }
        output += "\n```";
        return output;
    }

    public EmbedObject getInfoEmbed(Monster m, String query) {
        EmbedBuilder eb = new EmbedBuilder().ignoreNullEmptyFields();

        Color c = Color.GRAY;
        switch (m.getElement()) {
            case "0":
                c = new Color(0xff744b);
                break;
            case "1":
                c = new Color(0x40ffff);
                break;
            case "2":
                c = new Color(0x4cd962);
                break;
            case "3":
                c = new Color(0xf2e74c);
                break;
            case "4":
                c = new Color(0xcc54c2);
                break;
        }
        String desc = "";
        AwokenSkill[] awakenings = m.getAwoken_skills();
        for (int i = 0; i < awakenings.length; i++) {
            desc += Awakening.getAwakening(PADHerderAPI.getAwokenSkill(awakenings[i].getId()).getName()).getShortName();
            if (i != awakenings.length - 1) {
                desc += "║";
            }
        }
        if (desc.length() == 0)
            desc += "No Awakenings.";
        eb.withDesc("**" + desc + "**");

        Type type = m.getType().equals("null") ? null : Type.getType(Integer.parseInt(m.getType()));
        Type type2 = m.getType2().equals("null") ? null : Type.getType(Integer.parseInt(m.getType2()));
        Type type3 = m.getType3().equals("null") ? null : Type.getType(Integer.parseInt(m.getType3()));
        String typing = type.getName() + (type2 == null ? "" : "/" + type2.getName()) + (type3 == null ? "" : "/" + type3.getName()) + "\n";
        String info = String.format("**Rarity** %-5d" + "\n**Cost**   %-5d" + "\n**MP**     %-5d", m.getRarity(), m.getTeam_cost(), m.getMonster_points());
        eb.appendField(typing, info, true);

        int hp = m.getHp_max();
        int atk = m.getAtk_max();
        int rcv = m.getRcv_max();
        double wghtd = (hp / 10) + (atk / 5) + (rcv / 3);
        eb.appendField("**Weighted** " + wghtd, String.format("**HP**    %-4d\n**ATK** %-4d\n**RCV** %-4d", hp, atk, rcv), true);

        ActiveSkill active = m.getActive_skill();
        String activeName = "Active: " + (active == null ? "None." : active.getName() + " (" + active.getMaxCD() + "->" + active.getMinCD() + ")");
        eb.appendField(activeName, active == null ? "" : active.getEffect(), false);

        LeaderSkill leader = m.getLeader_skill();
        String leaderName = "Leader: " + (leader == null ? "None." : leader.getName());
        eb.appendField(leaderName, leader == null ? "" : leader.getEffect(), false);

        ArrayList<Integer> evos = PADHerderAPI.getEvos(m.getId());
        String otherEvos = "";
        for (int i = 0; i < evos.size(); i++) {
            if (i != evos.size() - 1)
                otherEvos += evos.get(i) + ", ";
            else
                otherEvos += evos.get(i);
        }

        ArrayList<Monster> similarNames = PADHerderAPI.getAllMonsters(query);
        String similar = "";
        if (similarNames.size() <= 10) {
            for (int i = 0; i < similarNames.size(); i++) {
                int currentID = similarNames.get(i).getId();
                if (currentID != m.getId() && !evos.contains(currentID)) {
                    if (i != similarNames.size() - 1)
                        similar += similarNames.get(i).getId() + ", ";
                    else
                        similar += similarNames.get(i).getId();
                }
            }
        } else if (similarNames.size() > 10) {
            similar += "Too many to show.";
        }

        eb.appendField("Other Evos", otherEvos, true);
        eb.appendField("Similar Names", similar, true);

        eb.withThumbnail("http://puzzledragonx.com/en/img/book/" + m.getId() + ".png");
        eb.withTitle("No." + m.getId() + " " + m.getName());
        eb.withUrl("http://puzzledragonx.com/en/monster.asp?n=" + m.getId());
        eb.withColor(c);
        return eb.build();
    }
}
