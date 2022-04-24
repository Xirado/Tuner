/*
 * Copyright 2022 Marcel Korzonek and the Tuner contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.xirado.tuner.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ConsoleAppenderLayout extends LayoutBase<ILoggingEvent> {

    private static final AttributedStyle INFO = AttributedStyle.DEFAULT.foreground(25, 140, 255);
    private static final AttributedStyle ERROR = AttributedStyle.DEFAULT.foreground(222, 23, 56);
    private static final AttributedStyle WARN = AttributedStyle.DEFAULT.foreground(255, 255, 0);
    private static final AttributedStyle DEBUG = AttributedStyle.DEFAULT.foreground(159, 160, 164);
    private static final AttributedStyle SECONDARY = AttributedStyle.DEFAULT.foreground(85, 85, 85);

    @Override
    public String doLayout(ILoggingEvent event) {
        AttributedStringBuilder stringBuilder = new AttributedStringBuilder();
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss");
        String formattedDate = dateTime.format(formatter);

        AttributedStyle priColor = switch (event.getLevel().levelInt) {
            case Level.INFO_INT -> INFO;
            case Level.ERROR_INT -> ERROR;
            case Level.WARN_INT -> WARN;
            default -> DEBUG;
        };

        stringBuilder.style(SECONDARY).append("[").style(priColor).append(formattedDate).style(SECONDARY).append("] [")
                .style(priColor).append(event.getThreadName()).style(SECONDARY).append("] [").style(priColor).append(event.getLevel().levelStr.toUpperCase(Locale.ROOT))
                .style(SECONDARY).append("]");

        stringBuilder.append(": ");

        stringBuilder.style(priColor).append(event.getFormattedMessage());

        if (event.getThrowableProxy() != null) {
            IThrowableProxy proxy = event.getThrowableProxy();
            stringBuilder.append(CoreConstants.LINE_SEPARATOR).style(priColor).append(ThrowableProxyUtil.asString(proxy));
        }
        stringBuilder.style(AttributedStyle.DEFAULT).append(CoreConstants.LINE_SEPARATOR);
        return stringBuilder.toAnsi();
    }
}