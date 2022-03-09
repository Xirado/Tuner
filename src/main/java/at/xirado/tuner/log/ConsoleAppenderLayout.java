package at.xirado.tuner.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import com.sun.security.auth.PrincipalComparator;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ConsoleAppenderLayout extends LayoutBase<ILoggingEvent> {

    private static final AttributedStyle PRIMARY = AttributedStyle.DEFAULT.foreground(25, 140, 255);
    private static final AttributedStyle SECONDARY = AttributedStyle.DEFAULT.foreground(85, 85, 85);
    private static final AttributedStyle FATAL = AttributedStyle.DEFAULT.foreground(222, 23, 56);
    private static final AttributedStyle WARN = AttributedStyle.DEFAULT.foreground(255, 255, 0);

    @Override
    public String doLayout(ILoggingEvent event) {
        AttributedStringBuilder stringBuilder = new AttributedStringBuilder();
        Level level = event.getLevel();

        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss");
        String formattedDate = dateTime.format(formatter);

        AttributedStyle priColor;
        if (Level.ERROR.equals(level))
            priColor = FATAL;
        else if (Level.WARN.equals(level))
            priColor = WARN;
        else priColor = PRIMARY;

        stringBuilder.style(SECONDARY).append("[").style(priColor).append(formattedDate).style(SECONDARY).append("] [")
                .style(priColor).append(event.getThreadName()).style(SECONDARY).append("] [").style(priColor).append(event.getLevel().levelStr.toUpperCase(Locale.ROOT))
                .style(SECONDARY).append("]: ").style(priColor).append(event.getFormattedMessage());

        if (event.getThrowableProxy() != null) {
            IThrowableProxy proxy = event.getThrowableProxy();
            stringBuilder.append(CoreConstants.LINE_SEPARATOR).style(priColor).append(ThrowableProxyUtil.asString(proxy));
        }
        stringBuilder.style(AttributedStyle.DEFAULT).append(CoreConstants.LINE_SEPARATOR);
        return stringBuilder.toAnsi();
    }
}
