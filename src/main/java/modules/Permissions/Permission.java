package modules.Permissions;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Iggie on 8/30/2016.
 */
public class Permission {
    private Db db;

    public Permission(Db db) {
        this.db = db;
    }

    public String getPerms(String table, String col1, String col1Val, String col2) {
        //Return col2 val
        String s = "";
        ResultSet rs = db.executeQuery("SELECT * FROM " + table + " WHERE " + col1 + "='" + col1Val + "'");
        try {
            if (rs.next()) {
                s = rs.getString(col2);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return s;
    }

    public ResultSet selectAllFrom(String table) {
        return db.executeQuery("SELECT * FROM " + table);
    }

    public void setPerms(String table, String col1, String col1Val, String col2, String newVal) {
        ResultSet rs = db.executeQuery("SELECT " + "*" + " FROM " + table + " WHERE " + col1 + "='" + col1Val + "'");
        try {
            if (rs.next()) {
                db.execute("UPDATE " + table + " SET " + col2 + "='" + newVal + "' WHERE " + col1 + "='" + col1Val + "'");
            } else {
                db.execute("INSERT INTO " + table + " (" + col1 + "," + col2 + ") VALUES ('" + col1Val + "','" + newVal + "')");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addPerms(String table, String col1, String col1Val, String col2, String addVal) {
        String oldVal = getPerms(table, col1, col1Val, col2).trim();
        if (oldVal.length() > 1) {
            oldVal += ";";
        }
        ResultSet rs = db.executeQuery("SELECT " + "*" + " FROM " + table + " WHERE " + col1 + "='" + col1Val + "'");
        try {
            if (rs.next()) {
                db.execute("UPDATE " + table + " SET " + col2 + "='" + oldVal + addVal + "' WHERE " + col1 + "='" + col1Val + "'");
            } else {
                db.execute("INSERT INTO " + table + " (" + col1 + "," + col2 + ") VALUES ('" + col1Val + "','" + addVal + "')");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deletePerms(String table, String col1, String col1Val) {
        ResultSet rs = db.executeQuery("SELECT " + "*" + " FROM " + table + " WHERE " + col1 + "='" + col1Val + "'");
        try {
            if (rs.next()) {
                db.execute("DELETE FROM " + table + " WHERE " + col1 + "='" + col1Val + "'");
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetPerms(String table, String col1, String col1Val, String col2) {
        db.execute("UPDATE " + table + " SET " + col2 + "='' WHERE " + col1 + "='" + col1Val + "'");
    }

    public void createTable(String table, String col1, String col1Type, String col2, String col2Type) {
        if (!db.tableExists(table)) {
            db.execute("CREATE TABLE " + table + " (" + col1 + " " + col1Type + ", " + col2 + " " + col2Type + ")");
        }
    }

    public void deleteTable(String table) {
        db.execute("DROP TABLE IF EXISTS " + table);
    }

    public void close() {
        db.close();
    }

    public boolean tableExists(String table) {
        return db.tableExists(table);
    }
}
