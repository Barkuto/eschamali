package base;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import modules.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.TreeMap;

public class Eschamali {
    // private String clientID = "214442111720751104";
    private String token = "";
    public static String configFileName = "config.properties";
    private String status = "";
    private String credentials = "";
    public static ArrayList<Module> modules;
    public static TreeMap<Module, Boolean> defaultmodules;
    public static DiscordClient client;
    //    public static String ownerID = "85844964633747456";
    public static ArrayList<Long> ownerIDs = new ArrayList<>();
    public static final LocalDateTime startTime = LocalDateTime.now();

    public Eschamali() {
        readConfig();
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

            ownerIDs.add(85844964633747456L);// Barkuto's ID
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

    public void run() {
        client = new DiscordClientBuilder(token).build();

        JoinLeave joinleave = new JoinLeave(client);
        Roles roles = new Roles(client);
        Admin admin = new Admin(client);
        PAD pad = new PAD(client, this.credentials);

        Comparator<Module> cmpr = Comparator.comparing(Module::getName);
        defaultmodules = new TreeMap<>(cmpr);
        defaultmodules.put(joinleave, true);
        defaultmodules.put(roles, true);
        defaultmodules.put(admin, true);
        if (this.credentials.length() != 0)
            defaultmodules.put(pad, true);

        modules = new ArrayList<>();
        modules.add(joinleave);
        modules.add(roles);
        modules.add(admin);
        if (this.credentials.length() != 0)
            modules.add(pad);
        modules.sort(Comparator.comparing(Module::getName));

        client.getEventDispatcher().on(ReadyEvent.class)
                .flatMap(event -> client.updatePresence(Presence.online(Activity.playing(this.status))))
                .subscribe();

        new General(client);
        new Owner(client);
        new ChannelPerms(client);

        client.login().block();
    }

    public static void main(String[] args) {
        new Eschamali().run();
    }
}
