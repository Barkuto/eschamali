package modules;

import base.Command;
import base.EschaUtil;
import base.Eschamali;
import base.Module;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

public class Roles extends Module {
    private String miscTableName = "rolesmisc";
    private String miscCol1 = "field";
    private String miscCol2 = "roles";
    private String miscField1 = "autorole";
    private String miscField2 = "selfroles";
    private String[] miscCols = new String[]{miscCol1, miscCol2};

    public Roles(DiscordClient client) {
        super(client, ".");

        client.getEventDispatcher().on(GuildCreateEvent.class).flatMap(event -> {
            DBDriver driver = ChannelPerms.getPermissionDB(event.getGuild());
            if (!driver.tableExists(miscTableName)) {
                driver.createTable(miscTableName, miscCols, new String[]{"string", "string"}, false);
                driver.addPerms(miscTableName, miscCol1, miscField1, miscCol2, "");
                driver.addPerms(miscTableName, miscCol1, miscField2, miscCol2, "");
            }
            driver.close();
            return Mono.empty();
        }).subscribe();

        client.getEventDispatcher().on(MemberJoinEvent.class).flatMap(event -> {
            Guild guild = event.getGuild().block();
            DBDriver driver = ChannelPerms.getPermissionDB(guild);
            if (ChannelPerms.isModuleOn(guild, getName())) {
                if (driver.tableExists(miscTableName)) {
                    String role = driver.getPerms(miscTableName, miscCol1, miscField1, miscCol2);
                    Role r = guild.getRoleById(Snowflake.of(Long.parseLong(role))).block();
                    if (r != null) {
                        return event.getMember().addRole(r.getId());
                    }
                }
                driver.close();
            }
            return Mono.empty();
        }).subscribe();
    }

