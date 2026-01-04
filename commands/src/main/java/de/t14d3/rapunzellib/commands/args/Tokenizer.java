package de.t14d3.rapunzellib.commands.args;

import java.util.ArrayList;
import java.util.List;

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
