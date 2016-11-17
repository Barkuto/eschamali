package base;

import modules.Admin.AdminModule;
import modules.CustomCommands.CustomCommandsModule;
import modules.Games.GamesModule;
import modules.JoinLeave.JoinLeaveModule;
import modules.Music.MusicModule;
import modules.PAD.PADModule;
import modules.Parrot.ParrotModule;
import modules.Permissions.PermissionsListener;
import modules.Reactions.ReactionsModule;
import modules.Roles.RolesModule;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by Iggie on 8/14/2016.
 */
public class Eschamali {
    private String clientID = "214442111720751104";
    public static ArrayList<IModule> modules;
    public static TreeMap<IModule, Boolean> defaultmodules;
    public static IDiscordClient client;
    public static String ownerID = "85844964633747456";
    public static final LocalDateTime startTime = LocalDateTime.now();

    public Eschamali() {
        Comparator<IModule> cmpr = new Comparator<IModule>() {
            @Override
            public int compare(IModule o1, IModule o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
        defaultmodules = new TreeMap<IModule, Boolean>(cmpr);
        RolesModule roles = new RolesModule();
        GamesModule games = new GamesModule();
        ParrotModule parrot = new ParrotModule();
        MusicModule music = new MusicModule();
        PADModule pad = new PADModule();
        JoinLeaveModule jl = new JoinLeaveModule();
        AdminModule admin = new AdminModule();
        CustomCommandsModule custom = new CustomCommandsModule();
        ReactionsModule reactions = new ReactionsModule();

        defaultmodules.put(roles, true);
        defaultmodules.put(games, true);
        defaultmodules.put(parrot, false);
        defaultmodules.put(music, true);
        defaultmodules.put(pad, true);
        defaultmodules.put(jl, true);
        defaultmodules.put(admin, true);
        defaultmodules.put(custom, true);
        defaultmodules.put(reactions, true);

        modules = new ArrayList<IModule>();
        modules.add(roles);
        modules.add(games);
        modules.add(parrot);
        modules.add(music);
        modules.add(pad);
        modules.add(jl);
        modules.add(admin);
        modules.add(custom);
        modules.add(reactions);
        modules.sort(new Comparator<IModule>() {
            @Override
            public int compare(IModule o1, IModule o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    public void run(String token) {
        try {
            client = new ClientBuilder().withToken(token).login();
            client.getDispatcher().registerListener(new GeneralListener());
            client.getDispatcher().registerListener(new OwnerListener());
            client.getDispatcher().registerListener(new PermissionsListener());
            for (IModule m : modules) {
                m.enable(client);
            }

        } catch (DiscordException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Eschamali e = new Eschamali();
        e.run(args[0]);
    }
}
