package modules;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Iggie on 12/30/2016.
 */

public class DBDriver {
    private DB db;

    public DBDriver(DB db) {
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

    public void addPerms(String table, String colValToAddTo, String[] colNames, String[] addVals, boolean dupe) {
        if (colNames.length - 1 == addVals.length) {
            String[] oldVals = new String[addVals.length];
            for (int i = 0; i < oldVals.length; i++) {
                String val = getPerms(table, colNames[0], colValToAddTo, colNames[i + 1]);
                if (addVals[i] == null || addVals[i].equalsIgnoreCase("null")) {
                    addVals[i] = "";
                }
                if (val.length() > 0 && addVals[i].length() > 0) {
                    val += ";";
                }
                oldVals[i] = val;
            }
            ResultSet rs = db.executeQuery("SELECT " + "*" + " FROM " + table + " WHERE " + colNames[0] + "='" + colValToAddTo + "'");
            try {
                if (rs.next()) {
                    if (!dupe) {
                        String sql = "UPDATE " + table + " SET ";
                        for (int i = 0; i < addVals.length; i++) {
                            if (i != addVals.length - 1) {
                                sql += colNames[i + 1] + "='" + oldVals[i] + addVals[i] + "', ";
                            } else {
                                sql += colNames[i + 1] + "='" + oldVals[i] + addVals[i] + "' ";
                            }
                        }
                        sql += "WHERE " + colNames[0] + "='" + colValToAddTo + "'";
                        db.execute(sql);
                    } else {
                        String sql = "INSERT INTO " + table + " (";
                        for (int i = 0; i < colNames.length; i++) {
                            if (i != colNames.length - 1) {
                                sql += colNames[i] + ",";
                            } else {
                                sql += colNames[i];
                            }
                        }
                        sql += ") VALUES ('" + colValToAddTo + "',";
                        for (int i = 0; i < oldVals.length; i++) {
                            if (i != oldVals.length - 1) {
                                sql += "'" + addVals[i] + "'" + ",";
                            } else {
                                sql += "'" + addVals[i] + "'";
                            }
                        }
                        sql += ")";
                        db.execute(sql);
                    }
                } else {
                    String sql = "INSERT INTO " + table + " (";
                    for (int i = 0; i < colNames.length; i++) {
                        if (i != colNames.length - 1) {
                            sql += colNames[i] + ",";
                        } else {
                            sql += colNames[i];
                        }
                    }
                    sql += ") VALUES ('" + colValToAddTo + "',";
                    for (int i = 0; i < oldVals.length; i++) {
                        if (i != oldVals.length - 1) {
                            sql += "'" + oldVals[i] + addVals[i] + "'" + ",";
                        } else {
                            sql += "'" + oldVals[i] + addVals[i] + "'";
                        }
                    }
                    sql += ")";
                    db.execute(sql);
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("addVals array length is not 1 less than the colNames array.");
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

    public void resetPerms(String table, String[] colNames, String col1Val) {
        String sql = "UPDATE " + table + " SET ";
        for (int i = 1; i < colNames.length; i++) {
            if (i != colNames.length - 1) {
                sql += colNames[i] + "=''" + ", ";
            } else {
                sql += colNames[i] + "=''" + " ";
            }
        }
        sql += "WHERE " + colNames[0] + "='" + col1Val + "'";
        db.execute(sql);
    }

    public void createTable(String table, String[] colNames, String[] colTypes, boolean overwrite) {
        if (!db.tableExists(table) || overwrite) {
            if (colNames.length == colTypes.length) {
                if (overwrite) {
                    deleteTable(table);
                }
                String sql = "CREATE TABLE " + table + " (";
                for (int i = 0; i < colNames.length; i++) {
                    if (i != colNames.length - 1) {
                        sql += colNames[i] + " " + colTypes[i] + ", ";
                    } else {
                        sql += colNames[i] + " " + colTypes[i] + ")";
                    }
                }
                db.execute(sql);
            } else {
                System.out.println("Array lengths do no match");
            }
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

