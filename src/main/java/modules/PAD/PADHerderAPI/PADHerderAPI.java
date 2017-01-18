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
            JsonParser parser = new JsonParser();
            try {
                JsonArray jsonArray = (JsonArray) parser.parse(new FileReader(path + "monsters.json"));
                for (Object o : jsonArray) {
                    JsonObject obj = (JsonObject) o;
                    if (stringContainsKeywords(obj.get("name").toString(), keywords) || stringContainsKeywords(obj.get("name_jp").toString(), keywords)) {
                        return new Monster(obj);
                    }
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    public static ArrayList<Monster> getAllMonsters(String keywords) {
        ArrayList<Monster> monsters = new ArrayList<>();
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
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
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
        String[] words = keywords.toLowerCase().split(" ");
        for (int i = 0; i < words.length; i++) {
            if (!string.toLowerCase().contains(words[i])) {
                return false;
            }
        }
        return true;
    }
}
