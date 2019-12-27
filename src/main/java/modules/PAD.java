package modules;

import base.Command;
import base.EschaUtil;
import base.Module;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import pad.data.PADData;
import pad.data.structure.card.*;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

public class PAD extends Module {
    private TreeMap<String, String> abbrMon = new TreeMap<>();
    private TreeMap<String, String> abbrDun = new TreeMap<>();
    private int maxMonNum = 5636;

    private String tableName = "pad";
    private String col1 = "field";
    private String col2 = "value";
    private String[] tableCols = {col1, col2};

    private String guerillasField = "guerillas";

    private String emoteServerFile = "modules/PAD/emoteserver.txt";
    private ArrayList<Guild> emoteServers = new ArrayList<>();
    private boolean useEmotes = false;

    private Pattern p1 = Pattern.compile("^&(buttoncalc|bc) (\\d+)\\s*(\\d+)?\\s*(\\d+)?\\s*(\\d+)?\\s*([TtYyFfNn])?\\s*(\\d+)?\\s*(\\d+)?\\s*$");
    private Pattern p2 = Pattern.compile("\\d+");
    private Pattern p3 = Pattern.compile(" [TtYyFfNn]");

    private boolean threadRunning = false;
    private boolean updatingDB = false;

    private ReactionEmoji left_arrow = ReactionEmoji.unicode("⬅️");
    private ReactionEmoji right_arrow = ReactionEmoji.unicode("➡️");
    private ReactionEmoji regional_indicator_n = ReactionEmoji.unicode("🇳");
    private ReactionEmoji regional_indicator_j = ReactionEmoji.unicode("🇯");
    private ReactionEmoji x = ReactionEmoji.unicode("❌");
    private ReactionEmoji left_triangle = ReactionEmoji.unicode("◀️");
    private ReactionEmoji right_triangle = ReactionEmoji.unicode("▶️");

