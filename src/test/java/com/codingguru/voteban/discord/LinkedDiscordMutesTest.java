package com.codingguru.voteban.discord;

import static org.junit.jupiter.api.Assertions.*;

import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.MessageAction;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class LinkedDiscordMutesTest {
    @Test
    void sendsOnceAsynchronouslyWithLiteralTextLineBreaksAndNoMentionNotifications() {
        Capture capture = new Capture();
        LinkedDiscordMutes.sendAnnouncement(capture.channel,
                "§eVote to mute §aSome_Name§e for 20m\\\\n§eReason: **spam** @everyone <@123456789012345678> $1", capture.logger);

        assertEquals("Vote to mute Some_Name for 20m\nReason: \\*\\*spam\\*\\* @everyone <@123456789012345678> $1", capture.content);
        assertEquals(1, capture.sends);
        assertEquals(1, capture.queues);
        assertNotNull(capture.mentions);
        assertTrue(capture.mentions.isEmpty());
        assertNull(capture.warning);
    }

    @Test
    void alsoHandlesSingleEscapedAndActualLineBreaksAndSkipsBlankNotices() {
        Capture capture = new Capture();
        LinkedDiscordMutes.sendAnnouncement(capture.channel, "first\\nsecond\nthird", capture.logger);
        assertEquals("first\nsecond\nthird", capture.content);
        LinkedDiscordMutes.sendAnnouncement(capture.channel, "§e  ", capture.logger);
        assertEquals(1, capture.sends);
    }

    @Test
    void failedAsynchronousSendIsReported() {
        Capture capture = new Capture();
        LinkedDiscordMutes.sendAnnouncement(capture.channel, "Vote failed", capture.logger);
        capture.failure.accept(new IllegalStateException("Discord disconnected"));
        assertTrue(capture.warning.contains("Discord disconnected"));
    }

    @Test
    void synchronousDiscordFailureDoesNotInterruptTheVote() {
        Capture capture = new Capture();
        TextChannel denied = (TextChannel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {TextChannel.class}, (proxy, method, args) -> {
                    throw new IllegalStateException("Missing send permission");
                });
        assertDoesNotThrow(() -> LinkedDiscordMutes.sendAnnouncement(denied, "Vote passed", capture.logger));
        assertTrue(capture.warning.contains("Missing send permission"));
    }

    private static final class Capture {
        String content;
        String warning;
        int sends;
        int queues;
        Collection<?> mentions;
        Consumer<Throwable> failure;
        final Logger logger = Logger.getAnonymousLogger();
        final MessageAction action = (MessageAction) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {MessageAction.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "allowedMentions" -> mentions = (Collection<?>) args[0];
                        case "queue" -> {
                            queues++;
                            @SuppressWarnings("unchecked")
                            Consumer<Throwable> callback = (Consumer<Throwable>) args[1];
                            failure = callback;
                            return null;
                        }
                        default -> throw new AssertionError("Unexpected/blocking action: " + method);
                    }
                    return proxy;
                });
        final TextChannel channel = (TextChannel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {TextChannel.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("sendMessage")) throw new AssertionError(method);
                    sends++;
                    content = args[0].toString();
                    return action;
                });

        Capture() {
            logger.setUseParentHandlers(false);
            logger.addHandler(new Handler() {
                public void publish(LogRecord record) { warning = record.getMessage(); }
                public void flush() {}
                public void close() {}
            });
        }
    }
}
