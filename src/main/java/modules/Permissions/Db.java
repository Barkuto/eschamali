package modules.Permissions;

import java.sql.*;

/**
 * Created by Iggie on 8/30/2016.
 */
public class Db {
    private String database;
    private String conUrl;
    private Connection con;
    private Statement stm;

    public Db(String database) {
        this.database = database;
        this.conUrl = "jdbc:sqlite:" + database;
        try {
            con = DriverManager.getConnection(conUrl);
            stm = con.createStatement();
            stm.setQueryTimeout(30);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void execute(String sql) {
        try {
            stm.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet executeQuery(String sql) {
        try {
            return stm.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() {
        if (stm != null) {
            try {
                stm.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public String getDatabasePath() {
        return database;
    }

    public boolean tableExists(String table) {
        try {
            stm.executeQuery("SELECT 1 FROM " + table);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