    public PAD(DiscordClient client) {
        super(client, "&");
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

        // Load Emote Servers
        client.getEventDispatcher().on(ReadyEvent.class).flatMap(event -> {
            File f = new File(emoteServerFile);
            try {
                Scanner s = new Scanner(f);
                String[] line = s.nextLine().split(";");
                for (int i = 0; i < line.length; i++) {
                    Guild g = client.getGuildById(Snowflake.of(Long.parseLong(line[i]))).block();
                    if (g != null)
                        emoteServers.add(g);
                }

                if (emoteServers.size() > 0)
                    useEmotes = true;

                s.close();

                AwakeningEmoji.loadEmojis(emoteServers);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return Mono.empty();
        }).subscribe();

        client.getEventDispatcher().on(GuildCreateEvent.class).flatMap(event -> {
            Guild guild = event.getGuild();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            if (!driver.tableExists(tableName)) {
                driver.createTable(tableName, tableCols, new String[]{"string", "string"}, false);
            }
            driver.close();
            return Mono.empty();
        }).subscribe();

        client.getEventDispatcher().on(ReactionAddEvent.class).flatMap(event -> {
            Guild guild = event.getGuild().block();
            Message message = event.getMessage().block();
            MessageChannel channel = message.getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel) && message.getAuthor().get().getId().asLong() == client.getSelfId().get().asLong()) {
                List<Reaction> reactions = Arrays.asList(message.getReactions().toArray(new Reaction[0]));
                ReactionEmoji eventEmoji = event.getEmoji();
                List<User> users = message.getReactors(eventEmoji).collectList().block();

                if (users.contains(client.getSelf().block()) && users.size() >= 2) {
                    if (eventEmoji.equals(x)) {
                        return message.delete();
                    } else {
                        Embed embed = message.getEmbeds().get(0);
                        if (!embed.getTitle().isPresent()) return Mono.empty();
                        String title = embed.getTitle().get();
                        if (title.startsWith("Series: \"")) {
                            String footer = embed.getFooter().get().getText();
                            String series = title.split("\"")[1];

                            String region = "NA";
                            if (footer.startsWith("JP")) region = "JP";

                            int currPage = Integer.parseInt(footer.split(" ")[1].split("\\/")[0]);
                            int maxPage = Integer.parseInt(footer.split(" ")[1].split("\\/")[1]);

                            ArrayList<Monster> monsters = PADData.getSeries(series, region);

                            final String fRegion = region;
                            boolean remove = false;
                            if (eventEmoji.equals(left_triangle)) {
                                if (currPage > 1)
                                    message.edit(messageEditSpec -> messageEditSpec.setEmbed(e -> seriesEmbedSpec(e, monsters, series, fRegion, currPage - 1))).subscribe();
                                remove = true;
                            } else if (eventEmoji.equals(right_triangle)) {
                                if (currPage < maxPage)
                                    message.edit(messageEditSpec -> messageEditSpec.setEmbed(e -> seriesEmbedSpec(e, monsters, series, fRegion, currPage + 1))).subscribe();
                                remove = true;
                            }
                            if (remove)
                                users.forEach(u -> {
                                    if (u.getId().asLong() != client.getSelfId().get().asLong())
                                        message.removeReaction(eventEmoji, u.asMember(guild.getId()).block().getId()).subscribe();
                                });
                        } else {
                            int number = Integer.parseInt(embed.getTitle().get().split("\\.")[1].split(" ")[0]);
                            List<Embed.Field> fields = embed.getFields();
                            String evos = "";
                            for (int i = 0; i < fields.size(); i++) {
                                if (fields.get(i).getName().equals("Other Evos"))
                                    evos = fields.get(i).getValue();
                            }

                            String[] evosArray = {};
                            int prevIndex = -1;
                            if (evos.length() > 0) {
                                evosArray = evos.split(", ");
                                for (int i = 0; i < evosArray.length; i++) {
                                    int current = Integer.parseInt(evosArray[i]);
                                    if (number > current) prevIndex = i;
                                }
                            }

                            String region = "NA";
                            for (int i = 0; i < reactions.size(); i++) {
                                if (reactions.get(i).getEmoji().asUnicodeEmoji().get().getRaw().equals("\uD83C\uDDF3"))
                                    region = "JP";
                            }

                            final String fRegion = region;
                            boolean remove = false;
                            if (eventEmoji.equals(left_arrow)) {
                                if (prevIndex != -1) {
                                    Monster m_dec = PADData.getMonster(evosArray[prevIndex], region);
                                    if (m_dec != null)
                                        message.edit(messageEditSpec -> messageEditSpec.setEmbed(e -> infoEmbedSpec(e, m_dec, m_dec.getNo() + "", fRegion))).subscribe();
                                }
                                remove = true;
                            } else if (eventEmoji.equals(right_arrow)) {
                                if (prevIndex + 1 < evosArray.length) {
                                    Monster m_inc = PADData.getMonster(evosArray[prevIndex + 1], region);
                                    if (m_inc != null)
                                        message.edit(messageEditSpec -> messageEditSpec.setEmbed(e -> infoEmbedSpec(e, m_inc, m_inc.getNo() + "", fRegion))).subscribe();
                                }
                                remove = true;
                            } else if (eventEmoji.equals(regional_indicator_n)) {
                                Monster m_na = PADData.getMonster(number + "", "NA");
                                if (m_na != null) {
                                    message.edit(messageEditSpec -> messageEditSpec.setEmbed(e -> infoEmbedSpec(e, m_na, number + "", "NA"))).subscribe();
                                    message.removeAllReactions().subscribe();
                                    addMonsterEmbedReactions(message, "NA").subscribe();
                                }
                                remove = true;
                            } else if (eventEmoji.equals(regional_indicator_j)) {
                                Monster m_jp = PADData.getMonster(number + "", "JP");
                                if (m_jp != null) {
                                    message.edit(messageEditSpec -> messageEditSpec.setEmbed(e -> infoEmbedSpec(e, m_jp, number + "", "JP"))).subscribe();
                                    message.removeAllReactions().subscribe();
                                    addMonsterEmbedReactions(message, "JP").subscribe();
                                }
                                remove = true;
                            }
                            if (remove)
                                users.forEach(u -> {
                                    if (u.getId().asLong() != client.getSelfId().get().asLong())
                                        message.removeReaction(eventEmoji, u.asMember(guild.getId()).block().getId()).subscribe();
                                });
                        }
                    }
                }
            }
            return Mono.empty();
        }).subscribe();
    }

    @Override
    protected Map<String, Command> makeCommands() {
        Map<String, Command> commands = new HashMap<>();

        Command update = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (!updatingDB) {
                    updatingDB = true;
                    EschaUtil.sendMessage(event, "Updating DB. Might take a while.").subscribe();
                    new Thread(() -> {
                        PADData.updateMonsters();
                        EschaUtil.sendMessage(channel, "DB updated.").subscribe();
                        updatingDB = false;
                    }).start();
                    return Mono.empty();
                } else return EschaUtil.sendMessage(channel, "Already updating, please wait.");
            }
            return Mono.empty();
        };

        Command info = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String cmd = event.getMessage().getContent().get().split(" ")[0].replace(prefix, "");
                String[] args = EschaUtil.getArgs(event);
                String query = EschaUtil.getArgsConcat(event);
                String region = "NA";
                if (cmd.equalsIgnoreCase("infojp") || cmd.equalsIgnoreCase("ij"))
                    region = "JP";

                Monster monster = null;
                if (args.length == 0) {
                    while (monster == null)
                        monster = PADData.getMonster(new Random().nextInt(maxMonNum) + 1 + "", region);
                } else {
                    monster = PADData.getMonster(query, region);
                    if (region.equals("NA") && monster == null) {
                        monster = PADData.getMonster(query, "JP");
                        region = "JP";
                    }
                }

                if (monster != null) {
                    Monster m = monster;
                    String fRegion = region;
                    return channel.createMessage(mSpec -> mSpec.setEmbed(e -> infoEmbedSpec(e, m, query, fRegion)))
                            .flatMap(message -> addMonsterEmbedReactions(message, fRegion));
                } else return EschaUtil.sendMessage(event, "Monster not found.");
            }
            return Mono.empty();
        };

        Command pic = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String cmd = event.getMessage().getContent().get().split(" ")[0].replace(prefix, "");
                String args[] = EschaUtil.getArgs(event);
                String argsconcat = EschaUtil.getArgsConcat(event);

                String region = "NA";
                if (cmd.equalsIgnoreCase("picjp")) region = "JP";
                if (args.length > 1 && args[0].contains("sheen")) {
                    int roll = new Random().nextInt(100);
                    System.out.println(roll);
                    if (roll >= 95) {
                        return EschaUtil.sendMessage(event, "http://i.imgur.com/oicGMFu.png");
                    }
                }
                String url;
                if (args.length == 0)
                    url = PADData.getFullPictureURL((new Random().nextInt(maxMonNum) + 1) + "", region);
                else {
                    url = PADData.getFullPictureURL(argsconcat, region);
                    if (url == null && region.equalsIgnoreCase("NA")) url = PADData.getFullPictureURL(argsconcat, "JP");
                    else if (url == null && region.equalsIgnoreCase("JP"))
                        url = PADData.getFullPictureURL(argsconcat, "NA");
                }
                if (url != null) {
                    String fUrl = url;
                    return channel.createMessage(mSpec -> mSpec.setEmbed(e -> {
                        e.setImage(fUrl);
                    })).then();
                } else return EschaUtil.sendMessage(event, "Nothing was found.");
            }
            return Mono.empty();
        };

        Command series = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String cmd = event.getMessage().getContent().get().split(" ")[0].replace(prefix, "");
                String argsconcat = EschaUtil.getArgsConcat(event);
                String region = "NA";
                if (argsconcat.length() <= 0) return Mono.empty();

                if (cmd.equalsIgnoreCase("sj") || cmd.equalsIgnoreCase("seriesjp")) region = "JP";
                ArrayList<Monster> monsters = PADData.getSeries(argsconcat, region);
                if (region.equalsIgnoreCase("NA") && monsters.size() == 0)
                    monsters = PADData.getSeries(argsconcat, "JP");
                else if (region.equalsIgnoreCase("JP") && monsters.size() == 0)
                    monsters = PADData.getSeries(argsconcat, "NA");
                if (monsters.size() == 0)
                    return EschaUtil.sendMessage(event, "Invalid series.");
                else {
                    ArrayList<Monster> fMons = monsters;
                    String fRegion = region;
                    return channel.createMessage(mSpec -> mSpec.setEmbed(e -> seriesEmbedSpec(e, fMons, argsconcat, fRegion, 1)))
                            .flatMap(message -> addSeriesEmbedReactions(message));
                }
            }
            return Mono.empty();
        };

        commands.put(prefix + "update", update);
        commands.put(prefix + "info", info);
        commands.put(prefix + "i", info);
        commands.put(prefix + "infojp", info);
        commands.put(prefix + "ij", info);
        commands.put(prefix + "pic", pic);
        commands.put(prefix + "picjp", pic);
        commands.put(prefix + "p", pic);
        commands.put(prefix + "pj", pic);
        commands.put(prefix + "series", series);
        commands.put(prefix + "seriesjp", series);
        commands.put(prefix + "s", series);
        commands.put(prefix + "sj", series);

        return commands;
    }

    @Override
    public String getName() {
        return "PAD";
    }

    private Mono<Void> addMonsterEmbedReactions(Message message, String region) {
        return message.addReaction(left_arrow)
                .then(message.addReaction(right_arrow))
                .then((region.equals("JP") ? message.addReaction(regional_indicator_n) : message.addReaction(regional_indicator_j)))
                .then(message.addReaction(x))
                .then();
    }

    private Mono<Void> addSeriesEmbedReactions(Message message) {
        return message.addReaction(left_triangle)
                .then(message.addReaction(right_triangle))
                .then(message.addReaction(x))
                .then();
    }

    private void infoEmbedSpec(EmbedCreateSpec e, Monster m, String query, String region) {
        Color c = Color.GRAY;
        switch (Attribute.fromID(m.getAttribute_1_id())) {
            case FIRE:
                c = new Color(0xff744b);
                break;
            case WATER:
                c = new Color(0x40ffff);
                break;
            case WOOD:
                c = new Color(0x4cd962);
                break;
            case LIGHT:
                c = new Color(0xf2e74c);
                break;
            case DARK:
                c = new Color(0xcc54c2);
                break;
        }
        String desc = "";
        int[] awakenings = m.getAwakenings();
        for (int i = 0; i < awakenings.length; i++) {
            if (useEmotes) {
                desc += AwakeningEmoji.getEmoji(awakenings[i]).asFormat();
            } else {
                desc += Awakening.values()[awakenings[i]].getShortName();
                if (i != awakenings.length - 1) {
                    desc += "║";
                }
            }
        }
        if (desc.length() == 0)
            desc += "No Awakenings.";

        int[] superAwakenings = m.getSupers();
        if (superAwakenings.length > 0)
            desc += "\n";
        for (int i = 0; i < superAwakenings.length; i++) {
            if (useEmotes) {
                desc += AwakeningEmoji.getEmoji(superAwakenings[i]).asFormat();
            } else {
                desc += Awakening.values()[superAwakenings[i]].getShortName();
                if (i != superAwakenings.length - 1) {
                    desc += "║";
                }
            }
        }

        TreeSet<Type> validKillers = m.getValidKillerLatents();
        if (validKillers.size() > 0) {
            desc += "\nKillers: ";
            if (validKillers.size() == 8)
                desc += "Any";
            else {
                StringBuilder sb = new StringBuilder();
                for (Type t : m.getValidKillerLatents()) {
                    sb.append(t.getName()).append(" ");
                }
                desc += sb.toString();
            }
        }
        e.setDescription("**" + desc + "**");

        Type type = Type.fromID(m.getType_1_id());
        Type type2 = Type.fromID(m.getType_2_id());
        Type type3 = Type.fromID(m.getType_3_id());
        String inheritable = m.getInheritable() == 1 ? "Yes" : "No";
        String typing = type.getName() + (type2 == Type.NONE ? "" : "/" + type2.getName()) + (type3 == Type.NONE ? "" : "/" + type3.getName()) + "\n";
        String mInfo = String.format("**Rarity** %-5d" + "\n**Cost**   %-5d" + "\n**MP**     %-5d" + "\n**Inheritable** %-5s", m.getRarity(), m.getCost(), m.getMp(), inheritable);
        e.addField(typing, mInfo, true);

        int hp = m.getHp_max();
        int atk = m.getAtk_max();
        int rcv = m.getRcv_max();
        int weighted = m.getWeighted();

        if (m.getLb_mult() == 0)
            e.addField("**Weighted** " + weighted, String.format("**HP**    %-4d\n**ATK** %-4d\n**RCV** %-4d\n**XP** %-5s", hp, atk, rcv, m.getExp()), true);
        else {
            int lbhp = m.getLB_hp();
            int lbatk = m.getLB_atk();
            int lbrcv = m.getLB_rcv();
            int lbweighted = m.getLB_weighted();
            e.addField("**Weighted** " + weighted + " | " + "**LB** " + lbweighted,
                    String.format("**HP**    %-4d | %-4d\n**ATK** %-4d | %-4d\n**RCV** %-4d | %-4d\n**XP** %-5s", hp, lbhp, atk, lbatk, rcv, lbrcv, m.getExp()), true);
        }


        ActiveSkill active = m.getActive();
        String activeName = "Active: " + (active == null ? "" : active.getName() + " (" + active.getTurn_max() + "->" + active.getTurn_min() + ")");
        e.addField(activeName, active == null ? "None." : active.getDesc(), false);

        LeaderSkill leader = m.getLeader();
        String leaderName = "Leader: " + (leader == null ? "" : leader.getName());
        e.addField(leaderName, leader == null ? "None." : leader.getDesc().replace("^p", "").replace(";", ""), false);

        int[] evos = m.getEvolutions();
        TreeSet<Integer> otherEvoes = new TreeSet<>(Integer::compareTo);
        for (int i = 0; i < evos.length; i++) {
            if (evos[i] != m.getNo())
                otherEvoes.add(evos[i]);
        }

        String otherEvos = otherEvoes.toString().replace("[", "").replace("]", "");

        ArrayList<Monster> similarNames = query.length() > 0 ? PADData.getAllMonsters(query, region) : new ArrayList<>();
        String similar = "";
        if (similarNames.size() <= 10) {
            for (int i = 0; i < similarNames.size(); i++) {
                int currentID = similarNames.get(i).getNo();
                if (currentID != m.getNo()) {
                    boolean contains = false;
                    for (int j = 0; j < evos.length; j++) {
                        if (evos[j] == currentID) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains)
                        similar += similarNames.get(i).getNo() + ", ";
                }
            }
            if (similar.contains(",")) {
                similar = similar.substring(0, similar.lastIndexOf(","));
            }
        } else {
            similar += "Too many to show.";
        }

        if (otherEvos.length() > 0) e.addField("Other Evos", otherEvos, true);
        if (similar.length() > 0) e.addField("Similar Names", similar, true);

        e.setThumbnail(PADData.getPortraitPictureURL(m.getNo() + "", region));
        e.setTitle("No." + m.getNo() + " " + m.getName());
        e.setUrl("http://puzzledragonx.com/en/monster.asp?n=" + m.getNo());
        e.setColor(c);

        String series = m.getSeries();
        if (!series.equalsIgnoreCase("unsorted"))
            e.setFooter("Series: " + series, "");
    }

    private void seriesEmbedSpec(EmbedCreateSpec e, ArrayList<Monster> monsters, String query, String region, int page) {
        e.setTitle("Series: \"" + query + "\"");

        int perPage = 10;

        StringBuilder s = new StringBuilder();
        for (int i = perPage * (page - 1); i < perPage * page; i++) {
            if (i >= monsters.size()) break;
            Monster m = monsters.get(i);
            s
                    .append("**")
                    .append(m.getNo())
                    .append("**: ")
                    .append(m.getName())
                    .append("\n");
        }
        e.setDescription(s.toString());
        e.setFooter(region.toUpperCase() + " " + page + "/" + (int) Math.ceil(monsters.size() / (double) perPage), "");
    }


