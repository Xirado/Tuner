package at.xirado.tuner.interaction;

import at.xirado.tuner.Application;
import at.xirado.tuner.config.TunerConfiguration;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InteractionManager {

    private static final Logger LOG = LoggerFactory.getLogger(InteractionManager.class);
    private static final String commandsPackage = "at.xirado.tuner.interaction.commands";

    private final Application application;

    private final List<GenericCommand> globalCommands;
    private final Map<Long, List<GenericCommand>> guildCommands;

    public InteractionManager(Application application) {
        this.application = application;
        this.globalCommands = Collections.synchronizedList(new ArrayList<>());
        this.guildCommands = new ConcurrentHashMap<>();
    }

    public void init() {
        registerCommands();
    }

    public GenericCommand getGenericCommand(String name, int type) {
        return globalCommands.stream()
                .filter(command -> command.getCommandData().getName().equalsIgnoreCase(name) && command.getType().getId() == type)
                .findFirst()
                .orElse(null);
    }

    public GenericCommand getGenericCommand(long guildId, String name, int type) {
        return guildCommands.getOrDefault(guildId, Collections.emptyList())
                .stream()
                .filter(command -> command.getCommandData().getName().equalsIgnoreCase(name) && command.getType().getId() == type)
                .findFirst()
                .orElse(null);
    }

    private EnumSet<Permission> getMissingPermissions(Member member, GuildChannel channel, EnumSet<Permission> requiredPerms) {
        EnumSet<Permission> perms = EnumSet.noneOf(Permission.class);

        for (Permission permission : requiredPerms) {
            if (!member.hasPermission(channel, permission))
                perms.add(permission);
        }

        return perms;
    }


    public void handleInteraction(GenericCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();

        GenericCommand command = getGenericCommand(guildId, event.getName(), event.getCommandType().getId());

        if (command == null)
            getGenericCommand(event.getName(), event.getCommandType().getId());

        if (command == null)
            return;

        Member member = event.getMember();

        var missingPermissions = getMissingPermissions(member, event.getGuildChannel(), command.getRequiredUserPermissions());

        if (!missingPermissions.isEmpty()) {
            var missingPermsString = missingPermissions.stream().map(Enum::toString).collect(Collectors.joining(", "));
            event.reply("You cannot execute this command because you are missing the following permission" + (missingPermissions.size() == 1 ? "" : "s") + ": `" + missingPermsString + "`").queue();
            return;
        }

        var missingBotPermissions = getMissingPermissions(event.getGuild().getSelfMember(), event.getGuildChannel(), command.getRequiredBotPermissions());

        if (!missingBotPermissions.isEmpty()) {
            var missingPermsString = missingBotPermissions.stream().map(Enum::toString).collect(Collectors.joining(", "));
            event.reply("You cannot execute this command because i am missing the following permission" + (missingBotPermissions.size() == 1 ? "" : "s") + ": `" + missingPermsString + "`").queue();
            return;
        }

        if (command.getType() == CommandType.SLASH_COMMAND) {
            var slashEvent = (SlashCommandInteractionEvent) event;
            var slashCommand = (SlashCommand) command;

            slashCommand.execute(slashEvent);
        }
    }

    private void registerCommands() {
        var updateAction = application.getShardManager().getShards().get(0).updateCommands();

        registerCommandsOfClass(SlashCommand.class, updateAction);

        updateAction.queue();
        updateGuildCommands();
    }

    private void registerCommand(@NotNull CommandListUpdateAction action, @NotNull GenericCommand command) {
        TunerConfiguration config = application.getTunerConfiguration();

        Checks.notNull(command, "Command");

        command.setApplication(application);

        if (command.isGlobal() && !config.isDevMode()) {
            this.globalCommands.add(command);
            action.addCommands(command.getCommandData());
            return;
        }

        var enabledGuilds = config.isDevMode() ? config.getDevGuilds() : command.getEnabledGuilds();
        enabledGuilds.forEach(id -> addGuildCommand(id, command));
    }

    private void addGuildCommand(long guildId, GenericCommand command) {
        List<GenericCommand> enabledCommands = guildCommands.containsKey(guildId) ? guildCommands.get(guildId) : new ArrayList<>();

        if (enabledCommands.contains(command))
            throw new IllegalArgumentException(command.getType() + " \"" + command.getCommandData().getName() + "\" has already been registered!");

        enabledCommands.add(command);

        guildCommands.put(guildId, enabledCommands);
    }

    private void updateGuildCommands() {
        var guilds = guildCommands.keySet();
        guilds.forEach(guildId -> {
            Guild guild = application.getShardManager().getGuildById(guildId);
            if (guild == null)
                return;

            CommandListUpdateAction action = guild.updateCommands();
            var commands = guildCommands.get(guildId);
            commands.forEach(command -> action.addCommands(command.getCommandData()));
            action.queue();
        });
    }

    private void registerCommandsOfClass(Class<? extends GenericCommand> clazz, CommandListUpdateAction action) {
        ScanResult rs = new ClassGraph().acceptPackages(commandsPackage).enableClassInfo().scan();
        rs.getSubclasses(clazz).loadClasses().forEach(loadedClazz -> {
            try {
                registerCommand(action, (GenericCommand) loadedClazz.getDeclaredConstructor().newInstance());
            } catch (Exception ignored) {}
        });
        rs.close();
    }
}
