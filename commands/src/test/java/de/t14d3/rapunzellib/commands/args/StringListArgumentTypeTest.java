package de.t14d3.rapunzellib.commands.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StringListArgumentTypeTest {
    @Test
    void parsesQuotedStrings() throws CommandSyntaxException {
        StringReader reader = new StringReader("foo \"hello world\" baz");
        List<String> out = StringListArgumentType.stringList().parse(reader);
        assertEquals(List.of("foo", "hello world", "baz"), out);
    }
}