//    @EventSubscriber
//    public void onMessage(MessageReceivedEvent event) {
//        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
//            IGuild guild = event.getMessage().getGuild();
//            IChannel channel = event.getMessage().getChannel();
//            if (PermissionsListener.isModuleOn(guild, PADModule.name)
//                    && PermissionsListener.canModuleInChannel(guild, PADModule.name, channel)) {
//                if (event.getMessage().getContent().startsWith(prefix)) {
//                    String msg = event.getMessage().getContent().toLowerCase().trim();
//                    String[] split = msg.split(" ");
//                    String cmd = split[0].replace(prefix, "");
//                    IUser user = event.getMessage().getAuthor();
//

//                    } else if (cmd.equals("guerilla") || cmd.equals("g")) {
//                        Guerilla g = paddata.getTodayGuerilla();
//                        if (split.length == 1) {
//                            try {
//                                ByteArrayOutputStream os = new ByteArrayOutputStream();
//                                ImageIO.write(paddata.guerillaToImage(g, "pst"), "png", os);
//                                InputStream is = new ByteArrayInputStream(os.toByteArray());
//                                channel.sendFile("", false, is, "img.png");
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        } else if (split.length == 2) {
//                            try {
//                                ByteArrayOutputStream os = new ByteArrayOutputStream();
//                                ImageIO.write(paddata.guerillaToImage(g, split[1].trim()), "png", os);
//                                InputStream is = new ByteArrayInputStream(os.toByteArray());
//                                channel.sendFile("", false, is, "img.png");
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    } else if (cmd.equals("ga") || cmd.equals("guerillaall")) {
//                        event.getMessage().delete();
//                        outputAllGuerillaImgs(channel);
//                    } else if (cmd.equals("forceupdateguerilla") || cmd.equals("fug") || cmd.equals("fgu")) {
//                        paddata.updateGuerillas();
//                        Sender.sendMessage(channel, "Guerillas have been updated for today.");
//                    } else if (cmd.equals("pic") || cmd.equals("picjp")) {


//                    } else if (cmd.equals("addnickname") || cmd.equals("an")) {
//
//                    } else if (cmd.equals("deletenickname") || cmd.equals("dn")) {
//
//                    } else if (cmd.equals("addguerillachannel") || cmd.equals("agc")) {
//                        if (userHasPerm(user, guild, Permissions.MANAGE_SERVER) || userHasPerm(user, guild, Permissions.MANAGE_CHANNEL)
//                                || userHasPerm(user, guild, Permissions.MANAGE_MESSAGES)) {
//                            if (split.length > 1) {
//                                Perms perms = PermissionsListener.getPermissionDB(guild);
//
//                                ArrayList<IChannel> channelsAdded = new ArrayList<>();
//                                for (int i = 1; i < split.length; i++) {
//                                    ArrayList<String> chans = new ArrayList<>(Arrays.asList(perms.getPerms(tableName, col1, guerillasField, col2).split(";")));
//                                    IChannel aChannel = null;
//                                    if (split[i].contains("#")) {
//                                        aChannel = guild.getChannelByID(Long.parseLong(split[i].replace("<#", "").replace(">", "")));
//                                        if (aChannel != null) {
//                                            if (!chans.contains(aChannel.getStringID())) {
//                                                channelsAdded.add(aChannel);
//                                                perms.addPerms(tableName, col1, guerillasField, col2, aChannel.getLongID() + "");
//                                            }
//                                        }
//                                    }
//                                }
//                                String output = "The guerillas will now additionally be posted in: ";
//                                if (channelsAdded.size() > 0) {
//                                    for (IChannel c : channelsAdded) {
//                                        output += c.mention() + " ";
//                                    }
//                                } else
//                                    output = "No channels were added, make sure you mention the channel(s) with `#`";
//                                Sender.sendMessage(channel, output);
//
//                                perms.close();
//                            }
//                        }
//                    } else if (cmd.equals("deleteguerillachannel") || cmd.equals("dgc")) {
//                        if (userHasPerm(user, guild, Permissions.MANAGE_SERVER) || userHasPerm(user, guild, Permissions.MANAGE_CHANNEL)
//                                || userHasPerm(user, guild, Permissions.MANAGE_MESSAGES)) {
//                            if (split.length > 1) {
//                                Perms perms = PermissionsListener.getPermissionDB(guild);
//
//                                ArrayList<IChannel> channelsToKeep = new ArrayList<>();
//                                ArrayList<IChannel> channelsToDel = new ArrayList<>();
//                                for (int i = 1; i < split.length; i++) {
//                                    IChannel aChannel = null;
//                                    if (split[i].contains("#")) {
//                                        aChannel = guild.getChannelByID(Long.parseLong(split[i].replace("<#", "").replace(">", "")));
//                                        if (aChannel != null) {
//                                            channelsToDel.add(aChannel);
//                                        }
//                                    }
//                                }
//
//                                ArrayList<String> channels = new ArrayList<>(Arrays.asList(perms.getPerms(tableName, col1, guerillasField, col2).split(";")));
//                                perms.setPerms(tableName, col1, guerillasField, col2, "");
//                                for (String s : channels) {
//                                    IChannel ch = null;
//                                    ch = guild.getChannelByID(Long.parseLong(s));
//                                    if (ch != null) {
//                                        if (!channelsToDel.contains(ch)) {
//                                            perms.addPerms(tableName, col1, guerillasField, col2, s);
//                                        }
//                                    }
//                                }
//
//                                String output = "The following guerilla channels were removed: ";
//                                if (channelsToDel.size() > 0) {
//                                    for (IChannel c : channelsToDel) {
//                                        output += c.mention() + " ";
//                                    }
//                                } else
//                                    output = "No channels were removed, make sure you mention the channel(s) with `#`";
//
//                                Sender.sendMessage(channel, output);
//
//                                perms.close();
//                            }
//                        }
//                    } else if (cmd.equals("guerillachannels") || cmd.equals("gc")) {
//                        if (userHasPerm(user, guild, Permissions.MANAGE_SERVER) || userHasPerm(user, guild, Permissions.MANAGE_CHANNEL)
//                                || userHasPerm(user, guild, Permissions.MANAGE_MESSAGES)) {
//                            Perms perms = PermissionsListener.getPermissionDB(guild);
//                            String[] channels = perms.getPerms(tableName, col1, guerillasField, col2).split(";");
//                            String output = "Guerilla channels are: ";
//                            for (int i = 0; i < channels.length; i++) {
//                                IChannel theChan = guild.getChannelByID(Long.parseLong(channels[i]));
//                                if (theChan != null) {
//                                    output += theChan.mention() + " ";
//                                }
//                            }
//
//                            Sender.sendMessage(channel, output);
//                            perms.close();
//                        }
//                }
//            }
//        }
//
//    }


//
//
//    @EventSubscriber
//    public void startPADThread(GuildCreateEvent event) {
//        if (false) // TODO temp while guerilla_data.json is not being updated.
//            if (!threadRunning) {
//                LocalTime targetTime = LocalTime.of(7, 0);
//                Thread t = new Thread("guerilla") {
//                    @Override
//                    public void run() {
//                        threadRunning = true;
//                        while (true) {
//                            LocalTime current = LocalTime.now();
//                            if (current.equals(targetTime) || current.isAfter(targetTime)) {
//                                List<IGuild> allGuilds = PADModule.client.getGuilds();
//                                for (IGuild guild : allGuilds) {
//                                    Perms perms = PermissionsListener.getPermissionDB(guild);
//                                    ArrayList<String> channels = new ArrayList<>(Arrays.asList(perms.getPerms(tableName, col1, guerillasField, col2).split(";")));
//                                    for (String s : channels) {
//                                        if (s.length() == 0)
//                                            break;
//                                        IChannel channel;
//                                        channel = guild.getChannelByID(Long.parseLong(s));
//                                        if (channel != null && PermissionsListener.isModuleOn(guild, PADModule.name)
//                                                && PermissionsListener.canModuleInChannel(guild, PADModule.name, channel)) {
//                                            LocalDateTime today = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
//                                            IMessage lastMessage = null;
//                                            for (IMessage m : channel.getMessageHistory(50)) {
//                                                if (m.getAuthor().getLongID() == PADModule.client.getOurUser().getLongID()) {
//                                                    lastMessage = m;
//                                                    break;
//                                                }
//                                            }
//                                            if (lastMessage != null) {
//                                                LocalDateTime mDate = LocalDateTime.ofInstant(lastMessage.getTimestamp(), ZoneId.systemDefault());
//                                                if (!(today.getYear() == mDate.getYear() && today.getMonth() == mDate.getMonth() && today.getDayOfMonth() == mDate.getDayOfMonth()))
//                                                    outputAllGuerillaImgs(channel);
//                                            } else
//                                                outputAllGuerillaImgs(channel);
//                                            try {
//                                                sleep(1000 * 60 * 30);//1000 millis = 1s; "Roughly" 30min sleep
//                                            } catch (InterruptedException e) {
//                                                e.printStackTrace();
//                                            }
//                                        }
//                                    }
//                                    perms.close();
//                                }
//                            }
//                        }
//                    }
//                };
//                t.start();
//            }
//    }
//


//    public void outputAllGuerillaImgs(IChannel channel) {
//        Guerilla g = paddata.getTodayGuerilla();
//
//        BufferedImage pstImg = paddata.guerillaToImage(g, "pst");
//        BufferedImage mstImg = paddata.guerillaToImage(g, "mst");
//        BufferedImage cstImg = paddata.guerillaToImage(g, "cst");
//        BufferedImage estImg = paddata.guerillaToImage(g, "est");
//        ArrayList<BufferedImage> images = new ArrayList<>();
//        images.add(pstImg);
//        images.add(mstImg);
//        images.add(cstImg);
//        images.add(estImg);
//
//        for (BufferedImage bi : images) {
//            try {
//                ByteArrayOutputStream os = new ByteArrayOutputStream();
//                ImageIO.write(bi, "png", os);
//                InputStream is = new ByteArrayInputStream(os.toByteArray());
//                channel.sendFile("", false, is, "img.png");
//            } catch (RateLimitException e) {
//                try {
//                    long delay = e.getRetryDelay();
//                    Thread.sleep(delay);
//                    ByteArrayOutputStream os = new ByteArrayOutputStream();
//                    ImageIO.write(bi, "png", os);
//                    InputStream is = new ByteArrayInputStream(os.toByteArray());
//                    channel.sendFile("", false, is, "img.png");
//                } catch (InterruptedException | IOException e1) {
//                    e1.printStackTrace();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//

}
