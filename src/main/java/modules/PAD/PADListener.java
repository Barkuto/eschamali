package modules.PAD;

import modules.BufferedMessage.BufferedMessage;
import modules.Permissions.PermissionsListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
                            BufferedMessage.sendMessage(PADModule.client, event, getInfo((new Random().nextInt(3164) + 1) + ""));
                        } else {
                            BufferedMessage.sendMessage(PADModule.client, event, getInfo(searchMonster(msg.substring(msg.indexOf(cmd) + cmd.length() + 1))));
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
                        Guerilla g = getTodayGuerilla();
                        if (split.length == 1) {
                            BufferedMessage.sendMessage(PADModule.client, event, g.allGroups("pst"));
                        } else if (split.length == 2) {
                            BufferedMessage.sendMessage(PADModule.client, event, g.allGroups(split[1].trim()));
                        } else if (split.length == 3) {
                            BufferedMessage.sendMessage(PADModule.client, event, g.forGroup(split[2].trim(), split[1].trim()));
                        }
                    } else if (cmd.equalsIgnoreCase("ga") || cmd.equalsIgnoreCase("guerillaall")) {
                        try {
                            event.getMessage().delete();
                        } catch (MissingPermissionsException e) {
                        } catch (RateLimitException e) {
                        } catch (DiscordException e) {
                        }
                        Guerilla g = getTodayGuerilla();
                        String est = g.allGroups("est");
                        String pst = g.allGroups("pst");
                        String cst = g.allGroups("cst");
                        String mst = g.allGroups("mst");
                        LocalDate today = LocalDate.now();
                        String date = today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                        BufferedMessage.sendMessage(PADModule.client, event, "__**" + date + "**__");
                        BufferedMessage.sendMessage(PADModule.client, event, est);
                        BufferedMessage.sendMessage(PADModule.client, event, pst);
                        BufferedMessage.sendMessage(PADModule.client, event, cst);
                        BufferedMessage.sendMessage(PADModule.client, event, mst);
                    } else if (cmd.equalsIgnoreCase("updateguerilla") || cmd.equalsIgnoreCase("ug") || cmd.equalsIgnoreCase("gu")) {
                        if (updateGuerilla()) {
                            BufferedMessage.sendMessage(PADModule.client, event, "Guerillas have been updated for today.");
                        }
                    } else if (cmd.equalsIgnoreCase("guerillatomorrow") || cmd.equalsIgnoreCase("gt")) {
                        Guerilla g = getTomorrowGuerilla();
                        if (g != null) {
                            BufferedMessage.sendMessage(PADModule.client, event, g.allGroups("pst"));
                        } else {
                            BufferedMessage.sendMessage(PADModule.client, event, "Hours are not out yet.");
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
                            found = searchMonster((new Random().nextInt(3164) + 1) + "");
                        } else {
                            found = searchMonster(msg.substring(msg.indexOf(cmd) + cmd.length() + 1));
                        }
                        if (found.contains("n=")) {
                            found = found.substring(found.indexOf("=") + 1);
                            BufferedMessage.sendMessage(PADModule.client, event, "http://puzzledragonx.com/en/img/monster/MONS_" + found + ".jpg");
                        } else {
                            BufferedMessage.sendMessage(PADModule.client, event, found);
                        }
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

    public String getInfo(String page) {
        try {
            URL url = new URL(page);
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
            Elements abilities = tables.get(17).child(0).select("tr");
            String activeName = "";
            String active = "";
            String cooldown = "";
            String leaderName = "";
            String leader = "";
            Elements pantheons = tables.get(20).select("h2");

            int spaces = 0;
            ArrayList<Integer> spaceIndexes = new ArrayList<Integer>();
            for (int i = 1; i < abilities.size(); i++) {
                if (abilities.get(i).toString().contains("colspan")) {
                    spaces++;
                    spaceIndexes.add(i);
                }
            }
            spaceIndexes.add(abilities.size() - 1);

            for (int i = 1; i < spaceIndexes.get(0); i++) {
                Element current = abilities.get(i);
                if (current.child(0).text().equals("Active Skill:")) {
                    activeName = current.child(1).text();
                } else if (current.child(0).text().equals("Effects:")) {
                    active = current.child(1).text();
                } else if (current.child(0).text().equals("Cool Down:")) {
                    cooldown = current.child(1).text();
                }
            }

            for (int i = spaceIndexes.get(0); i <= spaceIndexes.get(1); i++) {
                Element current = abilities.get(i);
                if (current.child(0).text().equals("Leader Skill:")) {
                    leaderName = current.child(1).text();
                } else if (current.child(0).text().equals("Effects:") && leader.equals("")) {
                    leader = current.child(1).text();
                }
            }

            String output = "```\n";
            output += doc.location() + "\n";
            output += "NAME: " + name + "\n";
            output += "JP NAME: " + jpName + "\n";
            output += "TYPING: " + typing.replace(" ", "") + "\n";
            output += "ATTR: " + element + "\n";
            output += String.format("RARITY: %-8s COST: %-3s MP: %-6s", rarity, cost, mp) + "\n";
            output += "ACTIVE: " + (cooldown.equals("") ? "" : cooldown + ", ") + (activeName.equals("None") ? "None." : activeName + ": ") + active + "\n";
            output += "LEADER: " + (leaderName.equals("None") ? "None." : leaderName + ": ") + leader + "\n";
            output += "AWAKENINGS: ";
            if (spaces == 2) {
                Elements awakenings = abilities.get(spaceIndexes.get(2)).select("img[src]");
                for (int i = 0; i < awakenings.size(); i++) {
                    String awakeningDesc = awakenings.get(i).attr("title");
                    String awakening = awakeningDesc.substring(0, awakeningDesc.indexOf('\n') - 1);
                    String smallAwakening = Awakening.getAwakening(awakening).getShortName();
                    output += "[" + smallAwakening + "]";
                }
            } else {
                output += "None.";
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
        return "Nothing found.";
    }

    public boolean updateGuerilla() {
        try {
            URL home = new URL("http://puzzledragonx.com/");
            Document document = Jsoup.parse(home, 15000);
            Elements sched = document.select("div#metal1a").select("table").get(0).select("tr");
            ArrayList<LocalTime> a = new ArrayList<LocalTime>();
            ArrayList<LocalTime> b = new ArrayList<LocalTime>();
            ArrayList<LocalTime> c = new ArrayList<LocalTime>();
            ArrayList<LocalTime> d = new ArrayList<LocalTime>();
            ArrayList<LocalTime> e = new ArrayList<LocalTime>();
            for (int i = 2; i < sched.size(); i += 2) {
                Elements times = sched.get(i).select("td");
                int group = 0;
                for (int j = 0; j < times.size(); j++) {
                    if (times.get(j).text().length() > 1) {
                        switch (group) {
                            case 0:
                                a.add(parseTime(times.get(j).text()));
                                break;
                            case 1:
                                b.add(parseTime(times.get(j).text()));
                                break;
                            case 2:
                                c.add(parseTime(times.get(j).text()));
                                break;
                            case 3:
                                d.add(parseTime(times.get(j).text()));
                                break;
                            case 4:
                                e.add(parseTime(times.get(j).text()));
                                break;
                        }
                        group++;
                    }
                }
            }
            ArrayList<String> dungeons = new ArrayList<String>();
            for (int i = 1; i < sched.size(); i += 2) {
                URL url = new URL("http://puzzledragonx.com/" + sched.get(i).select("td").select("a[href]").attr("href"));
                Document doc = Jsoup.parse(url, 150000);
                String dungeon = doc.select("table#tablestat").get(1).select("tr").get(1).text();
                dungeons.add(dungeon);
            }
            Guerilla g = new Guerilla(dungeons, a, b, c, d, e);
            g.writeOut("modules/PAD/");
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Guerilla getTomorrowGuerilla() {
        try {
            URL home = new URL("http://puzzledragonx.com/");
            Document document = Jsoup.parse(home, 15000);
            Elements sched = document.select("div#metal1b").select("table").get(0).select("tr");
            ArrayList<LocalTime> a = new ArrayList<LocalTime>();
            ArrayList<LocalTime> b = new ArrayList<LocalTime>();
            ArrayList<LocalTime> c = new ArrayList<LocalTime>();
            ArrayList<LocalTime> d = new ArrayList<LocalTime>();
            ArrayList<LocalTime> e = new ArrayList<LocalTime>();
            for (int i = 2; i < sched.size(); i += 2) {
                Elements times = sched.get(i).select("td");
                int group = 0;
                for (int j = 0; j < times.size(); j++) {
                    if (times.get(j).text().length() > 1) {
                        switch (group) {
                            case 0:
                                a.add(parseTime(times.get(j).text()));
                                break;
                            case 1:
                                b.add(parseTime(times.get(j).text()));
                                break;
                            case 2:
                                c.add(parseTime(times.get(j).text()));
                                break;
                            case 3:
                                d.add(parseTime(times.get(j).text()));
                                break;
                            case 4:
                                e.add(parseTime(times.get(j).text()));
                                break;
                        }
                        group++;
                    }
                }
            }
            ArrayList<String> dungeons = new ArrayList<String>();
            for (int i = 1; i < sched.size(); i += 2) {
                URL url = new URL("http://puzzledragonx.com/" + sched.get(i).select("td").select("a[href]").attr("href"));
                Document doc = Jsoup.parse(url, 150000);
                String dungeon = doc.select("table#tablestat").get(1).select("tr").get(1).text();
                dungeons.add(dungeon);
            }
            Guerilla g = new Guerilla(dungeons, a, b, c, d, e);
            return g;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Guerilla getGuerilla(int year, int month, int day) {
        Guerilla g = null;
        try {
            g = Guerilla.readIn("modules/PAD/guerilla-" + year + "-" + month + "-" + day + ".ser");
        } catch (ClassNotFoundException e) {
        } catch (IOException e) {
        }
        return g;
    }

    public Guerilla getTodayGuerilla() {
        Guerilla g = null;
        LocalDate ld = LocalDate.now();
        g = getGuerilla(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
        if (g == null) {
            updateGuerilla();
        } else {
            return g;
        }
        return getGuerilla(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
    }

    public LocalTime parseTime(String time) {
        String timeformat = time.toUpperCase();
        if (!timeformat.contains(":")) {
            timeformat = timeformat.replace(" ", ":00 ");
        }
        String DATE_FORMAT = "h:mm a";
        return LocalTime.parse(timeformat, DateTimeFormatter.ofPattern(DATE_FORMAT));
    }
}
