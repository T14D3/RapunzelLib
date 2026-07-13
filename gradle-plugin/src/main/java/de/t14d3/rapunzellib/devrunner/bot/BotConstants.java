package de.t14d3.rapunzellib.devrunner.bot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Constants and helpers for the bot protocol between in-server test plugins and the DevRunner.
 * <p>
 * Messages flow over stdout (server -> DevRunner) and stdin (DevRunner -> server).
 * </p>
 */
public final class BotConstants {

    /** Server prints this to request a bot connection: {@code [BOT_CONNECT] name server} */
    public static final String PREFIX_BOT_CONNECT = "[BOT_CONNECT]";

    /** Server prints this to request a bot disconnect: {@code [BOT_DISCONNECT] name} */
    public static final String PREFIX_BOT_DISCONNECT = "[BOT_DISCONNECT]";

    /** Server prints this to make a bot execute a chat command: {@code [BOT_EXEC] name /command} */
    public static final String PREFIX_BOT_EXEC = "[BOT_EXEC]";

    /** Server prints this to make a bot dig a block: {@code [BOT_DIG] name x y z direction} */
    public static final String PREFIX_BOT_DIG = "[BOT_DIG]";

    /** Server prints this to make a bot use/place an item: {@code [BOT_USE] name x y z hand direction} */
    public static final String PREFIX_BOT_USE = "[BOT_USE]";

    /** DevRunner sends this to confirm a bot is connected: {@code botcallback READY name} */
    // (no matching [BOT_READY] prefix exists; READY is sent as a /botcallback command)

    /** Server prints this to query a bot's position: {@code [BOT_QUERY_POSITION] name} */
    public static final String PREFIX_BOT_QUERY_POSITION = "[BOT_QUERY_POSITION]";

    /** Server prints this to query a bot's health: {@code [BOT_QUERY_HEALTH] name} */
    public static final String PREFIX_BOT_QUERY_HEALTH = "[BOT_QUERY_HEALTH]";

    /** Server prints this to query a bot's held item: {@code [BOT_QUERY_HELD_ITEM] name} */
    public static final String PREFIX_BOT_QUERY_HELD_ITEM = "[BOT_QUERY_HELD_ITEM]";

    /** Server prints this to query a bot's game mode: {@code [BOT_QUERY_GAMEMODE] name} */
    public static final String PREFIX_BOT_QUERY_GAMEMODE = "[BOT_QUERY_GAMEMODE]";

    /** Server prints this to query a bot's open container: {@code [BOT_QUERY_OPEN_CONTAINER] name} */
    public static final String PREFIX_BOT_QUERY_OPEN_CONTAINER = "[BOT_QUERY_OPEN_CONTAINER]";

    /** Server prints this to query entities tracked by the bot: {@code [BOT_QUERY_ENTITIES] name type} */
    public static final String PREFIX_BOT_QUERY_ENTITIES = "[BOT_QUERY_ENTITIES]";

    /** Server prints this to move a bot: {@code [BOT_MOVE_TO] name x y z} */
    public static final String PREFIX_BOT_MOVE_TO = "[BOT_MOVE_TO]";

    /** Server prints this to make a bot attack an entity: {@code [BOT_ATTACK] name entityId} */
    public static final String PREFIX_BOT_ATTACK = "[BOT_ATTACK]";

    /** Server prints this to make a bot interact with an entity: {@code [BOT_INTERACT] name entityId hand} */
    public static final String PREFIX_BOT_INTERACT = "[BOT_INTERACT]";

    /** Server prints this to make a bot swing its hand: {@code [BOT_SWING] name hand} */
    public static final String PREFIX_BOT_SWING = "[BOT_SWING]";

    /** Server prints this to set the bot's held item slot: {@code [BOT_SET_SLOT] name slot} */
    public static final String PREFIX_BOT_SET_SLOT = "[BOT_SET_SLOT]";

    private static final Pattern BOT_LINE_PATTERN =
        Pattern.compile("^\\[(\\w+)]\\s+(.*)$");

    private BotConstants() {
    }

