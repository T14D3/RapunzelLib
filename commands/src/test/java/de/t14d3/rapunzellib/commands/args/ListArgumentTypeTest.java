package de.t14d3.rapunzellib.commands.args;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ListArgumentTypeTest {
    @Test
    void getListValidatesAndReturnsTypedValues() throws Exception {
        ListArgumentType<String> arg = ListArgumentType.<String>builder()
            .delimiter(",")
            .allowDuplicates(false)
            .values(List.of("a", "b", "c"))
            .build();

        CommandContext<Object> ctx = parse("cmd a,b", arg);
        assertEquals(List.of("a", "b"), arg.getList(ctx, "items"));
    }

    @Test
    void getListRejectsNotAllowedValue() throws Exception {
        ListArgumentType<String> arg = ListArgumentType.<String>builder()
            .delimiter(",")
            .allowDuplicates(true)
            .values(List.of("a", "b"))
            .build();

        CommandContext<Object> ctx = parse("cmd a,c", arg);
        assertThrows(CommandSyntaxException.class, () -> arg.getList(ctx, "items"));
    }

    @Test
    void getListRejectsDuplicatesWhenDisabled() throws Exception {
        ListArgumentType<String> arg = ListArgumentType.<String>builder()
            .delimiter(",")
            .allowDuplicates(false)
            .values(List.of("a", "b"))
            .build();

        CommandContext<Object> ctx = parse("cmd a,a", arg);
        assertThrows(CommandSyntaxException.class, () -> arg.getList(ctx, "items"));
    }

    private static CommandContext<Object> parse(String input, ListArgumentType<String> arg) throws CommandSyntaxException {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(
            LiteralArgumentBuilder.literal("cmd").then(
                RequiredArgumentBuilder.argument("items", arg)
            )
        );
        return dispatcher.parse(input, new Object()).getContext().build(input);
    }
}

