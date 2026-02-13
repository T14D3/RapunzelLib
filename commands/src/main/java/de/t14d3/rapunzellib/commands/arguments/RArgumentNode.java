package de.t14d3.rapunzellib.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import de.t14d3.rapunzellib.commands.RCommandSource;
import de.t14d3.rapunzellib.commands.core.RCommandNode;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Represents a command node that accepts a typed argument.
 * <p>
 * Argument nodes provide type-safe parsing and tab completion suggestions
 * for command arguments. This is a wrapper that holds a reference to the
 * command node and its associated argument metadata.</p>
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * // Create an integer argument node
 * RIntegerArgument<RCommandSource> amountArg = RIntegerArgument.required("amount");
 * RArgumentNode<RCommandSource, Integer> amountNode = RArgumentNode.create(parentNode, amountArg);
 * 
 * // Configure and use in command tree
 * amountNode.getNode()
 *     .executes((source, args) -> {
 *         Integer amount = args.get("amount");
 *         source.sendMessage("Giving " + amount);
 *         return RCommandResult.SUCCESS;
 *     });
 * }</pre>
 * 
 * @param <S> the command source type
 * @param <T> the argument value type
 */
public class RArgumentNode<S extends RCommandSource, T> {
    
    private final RCommandNode<S> node;
    private final RArgument<S, T> argument;
    
    private RArgumentNode(@NotNull RCommandNode<S> node, @NotNull RArgument<S, T> argument) {
        this.node = node;
        this.argument = argument;
    }
    
    /**
     * Creates an argument node by adding a child node to the parent.
     * 
     * @param <S> the command source type
     * @param <T> the argument value type
     * @param parent the parent command node
     * @param argument the argument definition
     * @return a new argument node wrapping the created child node
     */
    public static <S extends RCommandSource, T> RArgumentNode<S, T> create(
            @NotNull RCommandNode<S> parent, 
            @NotNull RArgument<S, T> argument) {
        RCommandNode<S> childNode = parent.then(argument);
        return new RArgumentNode<>(childNode, argument);
    }
    
    /**
     * Gets the command node for this argument.
     * 
     * @return the command node
     */
    @NotNull
    public RCommandNode<S> getNode() {
        return node;
    }
    
    /**
     * Gets the argument definition for this node.
     * 
     * @return the argument definition
     */
    @NotNull
    public RArgument<S, T> getArgument() {
        return argument;
    }
    
    /**
     * Gets the Brigadier argument type for this node.
     * 
     * @return the Brigadier argument type
     */
    @NotNull
    public ArgumentType<T> getArgumentType() {
        return argument.getArgumentType();
    }
    
    /**
     * Gets whether this argument is optional.
     * 
     * @return true if optional
     */
    public boolean isOptional() {
        return argument.isOptional();
    }
    
    /**
     * Gets the default value supplier for this argument.
     * 
     * @return the default value supplier, or null if not set
     */
    @NotNull
    public java.util.function.Supplier<T> getDefaultValue() {
        return argument.getDefaultValue();
    }
    
    /**
     * Gets suggestion providers for tab completion.
     * 
     * @param source the command source
     * @return the list of suggestions
     */
    @NotNull
    public java.util.List<String> getSuggestions(@NotNull S source) {
        return argument.getSuggestions(source);
    }
    
    /**
     * Parses a string value into the argument type.
     * 
     * @param input the string input
     * @return the parsed value
     * @throws IllegalArgumentException if parsing fails
     */
    public T parse(@NotNull String input) throws IllegalArgumentException {
        return argument.parse(input);
    }
    
    /**
     * Validates a parsed value.
     * 
     * @param value the value to validate
     * @return true if valid
     */
    public boolean isValid(@NotNull T value) {
        return argument.isValid(value);
    }
}
