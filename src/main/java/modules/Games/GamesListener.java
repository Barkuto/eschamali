package modules.Games;

import modules.BufferedMessage.Sender;
import modules.Permissions.PermissionsListener;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuilder;

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
        answers = new ArrayList<>();
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
                    String cmd = args[0].toLowerCase();
                    if (cmd.equals("8") || cmd.equals("8ball")) {
                        if (args.length > 1) {
                            int num = new Random().nextInt(answers.size());
                            String question = "";
                            for (int i = 1; i < args.length; i++) {
                                question += args[i] + " ";
                            }
                            question = question.trim();
                            Sender.sendMessage(channel, "\n" + ":question: `Question` " + question + "\n:8ball: `8ball answers` " + answers.get(num));
                        }
                    } else if (cmd.equals("choose")) {
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
                            Sender.sendMessage(channel, choices[new Random().nextInt(choices.length)]);
                        }
                    } else if (cmd.equals("rps")) {
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
                            String output;
                            if (pick == botPick)
                                output = "It's a draw! Both picked :" + rpsPick(pick) + ":";
                            else if ((pick == 0 && botPick == 1) ||
                                    (pick == 1 && botPick == 2) ||
                                    (pick == 2 && botPick == 0))
                                output = GamesModule.client.getOurUser().mention() + " won! :" + rpsPick(botPick) + ": beats :" + rpsPick(pick) + ":";
                            else
                                output = user.mention() + " won! :" + rpsPick(pick) + ": beats :" + rpsPick(botPick) + ":";
                            Sender.sendMessage(channel, output);
                        }
                    } else if (cmd.equals("roll")) {
                        Sender.sendMessage(channel, "You rolled a `" + (new Random().nextInt(100) + 1) + "`!");
                    } else if (cmd.equals("poll")) {
                        String[] params = msg.substring(msg.indexOf(" ")).split(";");
                        if (params.length > 1 && params.length <= 27) {
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.withTitle(params[0]);

                            String a = "🇦";
                            int startCodepoint = a.codePointAt(0);

                            StringBuilder desc = new StringBuilder();
                            ArrayList<Integer> codes = new ArrayList<>();
                            for (int i = 1; i < params.length; i++) {
                                int code = startCodepoint + i - 1;
                                codes.add(code);
                                desc.appendCodePoint(code);
                                desc.append(": " + params[i] + "\n");
                            }

                            eb.withDesc(desc.toString());

                            EmbedObject e = eb.build();

                            RequestBuffer.request(() -> {
                                IMessage m = channel.sendMessage(e);

                                RequestBuilder rb = new RequestBuilder(GamesModule.client);
                                rb.shouldBufferRequests(true);
                                for (int i = 0; i < codes.size(); i++) {
                                    String unicode = new StringBuilder().appendCodePoint(codes.get(i)).toString();

                                    if (i == 0)
                                        rb.doAction(() -> {
                                            m.addReaction(ReactionEmoji.of(unicode));
                                            return true;
                                        });
                                    else
                                        rb.andThen(() -> {
                                            m.addReaction(ReactionEmoji.of(unicode));
                                            return true;
                                        });
                                }
                                rb.build();
                            });
                        }
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
