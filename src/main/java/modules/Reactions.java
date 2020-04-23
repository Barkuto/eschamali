package modules;

import base.Command;
import base.EschaUtil;
import base.Module;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Reactions extends Module {
    private String[] enk = {
            "wtf|why are you awake",
            "why are you awake",
            "shouldn't you be asleep",
            "shouldn't you be studying",
            ":gotosleep:",
            "wtf|:gotosleep:",
            "its past your bedtime",
            "isnt it past your bedtime|why are you awake"
    };

    public Reactions(DiscordClient client) {
        super(client, "");

        this.client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.justOrEmpty(event.getMessage())
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        .flatMap(message -> message.getChannel()
                                .ofType(TextChannel.class)
                                .flatMap(channel -> checkForReaction(event)))).subscribe();
    }

    private Mono<Void> checkForReaction(MessageCreateEvent event) {
        Guild guild = event.getGuild().block();
        Channel channel = event.getMessage().getChannel().block();
        if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
            Message message = event.getMessage();
            User author = message.getAuthor().get();
            String msg = message.getContent().isPresent() ? message.getContent().get() : "";
            Set<Snowflake> mentioned = message.getUserMentionIds();

            if (msg.replace("\'", "").toLowerCase().contains("dont quote me")) {
                return EschaUtil.sendMessage(event, "\"" + msg + "\" -" + author.getUsername());
            } else if (msg.toLowerCase().contains("alot")) {
                return EschaUtil.sendMessage(event, "http://thewritepractice.com/wp-content/uploads/2012/05/Alot-vs-a-lot1-600x450.png");
            } else if (author.getId().asLong() == 207986006840836097L && mentioned.contains(Snowflake.of(102559179507519488L))) {
//            } else if (author.getId().asLong() == 85844964633747456L && mentioned.contains(Snowflake.of(216037525754609664L))) {
                int choice = new Random().nextInt(enk.length);
                this.client.getGuilds()
                        .filter(g -> g.getId().asLong() == 85847666780553216L)
                        .flatMap(emojiGuild -> emojiGuild.getGuildEmojiById(Snowflake.of(667622392612257842L))
//                this.client.getGuilds()
//                        .filter(g -> g.getId().asLong() == 259163710109646849L)
//                        .flatMap(emojiGuild -> emojiGuild.getGuildEmojiById(Snowflake.of(667623052309168128L))
                                .flatMap(gotosleep -> {
                                    String[] lines = enk[choice].split("\\|");
                                    for (String s : lines) {
                                        if (s.contains(":gotosleep:"))
                                            s = s.replace(":gotosleep:", gotosleep.asFormat());
                                        EschaUtil.sendMessage(event, s).subscribe();
                                    }
                                    return Mono.empty();
                                })).subscribe();
                return Mono.empty();
            }
        }
        return Mono.empty();
    }

    @Override
    protected Map<String, Command> makeCommands() {
        return Collections.emptyMap();
    }

    @Override
    public String getName() {
        return "Reactions";
    }
}