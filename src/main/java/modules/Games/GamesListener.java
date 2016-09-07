package modules.Games;

import modules.BufferedMessage.BufferedMessage;
import modules.Channels.ChannelsListener;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Iggie on 8/17/2016.
 */
public class GamesListener {
    public static String prefix = ">";
    private ArrayList<String> answers;

    public GamesListener() {
        super();
        answers = new ArrayList<String>();
        answers.add("It is certain");
        answers.add("It is decidedly so");
        answers.add("Without a doubt");
        answers.add("Yes, definitely");
        answers.add("You may rely on it");
        answers.add("As I see it, yes");
        answers.add("Most likely");
        answers.add("Outlook good");
        answers.add("Yes");
        answers.add("Signs point to yes");

        answers.add("Reply hazy try again");
        answers.add("Ask again later");
        answers.add("Better not tell you now");
        answers.add("Cannot predict now");
        answers.add("Concentrate and ask again");

        answers.add("Don't count on it");
        answers.add("My reply is no");
        answers.add("My sources say no");
        answers.add("Outlook not so good");
        answers.add("Very doubtful");
        answers.add("NO - May cause disease contraction");

    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        if (!(event.getMessage().getChannel() instanceof IPrivateChannel)) {
            IGuild guild = event.getMessage().getGuild();
            IChannel channel = event.getMessage().getChannel();
            if (PermissionsListener.isModuleOn(guild, GamesModule.name) && PermissionsListener.canModuleInChannel(guild, GamesModule.name, channel)) {
                String msg = event.getMessage().getContent();
                IUser user = event.getMessage().getAuthor();
                if (msg.startsWith(prefix)) {
                    String[] args = msg.split(" ");
                    args[0] = args[0].replace(prefix, "");
                    String cmd = args[0];
                    if (cmd.equalsIgnoreCase("8") || cmd.equalsIgnoreCase("8ball")) {
                        if (args.length > 1) {
                            int num = new Random().nextInt(answers.size());
                            String question = "";
                            for (int i = 1; i < args.length; i++) {
                                question += args[i] + " ";
                            }
                            question.trim();
                            BufferedMessage.sendMessage(GamesModule.client, event, "\n" + ":question: `Question` " + question + "\n:8ball: `8ball answers`" + answers.get(num));
                        }
                    } else if (cmd.equalsIgnoreCase("choose")) {
                        if (args.length > 1) {
                            String wholeArgs = "";
                            for (int i = 1; i < args.length; i++) {
                                wholeArgs += args[i] + " ";
                            }
                            wholeArgs = wholeArgs.trim();
                            if (wholeArgs.charAt(wholeArgs.length() - 1) == ';') {
                                wholeArgs = wholeArgs.substring(0, wholeArgs.length() - 1);
                            }
                            String[] choices = wholeArgs.split(";");
                            for (int i = 0; i < choices.length; i++) {
                                choices[i] = choices[i].trim();
                            }
                            BufferedMessage.sendMessage(GamesModule.client, event, choices[new Random().nextInt(choices.length)]);
                        }
                    } else if (cmd.equalsIgnoreCase("rps")) {
                        if (args.length > 1) {
                            int pick;
                            switch (args[1].toLowerCase()) {
                                case "r":
                                case "rock":
                                case "rocket":
                                    pick = 0;
                                    break;
                                case "p":
                                case "paper":
                                case "paperclip":
                                    pick = 1;
                                    break;
                                case "scissors":
                                case "s":
                                    pick = 2;
                                    break;
                                default:
                                    return;
                            }
                            int botPick = new Random().nextInt(3);
                            String output = "";
                            if (pick == botPick)
                                output = "It's a draw! Both picked :" + rpsPick(pick) + ":";
                            else if ((pick == 0 && botPick == 1) ||
                                    (pick == 1 && botPick == 2) ||
                                    (pick == 2 && botPick == 0))
                                output = GamesModule.client.getOurUser().mention() + " won! :" + rpsPick(botPick) + ": beats :" + rpsPick(pick) + ":";
                            else
                                output = user.mention() + " won! :" + rpsPick(pick) + ": beats :" + rpsPick(botPick) + ":";
                            BufferedMessage.sendMessage(GamesModule.client, event, output);
                        }
                    } else if (cmd.equalsIgnoreCase("roll")) {
                        BufferedMessage.sendMessage(GamesModule.client, event, "You rolled a `" + (new Random().nextInt(100) + 1) + "`!");
                    }
                }
            }
        }
    }

    public String rpsPick(int pick) {
        if (pick == 0)
            return "rocket";
        else if (pick == 1)
            return "paperclip";
        else
            return "scissors";
    }
}
