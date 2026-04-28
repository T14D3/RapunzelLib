package de.t14d3.rapunzellib.multiversion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourcePreprocessor {
    private final String targetVersion;

    public SourcePreprocessor(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public String process(String source) throws IOException {
        SourceProcessingSession session = new SourceProcessingSession(targetVersion);
        try (BufferedReader reader = new BufferedReader(new StringReader(source))) {
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                session.processLine(line, lineNum);
            }

            session.finish(lineNum);
            return session.output();
        }
    }

    private static final class SourceProcessingSession {
        private final String targetVersion;
        private final SourceOutput output = new SourceOutput();
        private final Deque<ConditionalState> conditionStack = new ArrayDeque<>();

        private SourceProcessingSession(String targetVersion) {
            this.targetVersion = targetVersion;
        }

        private void processLine(String line, int lineNum) throws IOException {
            VersionDirective directive = VersionDirective.parse(line);

            if (directive != null) {
                handleDirective(directive, lineNum);
                output.writeDirective(line);
                return;
            }

            if (isIncluded()) {
                String uncommented = CommentedCodeLine.tryUncomment(line);
                if (uncommented != null && !conditionStack.isEmpty()) {
                    output.writeRaw(uncommented);
                } else {
                    output.writeRaw(line);
                }
            } else {
                output.writeCommented(line);
            }
        }

        private void finish(int lineNum) throws IOException {
            if (!conditionStack.isEmpty()) {
                throw new IOException("Line " + lineNum + ": Unclosed #if directive");
            }
        }

        private String output() {
            return output.content();
        }

        private void handleDirective(VersionDirective directive, int lineNum) throws IOException {
            switch (directive.getType()) {
                case IF -> openConditionalBlock(directive);
                case ELSEIF -> handleElseIf(directive, lineNum);
                case ELSE -> handleElse(lineNum);
                case ENDIF -> closeConditionalBlock(lineNum);
            }
        }

        private void openConditionalBlock(VersionDirective directive) {
            boolean parentActive = conditionStack.isEmpty() || conditionStack.peek().active();
            boolean branchActive = parentActive && directive.evaluate(targetVersion);
            conditionStack.push(ConditionalState.open(parentActive, branchActive));
        }

        private void handleElseIf(VersionDirective directive, int lineNum) throws IOException {
            if (conditionStack.isEmpty()) {
                throw new IOException("Line " + lineNum + ": #elseif without matching #if");
            }

            ConditionalState state = conditionStack.pop();
            conditionStack.push(state.advanceToElseIf(directive, targetVersion));
        }

        private void handleElse(int lineNum) throws IOException {
            if (conditionStack.isEmpty()) {
                throw new IOException("Line " + lineNum + ": #else without matching #if");
            }

            ConditionalState state = conditionStack.pop();
            conditionStack.push(state.advanceToElse());
        }

        private void closeConditionalBlock(int lineNum) throws IOException {
            if (conditionStack.isEmpty()) {
                throw new IOException("Line " + lineNum + ": #endif without matching #if");
            }

            conditionStack.pop();
        }

        private boolean isIncluded() {
            for (var iterator = conditionStack.descendingIterator(); iterator.hasNext(); ) {
                if (!iterator.next().active()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class SourceOutput {
        private final StringWriter writer = new StringWriter();

        private void writeRaw(String line) {
            writer.write(line);
            writer.write("\n");
        }

        private void writeCommented(String line) {
            writer.write(getIndentation(line));
            writer.write("//$$ ");
            writer.write(line.stripLeading());
            writer.write("\n");
        }

        private void writeDirective(String line) {
            writeCommented(line);
        }

        private String content() {
            return writer.toString();
        }
    }

    private static String getIndentation(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return line.substring(0, i);
    }

    private static final class CommentedCodeLine {
        private static final Pattern COMMENTED_CODE_PATTERN = Pattern.compile(
                "^(?<indent>[\\t ]*)//\\s*#\\s+(?<code>.*)$"
        );

        private CommentedCodeLine() {
        }

        private static String tryUncomment(String line) {
            Matcher matcher = COMMENTED_CODE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                return null;
            }

            return matcher.group("indent") + matcher.group("code");
        }
    }

    private record ConditionalState(boolean parentActive, boolean active, boolean branchMatched) {

        private static ConditionalState open(boolean parentActive, boolean branchActive) {
            return new ConditionalState(parentActive, branchActive, branchActive);
        }

        private ConditionalState advanceToElseIf(VersionDirective directive, String targetVersion) {
            if (!parentActive) {
                return new ConditionalState(false, false, false);
            }

            if (branchMatched) {
                return new ConditionalState(true, false, true);
            }

            boolean branchActive = directive.evaluate(targetVersion);
            return new ConditionalState(true, branchActive, branchActive);
        }

        private ConditionalState advanceToElse() {
            if (!parentActive || branchMatched) {
                return new ConditionalState(parentActive, false, branchMatched);
            }

            return new ConditionalState(true, true, true);
        }
    }
}
