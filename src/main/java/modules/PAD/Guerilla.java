package modules.PAD;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Iggie on 9/13/2016.
 */
public class Guerilla implements Serializable {
    private ArrayList<String> dungeons;
    private ArrayList<String> dungeonImgLinks;
    private ArrayList<LocalTime> a;
    private ArrayList<LocalTime> b;
    private ArrayList<LocalTime> c;
    private ArrayList<LocalTime> d;
    private ArrayList<LocalTime> e;

    public Guerilla(ArrayList<String> dungeons, ArrayList<String> dungeonImgLinks, ArrayList<LocalTime> a, ArrayList<LocalTime> b, ArrayList<LocalTime> c, ArrayList<LocalTime> d, ArrayList<LocalTime> e) {
        this.dungeons = dungeons;
        this.dungeonImgLinks = dungeonImgLinks;
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
        ArrayList<Integer> colWidths = new ArrayList<>();
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
        ArrayList<Integer> colWidths = new ArrayList<>();
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

    public void writeOut(String outputFolder) throws IOException {
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

    public static Guerilla readIn(String inputFile) throws ClassNotFoundException, IOException {
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

    public BufferedImage getTodayGuerillaImage(String timezone) {
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

        ArrayList<BufferedImage> dungeonImgs = new ArrayList<>();
        for (int i = 0; i < dungeonImgLinks.size(); i++) {
            try {
                dungeonImgs.add(ImageIO.read(new URL(dungeonImgLinks.get(i))));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        int blankWidth = 40;
        int cellHeight = 20;

        int width = blankWidth;
        int height = 0;
        int imgType = 5;
        for (BufferedImage img : dungeonImgs) {
            width += img.getWidth();
            height = img.getHeight() > height ? height = img.getHeight() : height;
            imgType = img.getType() > imgType ? img.getType() : imgType;
        }
        int biggestHeight = height;
        height += cellHeight * 5;
        BufferedImage concatImg = new BufferedImage(width, height, imgType);
        Graphics2D g = concatImg.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int xPos = blankWidth;
        int yPos = 0;
        g.setColor(Color.BLACK);
        for (int i = 0; i < dungeonImgs.size(); i++) {
            g.drawImage(dungeonImgs.get(i), xPos, yPos, null);
            g.drawLine(xPos, 0, xPos, height);//Vertical Lines
            try {
                xPos += dungeonImgs.get(i + 1).getWidth();
            } catch (IndexOutOfBoundsException ignored) {
            }
        }

        //Horizontal Lines
        int yLinePos = biggestHeight;
        for (int i = 0; i < 5; i++) {
            g.drawLine(0, yLinePos, width, yLinePos);
            try {
                yLinePos += cellHeight;
            } catch (IndexOutOfBoundsException ignored) {
            }
        }

        String fontName = "Arial";

        g.setFont(new Font(fontName, Font.BOLD, cellHeight));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        centerString(g, new Rectangle(0, biggestHeight, blankWidth, cellHeight), "A", g.getFont());
        centerString(g, new Rectangle(0, biggestHeight + cellHeight, blankWidth, cellHeight), "B", g.getFont());
        centerString(g, new Rectangle(0, biggestHeight + (cellHeight * 2), blankWidth, cellHeight), "C", g.getFont());
        centerString(g, new Rectangle(0, biggestHeight + (cellHeight * 3), blankWidth, cellHeight), "D", g.getFont());
        centerString(g, new Rectangle(0, biggestHeight + (cellHeight * 4), blankWidth, cellHeight), "E", g.getFont());

        //Date & Timezone
        g.setFont(new Font(g.getFont().getFontName(), Font.PLAIN, 14));
        LocalDate date = LocalDate.now();
        String month = date.format(DateTimeFormatter.ofPattern("MMM"));
        String day = date.format(DateTimeFormatter.ofPattern("d"));
        String year = date.format(DateTimeFormatter.ofPattern("uuu"));
        int padding = 10;
        centerString(g, new Rectangle(0, 0, blankWidth, biggestHeight / 4), month, g.getFont());
        centerString(g, new Rectangle(0, (cellHeight / 4) + padding, blankWidth, biggestHeight / 4), day, g.getFont());
        centerString(g, new Rectangle(0, (cellHeight / 4 * 2) + padding * 2, blankWidth, biggestHeight / 4), year, g.getFont());
        g.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 16));
        centerString(g, new Rectangle(0, (cellHeight / 4 * 3) + padding * 3, blankWidth, biggestHeight / 4), timezone.toUpperCase(), g.getFont());

        g.setFont(new Font(g.getFont().getFontName(), Font.PLAIN, 12));
        int xTimePos = blankWidth;
        int yTimePos = biggestHeight;
        ArrayList<ArrayList<LocalTime>> times = new ArrayList<>();
        times.add(a);
        times.add(b);
        times.add(c);
        times.add(d);
        times.add(e);
        for (ArrayList<LocalTime> array : times) {
            for (int i = 0; i < array.size(); i++) {
                LocalTime lt = array.get(i).plusHours(plusHours);
                String time = lt.format(DateTimeFormatter.ofPattern("h:mm"));
                String ampm = lt.format(DateTimeFormatter.ofPattern("a"));
                int imgWidth = dungeonImgs.get(i).getWidth();
                centerString(g, new Rectangle(xTimePos, yTimePos, imgWidth, cellHeight), time + " " + ampm, g.getFont());
                xTimePos += imgWidth;
            }
            xTimePos = blankWidth;
            yTimePos += cellHeight;
        }

        g.dispose();

        return concatImg;
    }

    //////////////////////////
    //Data Download Methods//
    ////////////////////////
    public static boolean updateGuerilla(String outputPath) {
        try {
            URL home = new URL("http://puzzledragonx.com/");
            Document document = Jsoup.parse(home, 15000);
            Elements sched = document.select("div#metal1a").select("table").get(0).select("tr");
            ArrayList<LocalTime> a = new ArrayList<>();
            ArrayList<LocalTime> b = new ArrayList<>();
            ArrayList<LocalTime> c = new ArrayList<>();
            ArrayList<LocalTime> d = new ArrayList<>();
            ArrayList<LocalTime> e = new ArrayList<>();
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
            ArrayList<String> dungeons = new ArrayList<>();
            for (int i = 1; i < sched.size(); i += 2) {
                URL url = new URL("http://puzzledragonx.com/" + sched.get(i).select("td").select("a[href]").attr("href"));
                Document doc = Jsoup.parse(url, 150000);
                String dungeon = doc.select("table#tablestat").get(1).select("tr").get(1).text();
                dungeons.add(dungeon);
            }
            ArrayList<BufferedImage> dungeonImgs = new ArrayList<>();
            ArrayList<String> dungeonLinks = new ArrayList<>();
            for (int i = 1; i < sched.size(); i += 2) {
                String html = sched.get(i).select("td").select("a[href]").html();
                html = html.substring(html.indexOf("data-original="));
                html = html.substring(html.indexOf('"') + 1, html.indexOf('"', html.indexOf('"') + 1));
                BufferedImage img = ImageIO.read(new URL("http://puzzledragonx.com/" + html));
                dungeonImgs.add(img);
                dungeonLinks.add("http://puzzledragonx.com/" + html);
            }

            Guerilla g = new Guerilla(dungeons, dungeonLinks, a, b, c, d, e);
//            System.out.println(g);
            g.sortByDungeonChrono();
//            System.out.println(g);
            g.writeOut(outputPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Guerilla getGuerilla(String path, int year, int month, int day) {
        Guerilla g = null;
        try {
            g = Guerilla.readIn(path + "guerilla-" + year + "-" + month + "-" + day + ".ser");
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return g;
    }

    public static Guerilla getTodayGuerilla(String outputPath) {
        Guerilla g;
        LocalDate ld = LocalDate.now();
        g = getGuerilla(outputPath, ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
        if (g == null) {
            updateGuerilla(outputPath);
        } else {
            return g;
        }
        return getGuerilla(outputPath, ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
    }

    /////////////////////////////
    //End Data Download Methods//
    ////////////////////////////

    private void sortByDungeonChrono() {
        ArrayList<LocalTime> dungeonEarliestTimes = new ArrayList<>();
        for (int i = 0; i < dungeons.size(); i++) {
            LocalTime aTime = a.get(i);
            LocalTime bTime = b.get(i);
            LocalTime cTime = c.get(i);
            LocalTime dTime = d.get(i);
            LocalTime eTime = e.get(i);
            LocalTime earliestTime = aTime;

            if (bTime.compareTo(earliestTime) == -1) {
                earliestTime = bTime;
            }
            if (cTime.compareTo(earliestTime) == -1) {
                earliestTime = cTime;
            }
            if (dTime.compareTo(earliestTime) == -1) {
                earliestTime = dTime;
            }
            if (eTime.compareTo(earliestTime) == -1) {
                earliestTime = eTime;
            }
            dungeonEarliestTimes.add(earliestTime);
        }

        ArrayList<IndexTimeObj> objs = new ArrayList<>();
        for (int i = 0; i < dungeonEarliestTimes.size(); i++) {
            objs.add(new IndexTimeObj(i, dungeonEarliestTimes.get(i)));
        }
        objs.sort(Comparator.comparing(o -> o.time));

        ArrayList<Integer> dungeonChrono = new ArrayList<>();
        for (int i = 0; i < objs.size(); i++) {
            dungeonChrono.add(objs.get(i).index);
        }

        ArrayList<String> newDungeons = new ArrayList<>();
        ArrayList<String> newDungeonImgLinks = new ArrayList<>();
        ArrayList<LocalTime> newA = new ArrayList<>();
        ArrayList<LocalTime> newB = new ArrayList<>();
        ArrayList<LocalTime> newC = new ArrayList<>();
        ArrayList<LocalTime> newD = new ArrayList<>();
        ArrayList<LocalTime> newE = new ArrayList<>();
        for (int i = 0; i < dungeonChrono.size(); i++) {
            newDungeons.add(dungeons.get(dungeonChrono.get(i)));
            newDungeonImgLinks.add(dungeonImgLinks.get(dungeonChrono.get(i)));
            newA.add(a.get(dungeonChrono.get(i)));
            newB.add(b.get(dungeonChrono.get(i)));
            newC.add(c.get(dungeonChrono.get(i)));
            newD.add(d.get(dungeonChrono.get(i)));
            newE.add(e.get(dungeonChrono.get(i)));
        }
        this.dungeons = newDungeons;
        this.dungeonImgLinks = newDungeonImgLinks;
        this.a = newA;
        this.b = newB;
        this.c = newC;
        this.d = newD;
        this.e = newE;
    }

//    public ArrayList<String> getDungeons() {
//        return dungeons;
//    }
//
//    public ArrayList<String> getDungeonLinks() {
//        return dungeonImgLinks;
//    }
//
//    public ArrayList<LocalTime> getA() {
//        return a;
//    }
//
//    public ArrayList<LocalTime> getB() {
//        return b;
//    }
//
//    public ArrayList<LocalTime> getC() {
//        return c;
//    }
//
//    public ArrayList<LocalTime> getD() {
//        return d;
//    }
//
//    public ArrayList<LocalTime> getE() {
//        return e;
//    }

    private static LocalTime parseTime(String time) {
        String timeformat = time.toUpperCase();
        if (!timeformat.contains(":")) {
            timeformat = timeformat.replace(" ", ":00 ");
        }
        String DATE_FORMAT = "h:mm a";
        return LocalTime.parse(timeformat, DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    public static void centerString(Graphics g, Rectangle r, String s, Font font) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Rectangle2D r2D = font.getStringBounds(s, frc);
        int rWidth = (int) Math.round(r2D.getWidth());
        int rHeight = (int) Math.round(r2D.getHeight());
        int rX = (int) Math.round(r2D.getX());
        int rY = (int) Math.round(r2D.getY());

        int a = (r.width / 2) - (rWidth / 2) - rX;
        int b = (r.height / 2) - (rHeight / 2) - rY;

        g.setFont(font);
        g.drawString(s, r.x + a, r.y + b);
    }

    public String toString() {
        return "<" + dungeons + "\n" + dungeonImgLinks + "\n" + a + "\n" + b + "\n" + c + "\n" + d + "\n" + e + ">";
    }

    private class IndexTimeObj {
        private int index;
        private LocalTime time;

        public IndexTimeObj(int index, LocalTime time) {
            this.index = index;
            this.time = time;
        }

        public int getIndex() {
            return index;
        }

        public LocalTime getTime() {
            return time;
        }
    }
}
