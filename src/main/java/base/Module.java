package base;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class Module {
    protected GatewayDiscordClient client;
    protected String prefix;

    public Module(GatewayDiscordClient client, String prefix) {
        this.client = client;
        this.prefix = prefix;

        Map<String, Command> commands = makeCommands();

        this.client.on(MessageCreateEvent.class)
                // Get event Message
                .flatMap(event -> Mono.justOrEmpty(event.getMessage())
                        // Filter out other bot messages
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                        // Only read from guild chats, aka not DMs
                        .flatMap(message -> message.getChannel()
                                .ofType(TextChannel.class)
                                // Turn message into its contents
                                .flatMap(channel -> Mono.justOrEmpty(event.getMessage().getContent())
                                        // Iterate through all commands
                                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                                //If a command matches, execute it
                                                .filter(entry -> {
                                                    if (content.contains(" ")) {
                                                        return content.substring(0, content.indexOf(" ")).equalsIgnoreCase(entry.getKey());
                                                    } else return content.equalsIgnoreCase(entry.getKey());
                                                })
                                                .flatMap(entry -> entry.getValue().execute(event))
                                                .next()))))
                .subscribe();
    }

    protected abstract Map<String, Command> makeCommands();

    public abstract String getName();
}
