package modules.PAD.PADHerderAPI;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by Iggie on 10/11/2016.
 */
public class PADHerderAPI {
    private static String path = "modules/PAD/PHAPI/";

    public static void updateJSON() {
        try {
            ArrayList<URL> urls = new ArrayList<>();
            URL actives = new URL("https://www.padherder.com/api/active_skills/");
            URL awakenings = new URL("https://www.padherder.com/api/awakenings/");
            URL events = new URL("https://www.padherder.com/api/events/");
            URL evolutions = new URL("https://www.padherder.com/api/evolutions/");
            URL food = new URL("https://www.padherder.com/api/food/");
            URL leaders = new URL("https://www.padherder.com/api/leader_skills/");
            URL materials = new URL("https://www.padherder.com/api/materials/");
            URL monsters = new URL("https://www.padherder.com/api/monsters/");
            urls.add(actives);
            urls.add(awakenings);
            urls.add(events);
            urls.add(evolutions);
            urls.add(food);
            urls.add(leaders);
            urls.add(materials);
            urls.add(monsters);

            for (URL u : urls) {
                URLConnection conn = u.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
                conn.connect();
                String p = u.getPath();
                String info = p.substring(p.indexOf("api") + 3).replace("/", "");
                FileUtils.copyInputStreamToFile(conn.getInputStream(), new File(path + info + ".json"));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Monster getMonster(int id) {
        JsonParser parser = new JsonParser();
        try {
            JsonArray jsonArray = (JsonArray) parser.parse(new FileReader(path + "monsters.json"));
            for (Object o : jsonArray) {
                JsonObject obj = (JsonObject) o;
                if (obj.get("id").getAsInt() == id) {
                    return new Monster(obj);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Monster getMonster(String keywords) {
        try {
            int id = Integer.parseInt(keywords.trim());
            return getMonster(id);
        } catch (NumberFormatException e) {
            ArrayList<Monster> allSimilar = getAllMonsters(keywords);
            if (allSimilar.size() > 0) {
                int fireCount = 0;
                int waterCount = 0;
                int woodCount = 0;
                int lightCount = 0;
                int darkCount = 0;
                for (int i = 0; i < allSimilar.size(); i++) {
                    Monster m = allSimilar.get(i);
                    Attribute element = m.getElement().equals("null") ? null : Attribute.values()[Integer.parseInt(m.getElement())];
                    switch (element) {
                        case FIRE:
                            fireCount++;
                            break;
                        case WATER:
                            waterCount++;
                            break;
                        case WOOD:
                            woodCount++;
                            break;
                        case LIGHT:
                            lightCount++;
                            break;
                        case DARK:
                            darkCount++;
                            break;

                    }
                }
                Attribute majorityAttr = Attribute.FIRE;
                int majorityAttrCount = fireCount;
                if (waterCount > majorityAttrCount) {
                    majorityAttr = Attribute.WATER;
                    majorityAttrCount = waterCount;
                }
                if (woodCount > majorityAttrCount) {
                    majorityAttr = Attribute.WOOD;
                    majorityAttrCount = woodCount;
                }
                if (lightCount > majorityAttrCount) {
                    majorityAttr = Attribute.LIGHT;
                    majorityAttrCount = lightCount;
                }
                if (darkCount > majorityAttrCount) {
                    majorityAttr = Attribute.DARK;
                    majorityAttrCount = darkCount;
                }

                ArrayList<Monster> majorityMonstersWithMajorityAttr = new ArrayList<>();
                for (int i = 0; i < allSimilar.size(); i++) {
                    Monster m = allSimilar.get(i);
                    if (m.getName().equalsIgnoreCase(keywords)) {
                        return m;
                    }
                    Attribute element = m.getElement().equals("null") ? null : Attribute.values()[Integer.parseInt(m.getElement())];
                    if (element == majorityAttr)
                        majorityMonstersWithMajorityAttr.add(m);
                }
                majorityMonstersWithMajorityAttr.sort(new Comparator<Monster>() {
                    @Override
                    public int compare(Monster o1, Monster o2) {
                        int id1 = o1.getId();
                        int id2 = o2.getId();
                        if (id1 > id2)
                            return 1;
                        else if (id1 == id2)
                            return 0;
                        else
                            return -1;
                    }
                });
                Monster biggestNumMon = majorityMonstersWithMajorityAttr.get(majorityMonstersWithMajorityAttr.size() - 1);
                ArrayList<Integer> evos = getEvos(biggestNumMon.getId());
                for (int i = 0; i < evos.size(); i++) {
                    if (evos.get(i) > biggestNumMon.getId()) {
                        biggestNumMon = getMonster(evos.get(i));
                    }
                }
                return biggestNumMon;
            } else {
                return null;
            }
        }
    }

    public static ArrayList<Monster> getAllMonsters(String keywords) {
        ArrayList<Monster> monsters = new ArrayList<>();
        ArrayList<Monster> exactMonsters = new ArrayList<>();
        try {
            int id = Integer.parseInt(keywords.trim());
            monsters.add(getMonster(id));
            return monsters;
        } catch (NumberFormatException e) {
            JsonParser parser = new JsonParser();
            try {
                JsonArray jsonArray = (JsonArray) parser.parse(new FileReader(path + "monsters.json"));
                for (Object o : jsonArray) {
                    JsonObject obj = (JsonObject) o;
                    if (stringContainsKeywords(obj.get("name").toString(), keywords) || stringContainsKeywords(obj.get("name_jp").toString(), keywords)) {
                        monsters.add(new Monster(obj));
                    }
                }
                //Find monsters with an exact word match.
                String[] keywordSplit = keywords.toLowerCase().replaceAll("[^\\w\\s]", "").split(" ");
                for (int i = 0; i < monsters.size(); i++) {
                    String[] nameSplit = monsters.get(i).getName().toLowerCase().replaceAll("[^\\w\\s]", "").split(" ");
                    boolean[] found = new boolean[keywordSplit.length];
                    for (int j = 0; j < nameSplit.length; j++) {
                        for (int k = 0; k < keywordSplit.length; k++) {
                            if (nameSplit[j].equalsIgnoreCase(keywordSplit[k])) {
                                found[k] = true;
                                nameSplit[j] = "";
                                keywordSplit[k] = "";
                            }
                        }
                    }
                    boolean hasAMatch = false;
                    for (int j = 0; j < found.length; j++) {
                        if (found[j])
                            hasAMatch = true;
                    }
                    if (hasAMatch) {
                        exactMonsters.add(monsters.get(i));
                    }
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            if (exactMonsters.size() > 0)
                return exactMonsters;
            return monsters;
        }
    }

    public static ActiveSkill getActive(String keywords) {
        JsonParser parser = new JsonParser();
        try {
            JsonArray jsonArray = (JsonArray) parser.parse(new FileReader(path + "active_skills.json"));
            for (Object o : jsonArray) {
                JsonObject obj = (JsonObject) o;
                String currentActiveName = StringEscapeUtils.unescapeJava(obj.get("name").toString()).replace("\"", "");
                if (stringContainsKeywords(currentActiveName, keywords)) {
                    return new ActiveSkill(obj);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static LeaderSkill getLeader(String keywords) {
        JsonParser parser = new JsonParser();
        try {
            JsonArray jsonArray = (JsonArray) parser.parse(new FileReader(path + "leader_skills.json"));
            for (Object o : jsonArray) {
                JsonObject obj = (JsonObject) o;
                String currentLeaderName = StringEscapeUtils.unescapeJava(obj.get("name").toString()).replace("\"", "");
                if (stringContainsKeywords(currentLeaderName, keywords)) {
                    return new LeaderSkill(obj);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static AwokenSkill getAwokenSkill(int id) {
        JsonParser parser = new JsonParser();
        try {
            JsonArray jsonArray = (JsonArray) parser.parse(new FileReader(path + "awakenings.json"));
            for (Object o : jsonArray) {
                JsonObject obj = (JsonObject) o;
                if (obj.get("id").getAsInt() == id) {
                    return new AwokenSkill(obj);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static AwokenSkill getAwokenSkill(String keywords) {
        JsonParser parser = new JsonParser();
        try {
            JsonArray jsonArray = (JsonArray) parser.parse(new FileReader(path + "awakenings.json"));
            for (Object o : jsonArray) {
                JsonObject obj = (JsonObject) o;
                if (stringContainsKeywords(obj.get("name").toString(), keywords)) {
                    return new AwokenSkill(obj);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<Integer> getEvos(int id) {
        ArrayList<Integer> evos = getEvos_sub(id, new ArrayList<>());
        evos.remove(new Integer(id));
        evos.sort(Integer::compareTo);
        return evos;
    }

    private static ArrayList<Integer> getEvos_sub(int id, ArrayList<Integer> checked) {
        JsonParser parser = new JsonParser();
        try {
            JsonObject jsonObject = (JsonObject) parser.parse(new FileReader(path + "evolutions.json"));
            JsonElement jsonElement = jsonObject.get("" + id);
            if (jsonElement != null) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                for (Object o : jsonArray) {
                    JsonObject obj = (JsonObject) o;
                    int evoNum = obj.get("evolves_to").getAsInt();
                    if (!checked.contains(evoNum)) {
                        checked.add(evoNum);
                        getEvos_sub(evoNum, checked);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return checked;
    }

    private static boolean stringContainsKeywords(String string, String keywords) {
        string = string.replaceAll("-", " ");
        keywords = keywords.replaceAll("-", " ");
        String basicString = string.toLowerCase().replaceAll("[^\\w\\s]", "");
        String basicKeywords = keywords.toLowerCase().replaceAll("[^\\w\\s]", "");
        if (basicString.toLowerCase().equalsIgnoreCase(basicKeywords.toLowerCase())) {
            return true;
        }
        String[] split = basicString.split(" ");
        String[] words = basicKeywords.split(" ");

        ArrayList<String> stringWords = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            stringWords.add(split[i]);
        }
        ArrayList<String> keywordWords = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            keywordWords.add(words[i]);
        }
        keywordWords.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1.length() == o2.length())
                    return 0;
                else if (o1.length() > o2.length())
                    return -1;
                else return 1;
            }
        });

        boolean[] found = new boolean[words.length];
        for (int i = 0; i < split.length; i++) {
            for (int j = 0; j < words.length; j++) {
                if (stringWords.get(i).contains(keywordWords.get(j))) {
                    found[j] = true;
                    stringWords.set(i, "");
                    keywordWords.set(j, "");
                }
            }
        }
        for (int i = 0; i < found.length; i++) {
            if (!found[i])
                return false;
        }
        return true;
    }
}
