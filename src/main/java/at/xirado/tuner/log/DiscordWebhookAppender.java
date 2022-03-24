package at.xirado.tuner.log;

import at.xirado.tuner.Application;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import club.minnced.discord.webhook.WebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class DiscordWebhookAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final String GREY = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String WHITE = "\u001B[37m";
    private static final String RESET = "\u001B[0m";

    private boolean active;
    private Application application;

    private final List<String> pendingMessages = new ArrayList<>();
    private final ReentrantLock webhookLock = new ReentrantLock();
    private final int emptyLength = getWebhookMessageLength(Collections.emptyList());
    private final Thread webhookThread;

    public DiscordWebhookAppender() {
        webhookThread = new Thread(() -> {
            while (Application.getApplication() == null || Application.getApplication().getTunerConfiguration() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
            if (!active) {
                application = Application.getApplication();
                if (application.getTunerConfiguration().getWebhookClient() == null) {
                    LOG.warn("Disabling Discord Webhook Appender because no url has been specified!");
                    return;
                }
                active = true;
            }
            int state = 0;
            long waitingTime = 0;
            while (true) {
                webhookLock.lock();
                int size = pendingMessages.size();
                webhookLock.unlock();
                if (size > 0 && state != 1) {
                    state = 1; // Waiting
                    waitingTime = System.currentTimeMillis();
                }

                if (state == 1 && System.currentTimeMillis() > waitingTime + 3000) {
                    webhookLock.lock();
                    var pack = splitUp(pendingMessages);
                    pack.forEach(this::sendWebhook);
                    pendingMessages.clear();
                    webhookLock.unlock();
                    state = 0;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        });

        webhookThread.setDaemon(true);
        webhookThread.setName("Discord Webhook Logger Thread");
        webhookThread.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!active)
            return;

        webhookLock.lock();
        pendingMessages.add(formatted(event));
        webhookLock.unlock();
    }

    private String formatted(ILoggingEvent event) {
        String formattedMessage = event.getFormattedMessage();
        if (formattedMessage.length() > 1800)
            formattedMessage = "[!] Message too long";

        Level level = event.getLevel();

        String primary;

        if (level.equals(Level.WARN))
            primary = YELLOW;
        else if (level.equals(Level.ERROR))
            primary = RED;
        else if (level.equals(Level.INFO))
            primary = BLUE;
        else
            primary = WHITE;

        StringBuilder builder = new StringBuilder().append(GREY).append("[").append(primary).append(level.levelStr.toUpperCase()).append(GREY)
                .append("] ").append(primary).append(formattedMessage).append('\n');

        if (event.getThrowableProxy() != null) {
            IThrowableProxy proxy = event.getThrowableProxy();
            String result = ThrowableProxyUtil.asString(proxy).length() > 1500 ? "     Too long!\n" : ThrowableProxyUtil.asString(proxy) + "\n";
            builder.append(result).append(RESET);
        }

        return builder.toString();
    }

    private void sendWebhook(List<String> logs)
    {
        WebhookClient client = application.getTunerConfiguration().getWebhookClient();

        StringBuilder result = new StringBuilder("```ansi\n");
        logs.forEach(result::append);
        result.append("```");
        client.send(result.toString().trim());
        logs.clear();
    }

    private int getWebhookMessageLength(List<String> logs)
    {
        StringBuilder result = new StringBuilder("```ansi\n");
        logs.forEach(result::append);
        result.append("```");
        return result.length();
    }

    /**
     * Splits up a List of Strings into smaller Lists that are smaller than 2000 characters in length
     *
     * @param input The input list
     * @return a List of Lists with a resulting length smaller than 2000 characters
     */
    private List<List<String>> splitUp(List<String> input)
    {
        List<List<String>> output = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentSize = emptyLength; // Including the char count of the code block (should be 11)
        int index = 0;
        for (int i = 0; i < input.size(); i++)
        {
            if (currentSize + input.get(i).length() <= 2000)
            {
                currentSize += input.get(i).length();
                current.add(input.get(i));
                continue;
            }
            output.add(new ArrayList<>(current));
            current.clear();
            current.add(input.get(i));
            currentSize = input.get(i).length();
            index = i;
        }
        List<String> lastIter = new ArrayList<>();
        for (int i = index; i < input.size(); i++)
            lastIter.add(input.get(i));
        output.add(lastIter);
        return output;
    }
}
