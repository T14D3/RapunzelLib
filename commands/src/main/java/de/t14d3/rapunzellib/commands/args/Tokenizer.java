package de.t14d3.rapunzellib.commands.args;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes command input strings for argument parsing and tab completion suggestions.
 *
 * <p>This class provides a stateful tokenizer that splits input strings into tokens,
 * supporting both quoted and unquoted arguments. It is designed to handle partial
 * input for command suggestion systems, tracking incomplete tokens at the cursor position.
 *
 * <p><strong>Tokenization Strategy:</strong>
 * <ul>
 *   <li>Whitespace separates tokens unless within quotes</li>
 *   <li>Leading whitespace is skipped before parsing begins</li>
 *   <li>Tokens are parsed left-to-right with state tracking</li>
 *   <li>Partial tokens at end of input are preserved for suggestion handling</li>
 * </ul>
 *
 * <p><strong>Supported Quote Styles:</strong>
 * <ul>
 *   <li>Double quotes ({@code "}) - preserves spaces within the quoted text</li>
 *   <li>Single quotes ({@code '}) - preserves spaces within the quoted text</li>
 *   <li>Mixed quotes are not interchangeable; each must be closed with its matching pair</li>
 * </ul>
 *
 * <p><strong>Escape Character Handling:</strong>
 * <p>This tokenizer does <strong>not</strong> currently support escape sequences.
 * Quotes within quoted strings cannot be escaped and will terminate the quote.
 * For literal quotes, use the other quote style as a delimiter.
 *
 * <p>Usage example:
 * <pre>{@code
 * // Tokenize input for suggestions
 * Tokenizer.Tokenization result = Tokenizer.tokenizeForSuggestions("give \"diamond sword\" 64");
 *
 * // Access parsed tokens
 * List<String> tokens = result.tokens(); // ["give", "diamond sword", "64"]
 *
 * // Check for partial token at cursor
 * String partial = result.partial(); // "" (empty if complete)
 * int partialStart = result.partialStart(); // position where partial begins
 * }</pre>
 *
 * <p>Usage example with partial input:
 * <pre>{@code
 * // Incomplete quoted string
 * Tokenizer.Tokenization result = Tokenizer.tokenizeForSuggestions("msg \"Hello");
 * List<String> tokens = result.tokens(); // []
 * String partial = result.partial(); // "Hello"
 * int partialStart = result.partialStart(); // 5 (after the opening quote)
 * }</pre>
 *
 * <p><strong>Implementation note:</strong> This is a utility class with static methods only. The
 * {@link Tokenization} record encapsulates both the parsed tokens and any trailing partial input.
 * @since 1.0
 * @see TokenSuggester
 */
final class Tokenizer {
    private Tokenizer() {
    }

    record Tokenization(List<String> tokens, String partial, int partialStart) {
    }

    static Tokenization tokenizeForSuggestions(String input) {
        if (input == null || input.isEmpty()) {
            return new Tokenization(List.of(), "", 0);
        }

        int length = input.length();
        int index = 0;

        while (index < length && Character.isWhitespace(input.charAt(index))) {
            index++;
        }

        List<String> tokens = new ArrayList<>();
        while (index < length) {
            int tokenStart = index;
            char c = input.charAt(index);

            if (c == '"' || c == '\'') {
                char quote = c;
                index++;
                int contentStart = index;
                boolean closed = false;
                while (index < length) {
                    if (input.charAt(index) == quote) {
                        String token = input.substring(contentStart, index);
                        tokens.add(token);
                        index++; // consume closing quote
                        closed = true;
                        break;
                    }
                    index++;
                }

                if (!closed) {
                    String partial = input.substring(contentStart);
                    return new Tokenization(List.copyOf(tokens), partial, contentStart);
                }
            } else {
                while (index < length && !Character.isWhitespace(input.charAt(index))) {
                    index++;
                }

                if (index >= length) {
                    String partial = input.substring(tokenStart);
                    return new Tokenization(List.copyOf(tokens), partial, tokenStart);
                }

                String token = input.substring(tokenStart, index);
                tokens.add(token);
            }

            while (index < length && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        return new Tokenization(List.copyOf(tokens), "", length);
    }
}
