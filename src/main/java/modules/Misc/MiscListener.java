package modules.Misc;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;

/**
 * Created by Iggie on 8/17/2016.
 */
public class MiscListener {
    private String prefix = "!";
    static ArrayList<String> answers;

    public MiscListener() {
        super();
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
        String msg = event.getMessage().getContent();
        IUser user = event.getMessage().getAuthor();
        if (msg.startsWith(prefix)) {
            String[] args = msg.split(" ");
            args[0] = args[0].replace(prefix, "");
            String cmd = args[0];
            if (cmd.equalsIgnoreCase("8") || cmd.equalsIgnoreCase("8ball")) {

            }
        }
    }
}