    @Override
    protected Map<String, Command> makeCommands() {
        Map<String, Command> commands = new HashMap<>();

        Command db = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                if (EschaUtil.hasPermOr(event.getMember().get(), Permission.MANAGE_GUILD)) {
                    return EschaUtil.sendMessage(event, databaseString(guild));
                }
            }
            return Mono.empty();
        };

        Command autorole = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                DBDriver driver = ChannelPerms.getPermissionDB(guild);
                Member user = event.getMember().get();
                if (EschaUtil.hasPermOr(user, Permission.MANAGE_GUILD, Permission.MANAGE_ROLES)) {
                    String[] args = EschaUtil.getArgs(event);
                    String argsconcat = EschaUtil.getArgsConcat(event);
                    if (args.length == 0) {
                        String output = "The current autorole is: ";
                        String id = driver.getPerms(miscTableName, miscCol1, miscField1, miscCol2);
                        if (id.length() > 0) {
                            Role role = guild.getRoleById(Snowflake.of(Long.parseLong(id))).block();
                            if (role != null) {
                                output += role.getMention();
                                return EschaUtil.sendMessage(event, output);
                            }
                        }
                        return EschaUtil.sendMessage(event, "There is no auto role.");
                    } else {
                        Role role = EschaUtil.roleFromGuild(guild, argsconcat);
                        if (role != null) {
                            driver.setPerms(miscTableName, miscCol1, "autorole", miscCol2, role.getId().asString());
                            return EschaUtil.sendMessage(event, "The auto-role is now " + role.getMention());
                        }
                    }
                }

            }
            return Mono.empty();
        };

        Command removeautorole = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                DBDriver driver = ChannelPerms.getPermissionDB(guild);
                Member user = event.getMember().get();
                if (EschaUtil.hasPermOr(user, Permission.MANAGE_GUILD, Permission.MANAGE_ROLES)) {
                    driver.setPerms(miscTableName, miscCol1, miscField1, miscCol2, "");
                    return EschaUtil.sendMessage(event, "The auto-role has been removed.");
                }
            }
            return Mono.empty();
        };

        Command addrole = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                Member author = event.getMember().get();
                String[] args = EschaUtil.getArgs(event);
                if (args.length >= 2) {
                    if (EschaUtil.hasPermOr(author, Permission.MANAGE_ROLES)) {
                        String role = "";
                        for (int i = 1; i < args.length; i++) {
                            role += args[i] + " ";
                        }
                        role = role.trim();

                        Snowflake[] mentions = event.getMessage().getUserMentionIds().toArray(new Snowflake[0]);
                        if (mentions.length != 1) return EschaUtil.sendMessage(event, "Invalid Usage.");

                        Member theUser = guild.getMemberById(mentions[0]).block();
                        Role theRole = EschaUtil.roleFromGuild(guild, role);

                        if (theRole != null) {
                            return theUser.addRole(theRole.getId())
                                    .then(EschaUtil.sendMessage(event, theUser.getMention() + " now has the " + theRole.getName() + " role."))
                                    .onErrorResume(v -> EschaUtil.sendMessage(event, "An error occurred adding that role to the user."));
                        } else return EschaUtil.sendMessage(event, "Invalid role.");
                    }
                }
            }
            return Mono.empty();
        };

        Command removerole = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                Member author = event.getMember().get();
                String[] args = EschaUtil.getArgs(event);

                if (args.length >= 2) {
                    if (EschaUtil.hasPermOr(author, Permission.MANAGE_ROLES)) {
                        String role = "";
                        for (int i = 1; i < args.length; i++) {
                            role += args[i] + " ";
                        }
                        role = role.trim();

                        Snowflake[] mentions = event.getMessage().getUserMentionIds().toArray(new Snowflake[0]);
                        if (mentions.length != 1) return EschaUtil.sendMessage(event, "Invalid Usage.");

                        Member theUser = guild.getMemberById(mentions[0]).block();
                        Role theRole = EschaUtil.roleFromGuild(guild, role);

                        if (theRole != null) {
                            return theUser.removeRole(theRole.getId())
                                    .then(EschaUtil.sendMessage(event, theUser.getMention() + " no longer has the " + theRole.getName() + " role."))
                                    .onErrorResume(v -> EschaUtil.sendMessage(event, "An error occurred removing that role from the user."));
                        } else return EschaUtil.sendMessage(event, "Invalid role.");
                    }
                }
            }
            return Mono.empty();
        };

        Command iam = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                Member author = event.getMember().get();
                String[] args = EschaUtil.getArgs(event);
                String argsconcat = EschaUtil.getArgsConcat(event);

                if (args.length >= 1) {
                    Role role = EschaUtil.roleFromGuild(guild, argsconcat);
                    if (role != null) {
                        if (roleISA(guild, role)) {
                            Role theRole = role;
                            return author.addRole(role.getId())
                                    .then(event.getMessage().getChannel().flatMap(c -> c.createMessage(author.getMention() + " You now the have the " + theRole.getMention() + " role."))
                                            .flatMap(m -> Mono.delay(Duration.ofSeconds(2)).then(m.delete()))
                                            .then(event.getMessage().delete()))
                                    .onErrorResume(v -> EschaUtil.sendMessage(event, "An error occurred adding that role to you."));
                        } else return EschaUtil.sendMessage(event, "That role is not self assignable.");
                    }
                    return EschaUtil.sendMessage(event, "That is not a role!");
                }
            }
            return Mono.empty();
        };

        Command iamn = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                Member author = event.getMember().get();
                String[] args = EschaUtil.getArgs(event);
                String argsconcat = EschaUtil.getArgsConcat(event);

                if (args.length >= 1) {
                    Role role = EschaUtil.roleFromGuild(guild, argsconcat);
                    if (role != null) {
                        if (roleISA(guild, role)) {
                            Role theRole = role;
                            return author.removeRole(role.getId())
                                    .then(event.getMessage().getChannel().flatMap(c -> c.createMessage(author.getMention() + " Removed " + theRole.getMention() + " role from you."))
                                            .flatMap(m -> Mono.delay(Duration.ofSeconds(2)).then(m.delete()))
                                            .then(event.getMessage().delete()))
                                    .onErrorResume(v -> EschaUtil.sendMessage(event, "An error occurred removing that role from you."));
                        } else return EschaUtil.sendMessage(event, "That role is not self assignable.");
                    }
                    return EschaUtil.sendMessage(event, "That is not a role!");
                }
            }
            return Mono.empty();
        };

        Command inrole = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String[] args = EschaUtil.getArgs(event);
                String argsconcat = EschaUtil.getArgsConcat(event);

                if (args.length >= 1) {
                    Role role = EschaUtil.roleFromGuild(guild, argsconcat);
                    if (role == null) {
                        return EschaUtil.sendMessage(event, "That is not a role!");
                    } else {
                        String output = "`Here is a list of people in the role " + role.getMention() + ":`\n";
                        List<Member> members = guild.getMembers().collectList().block();
                        for (Member m : members) {
                            List<Role> userRoles = m.getRoles().collectList().block();
                            for (Role r : userRoles) {
                                if (r.getName().equalsIgnoreCase(role.getName())) {
                                    output += "**" + m.getUsername() + "**#" + m.getDiscriminator() + ", ";
                                }
                            }
                        }
                        if (output.charAt(output.length() - 2) == ',') {
                            output = output.substring(0, output.length() - 2);
                        }
                        return EschaUtil.sendMessage(event, output);
                    }
                }
            }
            return Mono.empty();
        };

        Command roles = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String[] args = EschaUtil.getArgs(event);

                Member user;
                String output;
                if (args.length == 0) {
                    user = event.getMessage().getAuthorAsMember().block();
                    output = "`A list of your roles, " + user.getUsername() + "#" + user.getDiscriminator() + ":`";
                } else {
                    Snowflake[] mentions = event.getMessage().getUserMentionIds().toArray(new Snowflake[0]);
                    if (mentions.length <= 0) return EschaUtil.sendMessage(event, "Invalid Usage.");

                    user = guild.getMemberById(mentions[0]).block();
                    output = "`List of roles for " + user.getUsername() + "#" + user.getDiscriminator() + ":`";
                }
                if (user != null) {
                    List<Role> userRoles = user.getRoles().collectList().block();
                    for (Role r : userRoles) {
                        output += "\n•" + r.getName() + "";
                    }
                    return EschaUtil.sendMessage(event, output);
                }
            }
            return Mono.empty();
        };

        Command allroles = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String output = "`List of roles:`";
                List<Role> allRoles = guild.getRoles().collectList().block();
                for (Role r : allRoles) {
                    if (!r.isEveryone())
                        output += "\n•" + r.getName();
                }
                return EschaUtil.sendMessage(event, output);
            }
            return Mono.empty();
        };

        Command lsar = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                DBDriver driver = ChannelPerms.getPermissionDB(guild);
                String selfroles = driver.getPerms(miscTableName, miscCol1, "selfroles", miscCol2);
                int count = 0;
                String output = "";
                if (selfroles != null && selfroles.length() > 0) {
                    String[] theRoles = selfroles.split(";");
                    Arrays.sort(theRoles);
                    for (int i = 0; i < theRoles.length; i++) {
                        Role r = guild.getRoleById(Snowflake.of(Long.parseLong(theRoles[i]))).block();
                        if (r != null) {
                            output += r.getName() + ", ";
                            count++;
                        }
                    }
                    if (count > 0) {
                        output = output.substring(0, output.length() - 2);
                    }
                }
                String thingy1 = "are";
                String thingy2 = "roles";
                if (count == 1) {
                    thingy1 = "is";
                    thingy2 = "role";
                }
                return EschaUtil.sendMessage(event, "There " + thingy1 + " `" + count + "` self assignable " + thingy2 + ":\n" + output);
            }
            return Mono.empty();
        };

        Command asar = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String[] args = EschaUtil.getArgs(event);
                String argsconcat = EschaUtil.getArgsConcat(event);
                DBDriver driver = ChannelPerms.getPermissionDB(guild);

                if (EschaUtil.hasPermOr(event.getMessage().getAuthorAsMember().block(), Permission.MANAGE_ROLES)) {
                    if (args.length > 1 && !argsconcat.contains(";")) {
                        Role role = EschaUtil.roleFromGuild(guild, argsconcat.trim());
                        if (role != null) {
                            if (!roleISA(guild, role)) {
                                driver.addPerms(miscTableName, miscCol1, miscField2, miscCol2, role.getId().asString());
                                return EschaUtil.sendMessage(event, "Role has been successfully added as self assignable.");
                            } else {
                                return EschaUtil.sendMessage(event, "That role is already self assignable.");
                            }
                        } else {
                            return EschaUtil.sendMessage(event, "That is not a valid role.");
                        }
                    } else {
                        String[] theRoles = argsconcat.trim().split(";");
                        ArrayList<Role> rolesToAdd = new ArrayList<>();
                        for (int i = 0; i < theRoles.length; i++) {
                            Role r = EschaUtil.roleFromGuild(guild, theRoles[i].trim());
                            if (r != null) {
                                rolesToAdd.add(r);
                            }
                        }

                        int added = 0;
                        String output = "";
                        for (Role r : rolesToAdd) {
                            if (!roleISA(guild, r)) {
                                driver.addPerms(miscTableName, miscCol1, miscField2, miscCol2, r.getId().asString());
                                output += r.getName() + ", ";
                                added++;
                            }
                        }
                        if (added > 0) {
                            output = output.substring(0, output.length() - 2).trim();
                        }
                        return EschaUtil.sendMessage(event, "Added `" + added + "` role" + (added == 1 ? "" : "s") + " as self assignable:\n" + output);
                    }
                } else {
                    return EschaUtil.sendMessage(event, "You do not have permissions to manage roles.");
                }
            }
            return Mono.empty();
        };

        Command rsar = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                String[] args = EschaUtil.getArgs(event);
                String argsconcat = EschaUtil.getArgsConcat(event);
                DBDriver driver = ChannelPerms.getPermissionDB(guild);

                if (EschaUtil.hasPermOr(event.getMessage().getAuthorAsMember().block(), Permission.MANAGE_ROLES)) {
                    if (args.length > 1 && !argsconcat.contains(";")) {
                        Role role = EschaUtil.roleFromGuild(guild, argsconcat.trim());
                        if (role != null) {
                            if (roleISA(guild, role)) {
                                String[] csar = driver.getPerms(miscTableName, miscCol1, miscField2, miscCol2).split(";");
                                String nsar = "";
                                for (int i = 0; i < csar.length; i++) {
                                    if (!csar[i].equalsIgnoreCase(role.getId().asString())) {
                                        nsar += csar[i] + ";";
                                    }
                                }
                                if (nsar.length() > 0) {
                                    nsar = nsar.substring(0, nsar.lastIndexOf(";"));
                                }
                                driver.setPerms(miscTableName, miscCol1, miscField2, miscCol2, nsar);
                                return EschaUtil.sendMessage(event, "Role has been successfully removed from being self assignable.");
                            } else {
                                return EschaUtil.sendMessage(event, "That role is not self assignable.");
                            }
                        } else {
                            return EschaUtil.sendMessage(event, "That is not a valid role.");
                        }
                    } else {
                        String[] rolesToRemove = argsconcat.trim().split(";");
                        List<String> csar = Arrays.asList(driver.getPerms(miscTableName, miscCol1, miscField2, miscCol2).split(";"));
                        ArrayList<Role> removeRoles = new ArrayList<>();
                        for (int i = 0; i < rolesToRemove.length; i++) {
                            Role r = EschaUtil.roleFromGuild(guild, rolesToRemove[i]);
                            if (r != null) {
                                removeRoles.add(r);
                            }
                        }
                        ArrayList<String> nsar = new ArrayList<>();
                        int removed = 0;
                        String output = "";
                        for (int i = 0; i < csar.size(); i++) {
                            boolean remove = false;
                            for (int j = 0; j < removeRoles.size(); j++) {
                                if (csar.get(i).equalsIgnoreCase(removeRoles.get(j).getId().asString())) {
                                    removed++;
                                    remove = true;
                                    output += guild.getRoleById(removeRoles.get(j).getId()).block().getName() + ", ";
                                }
                            }
                            if (!remove) {
                                nsar.add(csar.get(i));
                            }
                        }
                        if (output.length() > 0) {
                            output = output.substring(0, output.lastIndexOf(","));
                        }

                        String newPerm = "";
                        for (int i = 0; i < nsar.size(); i++) {
                            newPerm += nsar.get(i) + ";";
                        }
                        if (newPerm.length() > 0) {
                            newPerm = newPerm.substring(0, newPerm.lastIndexOf(";"));
                        }
                        driver.setPerms(miscTableName, miscCol1, miscField2, miscCol2, newPerm);
                        return EschaUtil.sendMessage(event, "Removed `" + removed + "` role" + (removed == 1 ? "" : "s") + " from being self assignable:\n" + output);
                    }
                } else {
                    return EschaUtil.sendMessage(event, "You do not have permissions to manage roles.");
                }
            }
            return Mono.empty();
        };

        Command servertree = event -> {
            Guild guild = event.getGuild().block();
            Channel channel = event.getMessage().getChannel().block();
            if (ChannelPerms.canModuleIn(guild, getName(), channel)) {
                Member author = event.getMessage().getAuthorAsMember().block();
                if (Eschamali.ownerIDs.contains(author.getId().asLong())) {
                    List<Role> allRoles = guild.getRoles().collectList().block();
                    allRoles.sort((o1, o2) -> Integer.compare(o2.getPosition().block(), o1.getPosition().block()));
                    PrivateChannel pm;
                    pm = author.getPrivateChannel().block();
                    String s = "";
                    for (Role r : allRoles) {
                        if (!r.isEveryone())
                            s += r.getName() + "\n";
                    }
                    return EschaUtil.sendMessage(pm, s);
                }
            }
            return Mono.empty();
        };

        commands.put(prefix+"db", db);
        commands.put(prefix+"autorole", autorole);
        commands.put(prefix+"removeautorole", removeautorole);
        commands.put(prefix+"addrole", addrole);
        commands.put(prefix+"ar", addrole);
        commands.put(prefix+"removerole", removerole);
        commands.put(prefix+"rr", removerole);
        commands.put(prefix+"iam", iam);
        commands.put(prefix+"iamn", iamn);
        commands.put(prefix+"inrole", inrole);
        commands.put(prefix+"roles", roles);
        commands.put(prefix+"allroles", allroles);
        commands.put(prefix+"lsar", lsar);
        commands.put(prefix+"asar", asar);
        commands.put(prefix+"rsar", rsar);
        commands.put(prefix+"servertree", servertree);

        return commands;
    }

    @Override
    public String getName() {
        return "Roles";
    }

    public String databaseString(Guild guild) {
        //rolemisc:field|roles
        //         autorole
        //         selfroles
        //roles:role|general|roles|pad|music
        String s = "```\n";
        s += miscTableName + "\n";
        DBDriver driver = ChannelPerms.getPermissionDB(guild);
        ResultSet rs = driver.selectAllFrom(miscTableName);
        try {
            while (rs.next()) {
                s += rs.getString(miscCol1) + ": " + rs.getString(miscCol2) + "\n";
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        driver.close();
        return s + "\n```";
    }


    public boolean roleISA(Guild guild, Role role) { // checks to see if role is self assignable
        DBDriver driver = ChannelPerms.getPermissionDB(guild);
        String selfroles = driver.getPerms(miscTableName, miscCol1, miscField2, miscCol2);
        if (selfroles != null && selfroles.length() > 0) {
            String[] split = selfroles.split(";");
            for (int i = 0; i < split.length; i++) {
                if (split[i].equalsIgnoreCase(role.getId().asString())) {
                    return true;
                }
            }
        }
        return false;
    }
}
