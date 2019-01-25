package base;

import modules.Admin.AdminModule;
import modules.CustomCommands.CustomCommandsModule;
import modules.Games.GamesModule;
import modules.JoinLeave.JoinLeaveModule;
import modules.Music.MusicModule;
import modules.PAD.PADModule;
import modules.Parrot.ParrotModule;
import modules.Permissions.PermissionsListener;
import modules.Profiles.ProfilesModule;
import modules.Reactions.ReactionsModule;
import modules.Required.RequiredModule;
import modules.Roles.RolesModule;
import modules.Yugioh.YGOModule;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.modules.IModule;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Created by Iggie on 8/14/2016.
 */
public class Eschamali {
    //private String clientID = "214442111720751104";
    private String token = "";
    public static String configFileName = "config.properties";
    private String status = "";
    private String credentials = "";
    public static ArrayList<IModule> modules;
    public static TreeMap<IModule, Boolean> defaultmodules;
    public static IDiscordClient client;
    //    public static String ownerID = "85844964633747456";
    public static ArrayList<Long> ownerIDs = new ArrayList<>();
    public static final LocalDateTime startTime = LocalDateTime.now();

    public Eschamali() {
        readConfig();
        Comparator<IModule> cmpr = Comparator.comparing(IModule::getName);
        defaultmodules = new TreeMap<>(cmpr);
        RolesModule roles = new RolesModule();
        GamesModule games = new GamesModule();
        ParrotModule parrot = new ParrotModule();
        MusicModule music = new MusicModule();
        PADModule pad = new PADModule(this.credentials);
        JoinLeaveModule jl = new JoinLeaveModule();
        AdminModule admin = new AdminModule();
        CustomCommandsModule custom = new CustomCommandsModule();
        ReactionsModule reactions = new ReactionsModule();
        YGOModule ygo = new YGOModule();
        ProfilesModule profiles = new ProfilesModule();
        RequiredModule required = new RequiredModule();

        defaultmodules.put(roles, true);
        defaultmodules.put(games, true);
        defaultmodules.put(parrot, false);
        defaultmodules.put(music, true);
        if (this.credentials.length() != 0)
            defaultmodules.put(pad, true);
        defaultmodules.put(jl, true);
        defaultmodules.put(admin, true);
        defaultmodules.put(custom, true);
        defaultmodules.put(reactions, true);
        defaultmodules.put(ygo, true);
        defaultmodules.put(profiles, true);
        defaultmodules.put(required, false);

        modules = new ArrayList<>();
        modules.add(roles);
        modules.add(games);
        modules.add(parrot);
        modules.add(music);
        if (this.credentials.length() != 0)
            modules.add(pad);
        modules.add(jl);
        modules.add(admin);
        modules.add(custom);
        modules.add(reactions);
        modules.add(ygo);
        modules.add(profiles);
        modules.add(required);
        modules.sort(Comparator.comparing(IModule::getName));

    }

    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(new FileReader(configFileName));
            String[] ownerIDS = props.getProperty("ownerid").split(";");
            String token = props.getProperty("token");
            String status = props.getProperty("status");
            String credentials = props.getProperty("credentials");
            if (token.length() == 0) {
                System.out.println("No token specified, please fill out the token field in config.properties.");
                System.exit(0);
            } else this.token = token;

            if (credentials.length() == 0) {
                System.out.println("No Google Cloud Storage credential file was specified, PAD Module will not be enabled.");
            } else this.credentials = credentials;

            ownerIDs.add(85844964633747456L);//Barkuto's ID
            for (int i = 0; i < ownerIDS.length; i++) {
                if (ownerIDS[i].length() > 0)
                    ownerIDs.add(Long.parseLong(ownerIDS[i]));
            }

            this.status = status;
        } catch (IOException e) {
            props.setProperty("ownerid", "");
            props.setProperty("token", "");
            props.setProperty("status", "");
            props.setProperty("credentials", "");
            try {
                props.store(new FileWriter(configFileName), "Separate owner IDs using semi-colons(;). Make a Bot user and get a bot token at https://discordapp.com/developers/applications/me");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.out.println("No config file was found, please fill out the newly created config.properties file.");
            System.exit(0);
        }
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        if (status.length() > 0) {
            client.changePresence(StatusType.ONLINE, ActivityType.PLAYING, status);
        }
    }

    public void run() {
        client = new ClientBuilder().withToken(token).login();
        client.getDispatcher().registerListener(this);
        client.getDispatcher().registerListener(new GeneralListener());
        client.getDispatcher().registerListener(new OwnerListener());
        client.getDispatcher().registerListener(new PermissionsListener());
        for (IModule m : modules) {
            m.enable(client);
        }
    }

    public static void main(String[] args) {
        Eschamali e = new Eschamali();
        e.run();
    }
}