    /**
     * Parses a line of server output for a bot protocol message.
     */
    public static BotCommand parseBotMessage(String line) {
        if (line == null || line.isBlank()) return null;
        String trimmed = line.trim();

        int connectIdx = trimmed.indexOf(PREFIX_BOT_CONNECT);
        if (connectIdx >= 0) {
            String rest = trimmed.substring(connectIdx + PREFIX_BOT_CONNECT.length()).trim();
            int spaceIdx = rest.indexOf(' ');
            if (spaceIdx > 0) {
                String name = rest.substring(0, spaceIdx);
                String server = rest.substring(spaceIdx + 1).trim();
                return new BotCommand("CONNECT", name, server);
            }
            return new BotCommand("CONNECT", rest, "");
        }
        int disconnectIdx = trimmed.indexOf(PREFIX_BOT_DISCONNECT);
        if (disconnectIdx >= 0) {
            String rest = trimmed.substring(disconnectIdx + PREFIX_BOT_DISCONNECT.length()).trim();
            return new BotCommand("DISCONNECT", rest);
        }
        int execIdx = trimmed.indexOf(PREFIX_BOT_EXEC);
        if (execIdx >= 0) {
            String rest = trimmed.substring(execIdx + PREFIX_BOT_EXEC.length()).trim();
            int spaceIdx = rest.indexOf(' ');
            if (spaceIdx > 0) {
                String name = rest.substring(0, spaceIdx);
                String command = rest.substring(spaceIdx + 1).trim();
                return new BotCommand("EXEC", name, command);
            }
            return new BotCommand("EXEC", rest, "");
        }
        int digIdx = trimmed.indexOf(PREFIX_BOT_DIG);
        if (digIdx >= 0) {
            String rest = trimmed.substring(digIdx + PREFIX_BOT_DIG.length()).trim();
            String[] parts = rest.split("\\s+");
            if (parts.length >= 5) {
                return new BotCommand("DIG", parts[0], parts[1], parts[2], parts[3], parts[4]);
            }
            return new BotCommand("DIG", rest);
        }
        int useIdx = trimmed.indexOf(PREFIX_BOT_USE);
        if (useIdx >= 0) {
            String rest = trimmed.substring(useIdx + PREFIX_BOT_USE.length()).trim();
            String[] parts = rest.split("\\s+");
            if (parts.length >= 6) {
                return new BotCommand("USE", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
            }
            return new BotCommand("USE", rest);
        }
        int queryPosIdx = trimmed.indexOf(PREFIX_BOT_QUERY_POSITION);
        if (queryPosIdx >= 0) {
            String rest = trimmed.substring(queryPosIdx + PREFIX_BOT_QUERY_POSITION.length()).trim();
            return new BotCommand("QUERY_POSITION", rest);
        }
        int queryHealthIdx = trimmed.indexOf(PREFIX_BOT_QUERY_HEALTH);
        if (queryHealthIdx >= 0) {
            String rest = trimmed.substring(queryHealthIdx + PREFIX_BOT_QUERY_HEALTH.length()).trim();
            return new BotCommand("QUERY_HEALTH", rest);
        }
        int queryHeldIdx = trimmed.indexOf(PREFIX_BOT_QUERY_HELD_ITEM);
        if (queryHeldIdx >= 0) {
            String rest = trimmed.substring(queryHeldIdx + PREFIX_BOT_QUERY_HELD_ITEM.length()).trim();
            return new BotCommand("QUERY_HELD_ITEM", rest);
        }
        int gmIdx = trimmed.indexOf(PREFIX_BOT_QUERY_GAMEMODE);
        if (gmIdx >= 0) {
            String rest = trimmed.substring(gmIdx + PREFIX_BOT_QUERY_GAMEMODE.length()).trim();
            return new BotCommand("QUERY_GAMEMODE", rest);
        }
        int queryOpenContainerIdx = trimmed.indexOf(PREFIX_BOT_QUERY_OPEN_CONTAINER);
        if (queryOpenContainerIdx >= 0) {
            String rest = trimmed.substring(queryOpenContainerIdx + PREFIX_BOT_QUERY_OPEN_CONTAINER.length()).trim();
            return new BotCommand("QUERY_OPEN_CONTAINER", rest);
        }
        int queryEntitiesIdx = trimmed.indexOf(PREFIX_BOT_QUERY_ENTITIES);
        if (queryEntitiesIdx >= 0) {
            String rest = trimmed.substring(queryEntitiesIdx + PREFIX_BOT_QUERY_ENTITIES.length()).trim();
            int spaceIdx = rest.indexOf(' ');
            if (spaceIdx > 0) {
                String name = rest.substring(0, spaceIdx);
                String type = rest.substring(spaceIdx + 1).trim();
                return new BotCommand("QUERY_ENTITIES", name, type);
            }
            return new BotCommand("QUERY_ENTITIES", rest);
        }
        int moveToIdx = trimmed.indexOf(PREFIX_BOT_MOVE_TO);
        if (moveToIdx >= 0) {
            String rest = trimmed.substring(moveToIdx + PREFIX_BOT_MOVE_TO.length()).trim();
            String[] parts = rest.split("\\s+");
            if (parts.length >= 4) {
                return new BotCommand("MOVE_TO", parts[0], parts[1], parts[2], parts[3]);
            }
            return new BotCommand("MOVE_TO", rest);
        }
        int attackIdx = trimmed.indexOf(PREFIX_BOT_ATTACK);
        if (attackIdx >= 0) {
            String rest = trimmed.substring(attackIdx + PREFIX_BOT_ATTACK.length()).trim();
            String[] parts = rest.split("\\s+");
            if (parts.length >= 2) {
                return new BotCommand("ATTACK", parts[0], parts[1]);
            }
            return new BotCommand("ATTACK", rest);
        }
        int interactIdx = trimmed.indexOf(PREFIX_BOT_INTERACT);
        if (interactIdx >= 0) {
            String rest = trimmed.substring(interactIdx + PREFIX_BOT_INTERACT.length()).trim();
            String[] parts = rest.split("\\s+");
            if (parts.length >= 3) {
                return new BotCommand("INTERACT", parts[0], parts[1], parts[2]);
            }
            return new BotCommand("INTERACT", rest);
        }
        int swingIdx = trimmed.indexOf(PREFIX_BOT_SWING);
        if (swingIdx >= 0) {
            String rest = trimmed.substring(swingIdx + PREFIX_BOT_SWING.length()).trim();
            String[] parts = rest.split("\\s+");
            if (parts.length >= 2) {
                return new BotCommand("SWING", parts[0], parts[1]);
            }
            return new BotCommand("SWING", rest);
        }
        int setSlotIdx = trimmed.indexOf(PREFIX_BOT_SET_SLOT);
        if (setSlotIdx >= 0) {
            String rest = trimmed.substring(setSlotIdx + PREFIX_BOT_SET_SLOT.length()).trim();
            String[] parts = rest.split("\\s+");
            if (parts.length >= 2) {
                return new BotCommand("SET_SLOT", parts[0], parts[1]);
            }
            return new BotCommand("SET_SLOT", rest);
        }
        return null;
    }

    /**
     * A parsed bot command from a server output line.
     */
    public record BotCommand(String type, String botName, String... args) {
    }
}