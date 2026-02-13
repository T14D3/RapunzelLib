package de.t14d3.rapunzellib.commands.paper;

import com.mojang.brigadier.CommandDispatcher;
import de.t14d3.rapunzellib.commands.CommandSourceAdapters;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class PaperSharedBrigadierRegistration {
    private PaperSharedBrigadierRegistration() {
    }

    static void register(
        @NotNull Commands commands,
        @NotNull RCommandService commandService,
        @NotNull CommandSourceAdapters adapters
    ) {
        Objects.requireNonNull(commands, "commands");
        Objects.requireNonNull(commandService, "commandService");
        Objects.requireNonNull(adapters, "adapters");

        CommandDispatcher<CommandSourceStack> dispatcher = commands.getDispatcher();
        RCommandTree<RCommandSource> sharedTree = commandService.sharedTree();
        for (RCommandNode<RCommandSource> root : sharedTree.getRoots()) {
            commands.register(
                sharedTree.buildRootMapped(root, dispatcher, adapters::wrap),
                root.getDescription(),
                root.getAliases()
            );
        }
    }
}
