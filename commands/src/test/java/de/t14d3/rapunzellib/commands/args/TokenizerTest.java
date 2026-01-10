package de.t14d3.rapunzellib.commands.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TokenizerTest {
    @Test
    void tokenizesTrailingPartial() {
        Tokenizer.Tokenization t = Tokenizer.tokenizeForSuggestions("foo bar");
        assertEquals(java.util.List.of("foo"), t.tokens());
        assertEquals("bar", t.partial());
        assertEquals(4, t.partialStart());
    }

    @Test
    void tokenizesCompletedTokens() {
        Tokenizer.Tokenization t = Tokenizer.tokenizeForSuggestions("foo ");
        assertEquals(java.util.List.of("foo"), t.tokens());
        assertEquals("", t.partial());
        assertEquals(4, t.partialStart());
    }

    @Test
    void tokenizesQuotedTokenAndPartial() {
        Tokenizer.Tokenization t = Tokenizer.tokenizeForSuggestions("\"hello world\" baz");
        assertEquals(java.util.List.of("hello world"), t.tokens());
        assertEquals("baz", t.partial());
        assertEquals(14, t.partialStart());
    }

    @Test
    void tokenizesUnclosedQuoteAsPartial() {
        Tokenizer.Tokenization t = Tokenizer.tokenizeForSuggestions("\"unterminated");
        assertEquals(java.util.List.of(), t.tokens());
        assertEquals("unterminated", t.partial());
        assertEquals(1, t.partialStart());
    }
}

