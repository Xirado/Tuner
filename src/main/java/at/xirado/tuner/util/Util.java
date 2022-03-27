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

package at.xirado.tuner.util;

import java.util.Locale;

public class Util {

    /**
     * Auto closes AutoClosables
     *
     * @param closeables Closeables
     */
    public static void closeQuietly(AutoCloseable... closeables)
    {
        for (AutoCloseable c : closeables)
        {
            if (c != null)
            {
                try
                {
                    c.close();
                }
                catch (Exception ignored) {}
            }
        }
    }

    public static boolean startsWithIgnoreCase(String string, String prefix) {
        string = string.toLowerCase(Locale.ROOT);
        prefix = prefix.toLowerCase(Locale.ROOT);
        return string.startsWith(prefix);
    }
}
