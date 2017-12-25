package modules.PAD.PADHerderAPI;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class PADHerderAPI {
    private static String path = "modules/PAD/PHAPI/";
    private static String sqldriver = "jdbc:sqlite:";
    private static String activesDB = "actives.db";
    private static String awakeningsDB = "awakenings.db";
    private static String evosDB = "evos.db";
    private static String leadersDB = "leaders.db";
    private static String monstersDB = "monsters.db";

    public static void updateDB() {
        try {
            ArrayList<URL> urls = new ArrayList<>();
            urls.add(new URL("https://www.padherder.com/api/active_skills/"));
            urls.add(new URL("https://www.padherder.com/api/awakenings/"));
            urls.add(new URL("https://www.padherder.com/api/evolutions/"));
            urls.add(new URL("https://www.padherder.com/api/leader_skills/"));
            urls.add(new URL("https://www.padherder.com/api/monsters/"));

            ArrayList<JsonElement> jsons = new ArrayList<>();

            for (URL u : urls) {
                URLConnection conn = u.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
                conn.connect();

                JsonParser parser = new JsonParser();
                jsons.add(parser.parse(IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8)));
            }

            createActivesDB(jsons.get(0).getAsJsonArray());
            createAwakeningsDB(jsons.get(1).getAsJsonArray());
            createEvosDB(jsons.get(2).getAsJsonObject());
            createLeadersDB(jsons.get(3).getAsJsonArray());
            createMonstersDB(jsons.get(4).getAsJsonArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createActivesDB(JsonArray json) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + activesDB);
            Statement stm = con.createStatement();

            stm.execute("DROP TABLE IF EXISTS actives");
            stm.execute("CREATE TABLE actives (name string, mincd int, maxcd int, effect string)");

            for (int i = 0; i < json.size(); i++) {
                PreparedStatement pstm = con.prepareStatement("INSERT INTO actives (name, mincd, maxcd, effect) VALUES (?, ?, ?, ?)");
                JsonObject obj = json.get(i).getAsJsonObject();

                pstm.setString(1, obj.get("name").getAsString());
                pstm.setInt(2, obj.get("min_cooldown").getAsInt());
                pstm.setInt(3, obj.get("max_cooldown").getAsInt());
                pstm.setString(4, obj.get("effect").getAsString());
                pstm.execute();
            }

            stm.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createAwakeningsDB(JsonArray json) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + awakeningsDB);
            Statement stm = con.createStatement();

            stm.execute("DROP TABLE IF EXISTS awakenings");
            stm.execute("CREATE TABLE awakenings (id int, name string, desc string)");

            for (int i = 0; i < json.size(); i++) {
                PreparedStatement pstm = con.prepareStatement("INSERT INTO awakenings (id, name, desc) VALUES (?, ?, ?)");
                JsonObject obj = json.get(i).getAsJsonObject();

                pstm.setInt(1, obj.get("id").getAsInt());
                pstm.setString(2, obj.get("name").getAsString());
                pstm.setString(3, obj.get("desc").getAsString());
                pstm.execute();
            }

            stm.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createEvosDB(JsonObject json) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + evosDB);
            Statement stm = con.createStatement();

            stm.execute("DROP TABLE IF EXISTS evos");
            stm.execute("CREATE TABLE evos (id int, evosto int, mats object)");

            Set<Map.Entry<String, JsonElement>> set = json.entrySet();
            for (Map.Entry e : set) {
                PreparedStatement pstm = con.prepareStatement("INSERT INTO evos (id, evosto, mats) VALUES (?, ?, ?)");

                int id = Integer.parseInt(e.getKey().toString());
                JsonArray evos = (JsonArray) e.getValue();
                for (int i = 0; i < evos.size(); i++) {
                    JsonObject obj = evos.get(i).getAsJsonObject();

                    int to = obj.get("evolves_to").getAsInt();
                    JsonArray mats = obj.get("materials").getAsJsonArray();
                    ArrayList<int[]> matArrayList = new ArrayList<>();
                    for (JsonElement elem : mats) {
                        JsonArray ele = elem.getAsJsonArray();
                        matArrayList.add(new int[]{ele.get(0).getAsInt(), ele.get(1).getAsInt()});
                    }

                    pstm.setInt(1, id);
                    pstm.setInt(2, to);
                    pstm.setObject(3, matArrayList.toArray());
                    pstm.execute();
                }
            }

            stm.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createLeadersDB(JsonArray json) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + leadersDB);
            Statement stm = con.createStatement();

            stm.execute("DROP TABLE IF EXISTS leaders");
            stm.execute("CREATE TABLE leaders (name string, effect string)");

            for (int i = 0; i < json.size(); i++) {
                PreparedStatement pstm = con.prepareStatement("INSERT INTO leaders (name, effect) VALUES (?, ?)");
                JsonObject obj = json.get(i).getAsJsonObject();

                pstm.setString(1, obj.get("name").getAsString());
                pstm.setString(2, obj.get("effect").getAsString());
                pstm.execute();
            }

            stm.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createMonstersDB(JsonArray json) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + monstersDB);
            Statement stm = con.createStatement();

            stm.execute("DROP TABLE IF EXISTS monsters");
            stm.execute("CREATE TABLE monsters (id int, name string, jpname string, att1 string, att2 string, monster blob)");

            for (int i = 0; i < json.size(); i++) {
                PreparedStatement pstm = con.prepareStatement("INSERT INTO monsters (id, name, jpname, att1, att2, monster) VALUES (?, ?, ?, ?, ?, ?)");
                JsonObject obj = json.get(i).getAsJsonObject();

                int id;
                try {
                    id = obj.get("pdx_id").getAsInt();
                } catch (NullPointerException e) {
                    id = obj.get("id").getAsInt();
                }
                String name = obj.get("name").toString().replace("\"", "");
                String name_jp = obj.get("name_jp").toString().replace("\"", "");
                int rarity = obj.get("rarity").getAsInt();
                int team_cost = obj.get("team_cost").getAsInt();
                int monster_points = obj.get("monster_points").getAsInt();
                Attribute att1 = Attribute.values()[obj.get("element").getAsInt()];
                String element2 = obj.get("element2").toString();
                Attribute att2 = element2.equalsIgnoreCase("null") ? Attribute.NONE : Attribute.values()[obj.get("element2").getAsInt()];
                Type type = getType(obj.get("type").getAsInt());
                Type type2 = obj.get("type2").toString().equals("null") ? Type.NONE : getType(obj.get("type2").getAsInt());
                Type type3 = obj.get("type3").toString().equals("null") ? Type.NONE : getType(obj.get("type3").getAsInt());
                Active active = getActive(obj.get("active_skill").toString().replace("\"", ""));
                Leader leader = getLeader(obj.get("leader_skill").toString().replace("\"", ""));
                JsonArray array = obj.get("awoken_skills").getAsJsonArray();
//                int[] awakenings = new int[array.size()];
//                for (int j = 0; j < array.size(); j++) {
//                    awakenings[j] = array.get(j).getAsInt();
//                }
                Awakening[] awakenings = new Awakening[array.size()];
                for (int j = 0; j < array.size(); j++) {
                    awakenings[j] = getAwakening(array.get(j).getAsInt());
                }
                int hp_min = obj.get("hp_min").getAsInt();
                int hp_max = obj.get("hp_max").getAsInt();
                int atk_min = obj.get("atk_min").getAsInt();
                int atk_max = obj.get("atk_max").getAsInt();
                int rcv_min = obj.get("rcv_min").getAsInt();
                int rcv_max = obj.get("rcv_max").getAsInt();

                int max_level = obj.get("max_level").getAsInt();
                int xp_curve = obj.get("xp_curve").getAsInt();

                boolean jp_only = obj.get("jp_only").getAsBoolean();

                Monster m = new Monster(id, name, name_jp, rarity, team_cost, monster_points, att1, att2,
                        type, type2, type3, active, leader, awakenings,
                        hp_min, hp_max, atk_min, atk_max, rcv_min, rcv_max, max_level, xp_curve, jp_only);
                pstm.setInt(1, id);
                pstm.setString(2, name);
                pstm.setString(3, name_jp);
                pstm.setString(4, att1.toString());
                pstm.setString(5, att2.toString());
                pstm.setBytes(6, monsterToBytes(m));
                pstm.execute();
            }

            stm.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static byte[] monsterToBytes(Monster object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Monster bytesToMonster(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput in = new ObjectInputStream(bis)) {
            return (Monster) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Active getActive(String name) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + activesDB);
            PreparedStatement pstm = con.prepareStatement("SELECT * FROM actives WHERE name=?");
            pstm.setString(1, name);
            ResultSet result = pstm.executeQuery();
            if (result.next()) {
                return new Active(
                        result.getString("name"),
                        result.getString("effect"),
                        result.getInt("mincd"),
                        result.getInt("maxcd"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Active("", "", 0, 0);
    }

    private static Leader getLeader(String name) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + leadersDB);
            PreparedStatement pstm = con.prepareStatement("SELECT * FROM leaders WHERE name=?");
            pstm.setString(1, name);
            ResultSet result = pstm.executeQuery();
            if (result.next()) {
                return new Leader(
                        result.getString("name"),
                        result.getString("effect"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Leader("", "");
    }

    private static Awakening getAwakening(int id) {
        for (Awakening a : Awakening.values()) {
            if (a.getID() == id)
                return a;
        }
        return Awakening.UNKNOWN;
    }

    private static Type getType(int id) {
        for (Type t : Type.values()) {
            if (t.getID() == id)
                return t;
        }
        return Type.NONE;
    }

    public static ArrayList<Monster> getAllMonsters(String query) {
        ArrayList<Monster> monsters = new ArrayList<>();
        try {
            int id = Integer.parseInt(query);
            Monster m = getMonster(id);
            if (m != null)
                monsters.add(m);
            return monsters;
        } catch (NumberFormatException ignored) {
        }
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + monstersDB);
            Attribute[] atts = parseAttribute(query);
            String[] split = removeAttFromKeyword(query).split(" ");
            StringBuilder stmt = new StringBuilder("SELECT monster FROM monsters WHERE ");

            if (split.length == 0)
                return null;

            if (atts != null) {
                if (atts[0] != null) {
                    stmt.append("att1=? AND ");
                }
                if (atts[1] != null) {
                    stmt.append("att2=? AND ");
                }
            }

            for (String s : split) {
                stmt.append("( name LIKE ? OR jpname LIKE ? ) AND ");
            }
            String finishedStmt = stmt.toString().substring(0, stmt.lastIndexOf("AND")).trim();

            PreparedStatement pstm = con.prepareStatement(finishedStmt);

            int startIndex = 1;
            if (atts != null && atts[0] != null) {
                pstm.setString(startIndex++, atts[0].toString());
                if (atts[1] != null)
                    pstm.setString(startIndex++, atts[1].toString());
            }
            for (String s : split) {
                pstm.setString(startIndex++, "%" + s + "%");
                pstm.setString(startIndex++, "%" + s + "%");
            }
            ResultSet result = pstm.executeQuery();
            while (result.next()) {
                monsters.add(bytesToMonster(result.getBytes("monster")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return monsters;
        }
        return monsters;
    }

    private static Monster getMonster(int id) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + monstersDB);
            PreparedStatement pstm = con.prepareStatement("SELECT * FROM monsters WHERE id=?");
            pstm.setInt(1, id);
            ResultSet result = pstm.executeQuery();
            if (result.next()) {
                return bytesToMonster(result.getBytes("monster"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Monster getMonster(String query) {
        ArrayList<Monster> monsters = getAllMonsters(query);
        double bestWeight = -999.0;
        Monster highestWeighted = null;
        for (Monster m : monsters) {
            if (m.getWeighted() > bestWeight) {
                highestWeighted = m;
                bestWeight = m.getWeighted();
            }
        }
        return highestWeighted;
    }

    public static ArrayList<Integer> getEvos(int id) {
        ArrayList<Integer> evos = getEvos(id, new ArrayList<>());
        ArrayList<Integer> preEvos = getPreEvos(id, new ArrayList<>());
        ArrayList<Integer> allEvos = new ArrayList<>();
        for (Integer i : evos) {
            if (!allEvos.contains(i))
                allEvos.add(i);
        }
        for (Integer i : preEvos) {
            if (!allEvos.contains(i))
                allEvos.add(i);
        }
        allEvos.remove(new Integer(id));
        allEvos.sort(Integer::compareTo);
        return allEvos;
    }

    private static ArrayList<Integer> getEvos(int id, ArrayList<Integer> checked) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + evosDB);

            PreparedStatement pstm = con.prepareStatement("SELECT * FROM evos WHERE id=?");
            pstm.setInt(1, id);
            ResultSet result = pstm.executeQuery();
            while (result.next()) {
                int toID = result.getInt("evosto");
                if (!checked.contains(toID)) {
                    checked.add(toID);
                    getEvos(toID, checked);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return checked;
    }

    private static ArrayList<Integer> getPreEvos(int id, ArrayList<Integer> checked) {
        try {
            Connection con = DriverManager.getConnection(sqldriver + path + evosDB);

            PreparedStatement pstm = con.prepareStatement("SELECT * FROM evos WHERE evosto=?");
            pstm.setInt(1, id);
            ResultSet result = pstm.executeQuery();
            while (result.next()) {
                int fromID = result.getInt("id");
                if (!checked.contains(fromID)) {
                    checked.add(fromID);
                    getPreEvos(fromID, checked);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return checked;
    }

    private static Attribute[] parseAttribute(String keywords) {
        Attribute[] atts = new Attribute[]{null, null};
        String[] split = keywords.trim().split(" ");
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
                        atts[0] = Attribute.FIRE;
                        break;
                    case 'b':
                        atts[0] = Attribute.WATER;
                        break;
                    case 'g':
                        atts[0] = Attribute.WOOD;
                        break;
                    case 'l':
                        atts[0] = Attribute.LIGHT;
                        break;
                    case 'd':
                        atts[0] = Attribute.DARK;
                        break;
                    default:
                        return atts;
                }
                switch (e2) {
                    case 'r':
                        atts[1] = Attribute.FIRE;
                        break;
                    case 'b':
                        atts[1] = Attribute.WATER;
                        break;
                    case 'g':
                        atts[1] = Attribute.WOOD;
                        break;
                    case 'l':
                        atts[1] = Attribute.LIGHT;
                        break;
                    case 'd':
                        atts[1] = Attribute.DARK;
                        break;
                    case 'x':
                        atts[1] = Attribute.NONE;
                        break;
                    default:
                        if (s.length() == 2)
                            atts[0] = null;
                        atts[1] = null;
                        break;
                }
                break;
            }
        }
        return atts[0] == null ? null : atts;
    }

    private static String removeAttFromKeyword(String keywords) {
        String strToLookFor = "";
        Attribute[] atts = parseAttribute(keywords);
        if (atts == null || atts[0] == null)
            return keywords;
        switch (atts[0]) {
            case FIRE:
                strToLookFor += 'r';
                break;
            case WATER:
                strToLookFor += 'b';
                break;
            case WOOD:
                strToLookFor += 'g';
                break;
            case LIGHT:
                strToLookFor += 'l';
                break;
            case DARK:
                strToLookFor += 'd';
                break;
        }
        if (atts[1] != null) {
            switch (atts[1]) {
                case FIRE:
                    strToLookFor += 'r';
                    break;
                case WATER:
                    strToLookFor += 'b';
                    break;
                case WOOD:
                    strToLookFor += 'g';
                    break;
                case LIGHT:
                    strToLookFor += 'l';
                    break;
                case DARK:
                    strToLookFor += 'd';
                    break;
                case NONE:
                    strToLookFor += 'x';
                    break;
            }
        }
        String[] split = keywords.split(" ");
        int attIndex = 0;
        for (int i = 0; i < split.length; i++) {
            if (split[i].equals(strToLookFor)) {
                attIndex = i;
                break;
            }
        }
        StringBuilder newKeyword = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i != attIndex) {
                newKeyword.append(split[i]).append(" ");
            }
        }
        return newKeyword.toString().trim();
    }
}
