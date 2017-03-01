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
            int[] elements = parseAttribute(keywords);
            int e1 = elements[0];
            int e2 = elements[1];
            boolean match1 = false;
            boolean match2 = false;
            if (e1 != -1)
                match1 = true;
            if (e2 != -1)
                match2 = true;

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
                    Monster m = getMonster(evos.get(i));
                    Attribute a = Attribute.values()[Integer.parseInt(m.getElement())];
                    if (evos.get(i) > biggestNumMon.getId() && a.equals(majorityAttr)) {
                        if (match1 && Integer.parseInt(m.getElement()) == e1) {
                            if (match2 && Integer.parseInt(m.getElement2()) == e2) {
                                biggestNumMon = m;
                            } else if (!match2) {
                                biggestNumMon = m;
                            }
                        }
                    }
                }
                return biggestNumMon;
            } else {
                return null;
            }
        }
    }

    public static ArrayList<Monster> getAllMonsters(String keywords) {
        int element1 = -1;
        int element2 = -1;
        boolean matchEle1 = false;
        boolean matchEle2 = false;
        int[] elements = parseAttribute(keywords);
        element1 = elements[0];
        element2 = elements[1];
        if (element1 != -1)
            matchEle1 = true;
        if (element2 != -1)
            matchEle2 = true;

        if (matchEle1 || matchEle2) {
            keywords = removeAttFromKeyword(keywords);
        }

        ArrayList<Monster> monsters = new ArrayList<>();
        ArrayList<Monster> exactMonsters = new ArrayList<>();
        ArrayList<Monster> superExactMonsters = new ArrayList<>();
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
                        if (!matchEle1) {
                            monsters.add(new Monster(obj));
                        } else if (matchEle1) {
                            if (obj.get("element").getAsInt() == element1) {
                                if (matchEle2) {
                                    int tmpE2 = -1;
                                    try {
                                        tmpE2 = obj.get("element2").getAsInt();
                                    } catch (UnsupportedOperationException ex) {
                                    }
                                    if (tmpE2 >= 0 && tmpE2 == element2) {
                                        monsters.add(new Monster(obj));
                                    }
                                } else {
                                    monsters.add(new Monster(obj));
                                }
                            }
                        }
                    }
                }
                //Find monsters with an exact word match.
                for (int i = 0; i < monsters.size(); i++) {
                    String[] keywordSplit = normalizeString(keywords).split(" ");
                    String[] nameSplit = normalizeString(monsters.get(i).getName()).split(" ");
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
                    int matches = 0;
                    for (int j = 0; j < found.length; j++) {
                        if (found[j])
                            matches++;
                    }
                    if (matches == keywordSplit.length)
                        superExactMonsters.add(monsters.get(i));
                    else if (matches > 0)
                        exactMonsters.add(monsters.get(i));
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            if (superExactMonsters.size() > 0)
                return superExactMonsters;
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
                if (stringContainsKeywords(currentActiveName, keywords) || keywords.equalsIgnoreCase(currentActiveName)) {
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
            int i = 0;
            for (Object o : jsonArray) {
                JsonObject obj = (JsonObject) o;
                String currentLeaderName = StringEscapeUtils.unescapeJava(obj.get("name").toString()).replace("\"", "");
                if (stringContainsKeywords(currentLeaderName, keywords) || keywords.equalsIgnoreCase(currentLeaderName)) {
                    return new LeaderSkill(obj);
                }
                i++;
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

    private static String normalizeString(String str) {
        return str.replace("-", " ").toLowerCase().replaceAll("[^\\w\\s]", "").trim();
    }

    private static boolean stringContainsKeywords(String string, String keywords) {
        if (string.equals(string.toLowerCase())) {
            return false;
        }
        String basicString = normalizeString(string);
        String basicKeywords = normalizeString(keywords);
        if (basicString.contains("tamadra") && !(basicKeywords.contains("tamadra") || basicKeywords.contains("tama"))) {
            return false;
        }
        if (basicString.length() == 0 || basicKeywords.length() == 0) {
            return false;
        }
        if (basicString.toLowerCase().equalsIgnoreCase(basicKeywords.toLowerCase())) {
            return true;
        }
        if (basicString.length() == 0) {
            return false;
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
                if (stringWords.get(i).contains(keywordWords.get(j))
                        && stringWords.get(i).length() > 0
                        && keywordWords.get(j).length() > 0) {
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

    public static int[] parseAttribute(String keywords) {
        int[] atts = new int[]{-1, -1};
        int attIndex = 0;
        String[] split = normalizeString(keywords).split(" ");
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if (s.length() <= 2) {
                char e1;
                char e2 = 'z';
                e1 = s.charAt(0);
                if (s.length() == 2) {
                    e2 = s.charAt(1);
                }
                switch (e1) {
                    case 'r':
                        atts[0] = 0;
                        break;
                    case 'b':
                        atts[0] = 1;
                        break;
                    case 'g':
                        atts[0] = 2;
                        break;
                    case 'l':
                        atts[0] = 3;
                        break;
                    case 'd':
                        atts[0] = 4;
                        break;
                    default:
                        return atts;
                }
                switch (e2) {
                    case 'r':
                        atts[1] = 0;
                        break;
                    case 'b':
                        atts[1] = 1;
                        break;
                    case 'g':
                        atts[1] = 2;
                        break;
                    case 'l':
                        atts[1] = 3;
                        break;
                    case 'd':
                        atts[1] = 4;
                        break;
                    default:
                        atts[0] = -1;
                        atts[1] = -1;
                        break;
                }
                attIndex = i;
                break;
            }
        }
        return atts;
    }

    public static String removeAttFromKeyword(String keywords) {
        String strToLookFor = "";
        int[] atts = parseAttribute(keywords);
        switch (atts[0]) {
            case 0:
                strToLookFor += 'r';
                break;
            case 1:
                strToLookFor += 'b';
                break;
            case 2:
                strToLookFor += 'g';
                break;
            case 3:
                strToLookFor += 'l';
                break;
            case 4:
                strToLookFor += 'd';
                break;
        }
        switch (atts[1]) {
            case 0:
                strToLookFor += 'r';
                break;
            case 1:
                strToLookFor += 'b';
                break;
            case 2:
                strToLookFor += 'g';
                break;
            case 3:
                strToLookFor += 'l';
                break;
            case 4:
                strToLookFor += 'd';
                break;
        }
        String[] split = normalizeString(keywords).split(" ");
        int attIndex = 0;
        for (int i = 0; i < split.length; i++) {
            if (split[i].equals(strToLookFor)) {
                attIndex = i;
                break;
            }
        }
        String newKeyword = "";
        for (int i = 0; i < split.length; i++) {
            if (i != attIndex) {
                newKeyword += split[i] + " ";
            }
        }
        return newKeyword.trim();
    }
}
