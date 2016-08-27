package modules.PAD;

import base.ModuleListener;
import modules.BufferedMessage.BufferedMessage;
import modules.Channels.ChannelsListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.TreeMap;

/**
 * Created by Iggie on 8/25/2016.
 */
public class PADListener {
    public static String prefix = "&";
    private TreeMap<String, String> abbrMon = new TreeMap<String, String>();
    private TreeMap<String, String> abbrDun = new TreeMap<String, String>();

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
            if (ModuleListener.isModuleOn(event.getMessage().getGuild(), PADModule.name) && ChannelsListener.canTalkInChannel(event.getMessage().getGuild(), event.getMessage().getChannel().getName())) {
                if (event.getMessage().getContent().startsWith(prefix)) {
                    String msg = event.getMessage().getContent();
                    String[] split = msg.split(" ");
                    String cmd = split[0].replace(prefix, "");
                    IUser user = event.getMessage().getAuthor();
                    IGuild guild = event.getMessage().getGuild();

                    if (cmd.equalsIgnoreCase("monster") || cmd.equalsIgnoreCase("mon") || cmd.equalsIgnoreCase("m")) {
                        BufferedMessage.sendMessage(PADModule.client, event, searchMonster(msg.substring(msg.indexOf(cmd) + cmd.length() + 1)));
                    } else if (cmd.equalsIgnoreCase("info") || cmd.equalsIgnoreCase("i")) {
                        BufferedMessage.sendMessage(PADModule.client, event, searchMonster(msg.substring(msg.indexOf(cmd) + cmd.length())));
                    } else if (cmd.equalsIgnoreCase("dungeon") || cmd.equalsIgnoreCase("dun") || cmd.equalsIgnoreCase("d")) {
                        BufferedMessage.sendMessage(PADModule.client, event, searchDungeon(msg.substring(msg.indexOf(cmd) + cmd.length() + 1)));
                    } else if (cmd.equalsIgnoreCase("as")) {
                        //List monsters with keyword in active?
                        //List monsters with that active "type"?
                    } else if (cmd.equalsIgnoreCase("reloadabbr") || cmd.equalsIgnoreCase("ra")) {

                    } else if (cmd.equalsIgnoreCase("addabbr") || cmd.equalsIgnoreCase("aa")) {

                    }
                }
            }
        }
    }

    public void loadAbbr() {

    }

    public String searchMonster(String keyword) {
        if (abbrMon.containsKey(keyword)) {
            keyword = abbrMon.get(keyword);
        }
        try {
            keyword = keyword.trim().replace(" ", "+");
            URL url = new URL("http://puzzledragonx.com/en/search.asp?q=" + keyword);
            Document doc = Jsoup.parse(url, 15000);
            if (url.toString().equals(doc.location())) {
                Elements search = doc.select("div#searchresult1").select("tbody").select("tr");
                String linkID = search.get(0).getElementsByClass("sname").select("a[href]").attr("href");
                return "http://puzzledragonx.com/en/" + linkID;
            } else {
                return doc.location();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            return "Keyword did not find a monster, try different or more keywords.";
        }
        return "Nothing could be found.";
    }

    public String searchDungeon(String keyword) {
        if (abbrDun.containsKey(keyword)) {
            keyword = abbrDun.get(keyword);
        }
        System.out.println(keyword);
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

    public String getInfo(String monster) {
        try {
            URL url = new URL(monster);
            Document doc = Jsoup.parse(url, 15000);
            Elements tables = doc.select("table");
            String avatar = "http://puzzledragonx.com/en/" + doc.select("div.avatar").select("img").attr("src");
            //PROFILE
            Elements profile = doc.select("div#compareprofile");
            Elements tableprofile = profile.select("table#tableprofile").get(0).select("tr");
            String name = tableprofile.get(0).child(1).text();
            String jpName = tableprofile.get(1).child(1).text();

            Elements tableprofileprofile = profile.select("table#tableprofile.tableprofile").select("tr");
            String typing = tableprofileprofile.get(0).child(1).text();
            String element = tableprofileprofile.select("tr").get(1).child(1).text();
            String rarity = tableprofileprofile.select("tr").get(6).child(1).text();
            String cost = tableprofileprofile.select("tr").get(7).child(1).text();
            String mp = tableprofileprofile.select("tr").get(8).child(1).text();
            //ABILITIES
            //Account for jp ver ;-;
            int shift = 0;
            Elements abilities = tables.get(17).child(0).select("tr");
            String activeName = abilities.get(1).child(1).text();
            String active = abilities.get(2).child(1).text();
            if (abilities.get(3).text().contains("Effect") || abilities.get(3).toString().contains("orb")) {
                shift++;
            }
            String cooldown = abilities.get(3 + shift).child(1).text();
            if (abilities.get(4 + shift).text().contains("Cool")) {
                shift++;
            }
            String leaderName = abilities.get(6 + shift).child(1).text();
            String leader = abilities.get(7 + shift).child(1).text();
            try {
                if (abilities.get(8 + shift).text().contains("Effect")) {
                    shift++;
                }
            } catch (IndexOutOfBoundsException e) {

            }
            boolean hasAwakenings = true;
            Elements awakenings = null;
            try {
                awakenings = abilities.get(9 + shift).child(1).select("img[src]");
            } catch (IndexOutOfBoundsException e) {
                hasAwakenings = false;
            }
            Elements pantheons = tables.get(20).select("h2");

            String output = "```\n";
            output += doc.location() + "\n";
            output += "NAME: " + name + "\n";
            output += "JP NAME: " + jpName + "\n";
            output += "ATTR: " + element + "\n";
            output += String.format("RARITY: %-8s COST: %-3s MP: %-6s", rarity, cost, mp) + "\n";
            output += String.format("ACTIVE: %s %s: %s", cooldown, activeName, active) + "\n";
            output += String.format("LEADER: %s: %s", leaderName, leader) + "\n";
            output += String.format("AWAKENINGS: ");
            if (hasAwakenings) {
                for (int i = 0; i < awakenings.size(); i++) {
                    String awakeningDesc = awakenings.get(i).attr("title");
                    String awakening = awakeningDesc.substring(0, awakeningDesc.indexOf('\n') - 1);
                    String smallAwakening = "";
                    switch (awakening) {
                        case "Enhanced HP":
                            smallAwakening = "+HP";
                            break;
                        case "Enhanced Attack":
                            smallAwakening = "+ATK";
                            break;
                        case "Enhanced Heal":
                            smallAwakening = "+RCV";
                            break;

                        case "Auto-Recover":
                            smallAwakening = "AutoRCV";
                            break;
                        case "Extend Time":
                            smallAwakening = "TE";
                            break;
                        case "Recover Bind":
                            smallAwakening = "BindRCV";
                            break;
                        case "Skill Boost":
                            smallAwakening = "SB";
                            break;
                        case "Two-Pronged Attack":
                            smallAwakening = "TPA";
                            break;
                        case "Resistance-Skill Bind":
                            smallAwakening = "SBR";
                            break;

                        case "Reduce Fire Damage":
                            smallAwakening = "FireRes";
                            break;
                        case "Reduce Water Damage":
                            smallAwakening = "WaterRes";
                            break;
                        case "Reduce Wood Damage":
                            smallAwakening = "WoodRes";
                            break;
                        case "Reduce Light Damage":
                            smallAwakening = "LightRes";
                            break;
                        case "Reduce Dark Damage":
                            smallAwakening = "DarkRes";
                            break;

                        case "Resistance-Bind":
                            smallAwakening = "BindRes";
                            break;
                        case "Resistance-Dark":
                            smallAwakening = "BlindRes";
                            break;
                        case "Resistance-Jammers":
                            smallAwakening = "JamRes";
                            break;
                        case "Resistance-Poison":
                            smallAwakening = "PoiRes";
                            break;

                        case "Enhanced Fire Orbs":
                            smallAwakening = "FireOE";
                            break;
                        case "Enhanced Water Orbs":
                            smallAwakening = "WaterOE";
                            break;
                        case "Enhanced Wood Orbs":
                            smallAwakening = "WoodOE";
                            break;
                        case "Enhanced Light Orbs":
                            smallAwakening = "LightOE";
                            break;
                        case "Enhanced Dark Orbs":
                            smallAwakening = "DarkOE";
                            break;

                        case "Enhanced Fire Attribute":
                            smallAwakening = "FireRow";
                            break;
                        case "Enhanced Water Attribute":
                            smallAwakening = "WaterRow";
                            break;
                        case "Enhanced Wood Attribute":
                            smallAwakening = "WoodRow";
                            break;
                        case "Enhanced Light Attribute":
                            smallAwakening = "LightRow";
                            break;
                        case "Enhanced Dark Attribute":
                            smallAwakening = "DarkRow";
                            break;
                    }
                    output += "[" + smallAwakening + "]";
                }
            }
            output += "\nPANTHEONS: ";
            for (int i = 0; i < pantheons.size() - 1; i++) {
                output += pantheons.get(i).text() + ", ";
            }
            output = output.trim().substring(0, output.lastIndexOf(',')) + "\n";
            output += "```";
            return output;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
