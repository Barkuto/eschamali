package modules.PAD;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Created by Iggie on 9/13/2016.
 */
public class Guerilla implements Serializable {
    private ArrayList<String> dungeons;
    private ArrayList<LocalTime> a;
    private ArrayList<LocalTime> b;
    private ArrayList<LocalTime> c;
    private ArrayList<LocalTime> d;
    private ArrayList<LocalTime> e;

    public Guerilla(ArrayList<String> dungeons, ArrayList<LocalTime> a, ArrayList<LocalTime> b, ArrayList<LocalTime> c, ArrayList<LocalTime> d, ArrayList<LocalTime> e) {
        this.dungeons = dungeons;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    public String allGroups(String timezone) {
        int plusHours = 0;
        switch (timezone) {
            case "est":
                plusHours = 3;
                break;
            case "cst":
                plusHours = 2;
                break;
            case "mst":
                plusHours = 1;
                break;
            default:
                timezone = "pst";
                break;
        }
        String output = "```\nGuerilla for today in " + timezone.toUpperCase() + "\n   ";
        ArrayList<Integer> colWidths = new ArrayList<Integer>();
        for (int i = 0; i < dungeons.size(); i++) {
            output += dungeons.get(i) + "|";
            colWidths.add(dungeons.get(i).length());
        }
        String DATE_FORMAT = "h:mm a";
        output += "\nA: ";
        for (int i = 0; i < a.size(); i++) {
            output += centerString(a.get(i).plusHours(plusHours).format(DateTimeFormatter.ofPattern(DATE_FORMAT)), colWidths.get(i)) + "|";
        }
        output += "\nB: ";
        for (int i = 0; i < a.size(); i++) {
            output += centerString(b.get(i).plusHours(plusHours).format(DateTimeFormatter.ofPattern(DATE_FORMAT)), colWidths.get(i)) + "|";
        }
        output += "\nC: ";
        for (int i = 0; i < a.size(); i++) {
            output += centerString(c.get(i).plusHours(plusHours).format(DateTimeFormatter.ofPattern(DATE_FORMAT)), colWidths.get(i)) + "|";
        }
        output += "\nD: ";
        for (int i = 0; i < a.size(); i++) {
            output += centerString(d.get(i).plusHours(plusHours).format(DateTimeFormatter.ofPattern(DATE_FORMAT)), colWidths.get(i)) + "|";
        }
        output += "\nE: ";
        for (int i = 0; i < a.size(); i++) {
            output += centerString(e.get(i).plusHours(plusHours).format(DateTimeFormatter.ofPattern(DATE_FORMAT)), colWidths.get(i)) + "|";
        }
        return output + "\n```";
    }

    public String forGroup(String group, String timezone) {
        ArrayList<LocalTime> groupTimes;
        switch (group.toLowerCase()) {
            case "a":
                groupTimes = a;
                break;
            case "b":
                groupTimes = b;
                break;
            case "c":
                groupTimes = c;
                break;
            case "d":
                groupTimes = d;
                break;
            case "e":
                groupTimes = e;
                break;
            default:
                return "";
        }
        int plusHours = 0;
        switch (timezone) {
            case "est":
                plusHours = 3;
                break;
            case "cst":
                plusHours = 2;
                break;
            case "mst":
                plusHours = 1;
                break;
        }
        String output = "```\nGuerilla for today in " + timezone.toUpperCase() + " for Group " + group.toUpperCase() + "\n   ";
        ArrayList<Integer> colWidths = new ArrayList<Integer>();
        String DATE_FORMAT = "h:mm a";
        for (int i = 0; i < dungeons.size(); i++) {
            output += dungeons.get(i) + "|";
            colWidths.add(dungeons.get(i).length());
        }
        output += "\n" + group.toUpperCase() + ": ";
        for (int i = 0; i < groupTimes.size(); i++) {
            output += centerString(groupTimes.get(i).plusHours(plusHours).format(DateTimeFormatter.ofPattern(DATE_FORMAT)), colWidths.get(i)) + "|";
        }
        return output + "\n```";
    }

    public void writeOut(String outputFolder) throws FileNotFoundException, IOException {
        if (outputFolder.charAt(outputFolder.length() - 1) != '/') {
            outputFolder += "/";
        }
        LocalDate ld = LocalDate.now();
        File f = new File(outputFolder + "guerilla-" + ld.getYear() + "-" + ld.getMonthValue() + "-" + ld.getDayOfMonth() + ".ser");
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        FileOutputStream fileOut = new FileOutputStream(f);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(this);
        out.close();
        fileOut.close();
    }

    public static Guerilla readIn(String inputFile) throws ClassNotFoundException, FileNotFoundException, IOException {
        Guerilla g = null;
        File f = new File(inputFile);
        FileInputStream fileIn = new FileInputStream(f);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        g = (Guerilla) in.readObject();
        in.close();
        fileIn.close();
        return g;
    }

    public String centerString(String str, int width) {
        if (str.length() >= width)
            return str;
        String formatted = str;
        double toAdd = width - str.length();
        double addFr = Math.floor(toAdd / 2);
        double addBa = Math.ceil(toAdd / 2);
        for (int i = 0; i < addFr; i++) {
            formatted = " " + formatted;
        }
        for (int i = 0; i < addBa; i++) {
            formatted += " ";
        }
        return formatted;
    }

    public String toString() {
        return dungeons + "\n" + a + "\n" + b + "\n" + c + "\n" + d + "\n" + e;
    }
}
