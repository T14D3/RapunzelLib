package de.t14d3.rapunzellib.common.message;

import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.context.ResourceProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class YamlMessageFormatServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlMessageFormatServiceTest.class);

    @Test
    void parsesStandardMiniMessageTags(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("messages.yml");
        Files.writeString(
            file,
            "prefix: \"\"\n" +
                "test: \"<red>Hi</red>\"\n",
            StandardCharsets.UTF_8
        );

        ResourceProvider resources = path -> Optional.empty();
        ConfigService configService = new SnakeYamlConfigService(resources, LOGGER);
        YamlMessageFormatService service = new YamlMessageFormatService(configService, LOGGER, file, null);

        Component component = service.component("test");
        TextComponent text = assertInstanceOf(TextComponent.class, component);  

        assertEquals("Hi", text.content());
        assertEquals(NamedTextColor.RED, text.style().color());
    }

    @Test
    void replacesAngleBracketPlaceholders(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("messages.yml");
        Files.writeString(
            file,
            "prefix: \"\"\n" +
                "test: \"<light_purple>Name: </light_purple><gold><name>\"\n",
            StandardCharsets.UTF_8
        );

        ResourceProvider resources = path -> Optional.empty();
        ConfigService configService = new SnakeYamlConfigService(resources, LOGGER);
        YamlMessageFormatService service = new YamlMessageFormatService(configService, LOGGER, file, null);

        Component component = service.component(
            "test",
            de.t14d3.rapunzellib.message.Placeholders.builder().string("name", "bob").build()
        );

        TextComponent root = assertInstanceOf(TextComponent.class, component);
        assertEquals(2, root.children().size());

        TextComponent nameLabel = assertInstanceOf(TextComponent.class, root.children().get(0));
        TextComponent nameValue = assertInstanceOf(TextComponent.class, root.children().get(1));

        assertEquals("Name: ", nameLabel.content());
        assertEquals(NamedTextColor.LIGHT_PURPLE, nameLabel.style().color());
        assertEquals("bob", nameValue.content());
        assertEquals(NamedTextColor.GOLD, nameValue.style().color());
    }
}
