package base;

import modules.BufferedMessage.BufferedMessage;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.modules.IModule;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Iggie on 8/19/2016.
 */
public class ModuleListener {
    private String prefix = "!";

    @EventSubscriber
    public void onJoin(GuildCreateEvent event) {
        File modulesF = new File("servers/" + event.getGuild().getName() + "-" + event.getGuild().getID() + "/modules.txt");
        if (!modulesF.exists()) {
            modulesF.getParentFile().mkdirs();
            try {
                modulesF.createNewFile();
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(modulesF, false)));
                for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                    pw.println(entry.getKey().getName() + ":" + entry.getValue());
                }
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        String[] args = message.split(" ");
        args[0] = args[0].replace(prefix, "").trim();
        String cmd = args[0];
        IGuild guild = event.getMessage().getGuild();
        File modulesF = new File("servers/" + event.getMessage().getGuild().getName() + "-" + event.getMessage().getGuild().getID() + "/modules.txt");
        if (message.startsWith(prefix)) {
            if (cmd.equalsIgnoreCase("m") || cmd.equalsIgnoreCase("mods") || cmd.equalsIgnoreCase("modules")) {//Lists modules
                String msg = "`List of modules: `\n";
                try {
                    Scanner s = new Scanner(modulesF);
                    while (s.hasNextLine()) {
                        String line = s.nextLine();
                        String[] split = line.split(":");
                        msg += (Boolean.parseBoolean(split[1]) ? ":o: " : ":x: ") + split[0] + "\n";
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                BufferedMessage.sendMessage(Eschamali.client, event, msg);
            } else if (cmd.equalsIgnoreCase("ddm") || cmd.equalsIgnoreCase("dam")) {//Disable all modules
                try {
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(modulesF, false)));
                    for (int i = 0; i < Eschamali.modules.size(); i++) {
                        pw.println(Eschamali.modules.get(i).getName() + ":" + false);
                    }
                    pw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedMessage.sendMessage(Eschamali.client, event, "All modules have been disabled.");
            } else if (cmd.equalsIgnoreCase("edm")) {//Enables default modules
                try {
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(modulesF, false)));
                    for (Map.Entry<IModule, Boolean> entry : Eschamali.defaultmodules.entrySet()) {
                        pw.println(entry.getKey().getName() + ":" + entry.getValue());
                    }
                    pw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedMessage.sendMessage(Eschamali.client, event, "Default modules have been enabled.");
            } else if (cmd.equalsIgnoreCase("eam")) {//Enables all modules
                try {
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(modulesF, false)));
                    for (int i = 0; i < Eschamali.modules.size(); i++) {
                        pw.println(Eschamali.modules.get(i).getName() + ":" + true);
                    }
                    pw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BufferedMessage.sendMessage(Eschamali.client, event, "All modules have been enabled.");
            } else if (cmd.equalsIgnoreCase("em")) {//Enables given module
                if (args.length > 1) {
                    String module = "";
                    for (int i = 1; i < args.length; i++) {
                        module += args[i] + " ";
                    }
                    module = module.trim();

                    ArrayList<String> lines = new ArrayList<String>();
                    try {
                        Scanner s = new Scanner(modulesF);
                        while (s.hasNextLine()) {
                            lines.add(s.nextLine());
                        }

                        String modName = "";
                        for (int i = 0; i < lines.size(); i++) {
                            if (lines.get(i).equalsIgnoreCase(module + ":" + true)) {
                                modName = "done";
                                break;
                            } else if (lines.get(i).equalsIgnoreCase(module + ":" + false)) {
                                lines.set(i, lines.get(i).replace("false", "true"));
                                modName = lines.get(i).split(":")[0];
                                break;
                            }
                        }

                        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(modulesF, false)));
                        for (int i = 0; i < lines.size(); i++) {
                            pw.println(lines.get(i));
                        }
                        pw.close();

                        if (modName.equals("done")) {
                            BufferedMessage.sendMessage(Eschamali.client, event, "That module is already enabled.");
                        } else if (!modName.equals("")) {
                            BufferedMessage.sendMessage(Eschamali.client, event, "The " + modName + " module has been enabled.");
                        } else {
                            BufferedMessage.sendMessage(Eschamali.client, event, "That is not a valid module.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (cmd.equalsIgnoreCase("dm")) {//Disables given module
                if (args.length > 1) {
                    String module = "";
                    for (int i = 1; i < args.length; i++) {
                        module += args[i] + " ";
                    }
                    module = module.trim();

                    ArrayList<String> lines = new ArrayList<String>();
                    try {
                        Scanner s = new Scanner(modulesF);
                        while (s.hasNextLine()) {
                            lines.add(s.nextLine());
                        }

                        String modName = "";
                        for (int i = 0; i < lines.size(); i++) {
                            if (lines.get(i).equalsIgnoreCase(module + ":" + false)) {
                                modName = "done";
                                break;
                            } else if (lines.get(i).equalsIgnoreCase(module + ":" + true)) {
                                lines.set(i, lines.get(i).replace("true", "false"));
                                modName = lines.get(i).split(":")[0];
                                break;
                            }
                        }

                        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(modulesF, false)));
                        for (int i = 0; i < lines.size(); i++) {
                            pw.println(lines.get(i));
                        }
                        pw.close();

                        if (modName.equals("done")) {
                            BufferedMessage.sendMessage(Eschamali.client, event, "That module is already disabled.");
                        } else if (!modName.equals("")) {
                            BufferedMessage.sendMessage(Eschamali.client, event, "The " + modName + " module has been disabled.");
                        } else {
                            BufferedMessage.sendMessage(Eschamali.client, event, "That is not a valid module.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static boolean isModuleOn(IGuild guild, String moduleName) {
        File f = new File("servers/" + guild.getName() + "-" + guild.getID() + "/modules.txt");
        try {
            Scanner s = new Scanner(f);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.equalsIgnoreCase(moduleName + ":" + true)) {
                    return true;
                } else if (line.equalsIgnoreCase(moduleName + ":" + false)) {
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
}
