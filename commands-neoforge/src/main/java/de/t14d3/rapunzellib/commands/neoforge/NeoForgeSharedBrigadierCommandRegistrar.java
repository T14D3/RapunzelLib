package de.t14d3.rapunzellib.commands.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.commands.CommandSourceAdapters;
import de.t14d3.rapunzellib.commands.RCommandService;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.SharedBrigadierCommandRegistrar;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import de.t14d3.rapunzellib.commands.core.RCommandTree;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

final class NeoForgeSharedBrigadierCommandRegistrar implements SharedBrigadierCommandRegistrar<CommandSourceStack> {
    private final RCommandService commandService;
    private final CommandSourceAdapters adapters;

    NeoForgeSharedBrigadierCommandRegistrar(
        @NotNull RCommandService commandService,
        @NotNull CommandSourceAdapters adapters
    ) {
        this.commandService = Objects.requireNonNull(commandService, "commandService");
        this.adapters = Objects.requireNonNull(adapters, "adapters");
    }

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.NEOFORGE;
    }

    @Override
    public @NotNull Class<CommandSourceStack> sourceType() {
        return CommandSourceStack.class;
    }

    @Override
    public void registerSharedCommands(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");

        RCommandTree<RCommandSource> sharedTree = commandService.sharedTree();
        for (RCommandNode<RCommandSource> root : sharedTree.getRoots()) {
            LiteralCommandNode<CommandSourceStack> nativeRoot = sharedTree.buildRootMapped(root, dispatcher, adapters::wrap);
            dispatcher.getRoot().addChild(nativeRoot);
            registerAliases(root, nativeRoot, dispatcher);
        }
    }

    private void registerAliases(
        @NotNull RCommandNode<RCommandSource> root,
        @NotNull LiteralCommandNode<CommandSourceStack> nativeRoot,
        @NotNull CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        for (String alias : root.getAliases()) {
            if (alias == null || alias.isBlank()) {
                continue;
            }
            dispatcher.getRoot().addChild(
                LiteralArgumentBuilder.<CommandSourceStack>literal(alias)
                    .requires(nativeRoot.getRequirement())
                    .redirect(nativeRoot)
                    .build()
            );
        }
    }
}
