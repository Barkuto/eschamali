package modules;

import base.Command;
import base.EschaUtil;
import base.Module;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Games extends Module {
    private ArrayList<String> answers;

    public Games(GatewayDiscordClient client) {
        super(client, ">");
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

    @Override
    protected Map<String, Command> makeCommands() {
        Map<String, Command> commands = new HashMap<>();

        Command eightball = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String[] args = EschaUtil.getArgs(event);
                if (args.length >= 1) {
                    int num = new Random().nextInt(answers.size());
                    String question = "";
                    for (int i = 0; i < args.length; i++) {
                        question += args[i] + " ";
                    }
                    question = question.trim();
                    return EschaUtil.sendMessage(event, "\n" + ":question: `Question` " + question + "\n:8ball: `8ball answers` " + answers.get(num));
                }
            }
            return Mono.empty();
        };

        Command choose = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String[] args = EschaUtil.getArgs(event);
                if (args.length >= 1) {
                    String wholeArgs = "";
                    for (int i = 0; i < args.length; i++) {
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
                    return EschaUtil.sendMessage(event, choices[new Random().nextInt(choices.length)]);
                }
            }
            return Mono.empty();
        };

        Command rps = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String[] args = EschaUtil.getArgs(event);
                if (args.length >= 1) {
                    int pick;
                    switch (args[0].toLowerCase()) {
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
                            return Mono.empty();
                    }
                    int botPick = new Random().nextInt(3);
                    String output;
                    if (pick == botPick)
                        output = "It's a draw! Both picked :" + rpsPick(pick) + ":";
                    else if ((pick == 0 && botPick == 1) ||
                            (pick == 1 && botPick == 2) ||
                            (pick == 2 && botPick == 0))
                        output = client.getSelf().block().getMention() + " won! :" + rpsPick(botPick) + ": beats :" + rpsPick(pick) + ":";
                    else
                        output = event.getMember().get().getMention() + " won! :" + rpsPick(pick) + ": beats :" + rpsPick(botPick) + ":";
                    return EschaUtil.sendMessage(event, output);
                }
            }
            return Mono.empty();
        };

        Command roll = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String[] args = EschaUtil.getArgs(event);
                if (args.length == 0) {
                    return EschaUtil.sendMessage(event, "You rolled a `" + (new Random().nextInt(100) + 1) + "`!");
                } else {
                    int bound = -1;
                    try {
                        bound = Integer.parseInt(args[0]);
                        return EschaUtil.sendMessage(event, "You rolled a `" + (new Random().nextInt(bound) + 1) + "`!");
                    } catch (IllegalArgumentException e) {
                        return EschaUtil.sendMessage(event, "Invalid Argument.");
                    }
                }
            }
            return Mono.empty();
        };

        Command poll = event -> {
            Guild guild = event.getGuild().block();
            MessageChannel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String argsconcat = EschaUtil.getArgsConcat(event);
                String[] params = argsconcat.split(";");
                if (params.length > 0 && params.length <= 27) {
                    ArrayList<Integer> codes = new ArrayList<>();
                    Mono<Message> msg = channel.createMessage(mSpec -> mSpec.setEmbed(e -> {
                        String a = "🇦";
                        int startCodepoint = a.codePointAt(0);

                        StringBuilder desc = new StringBuilder();
                        for (int i = 1; i < params.length; i++) {
                            int code = startCodepoint + i - 1;
                            codes.add(code);
                            desc.appendCodePoint(code);
                            desc.append(": " + params[i] + "\n");
                        }

                        e.setTitle(params[0]);
                        e.setDescription(desc.toString());
                    }));
                    return msg.flatMap(message -> {
                        Flux.fromIterable(codes).subscribe(c -> message.addReaction(ReactionEmoji.unicode(new StringBuilder().appendCodePoint(c).toString())).subscribe());
                        return Mono.empty();
                    });
                } else {
                    return EschaUtil.sendMessage(event, "Too many arguments.");
                }
            }
            return Mono.empty();
        };

        commands.put(prefix + "8ball", eightball);
        commands.put(prefix + "8", eightball);
        commands.put(prefix + "choose", choose);
        commands.put(prefix + "rps", rps);
        commands.put(prefix + "roll", roll);
        commands.put(prefix + "poll", poll);

        return commands;
    }

    @Override
    public String getName() {
        return "Games";
    }

    private String rpsPick(int pick) {
        if (pick == 0)
            return "rocket";
        else if (pick == 1)
            return "paperclip";
        else
            return "scissors";
    }
}
