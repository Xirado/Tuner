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

package at.xirado.tuner.util.autocomplete;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

public class BasicAutocompleteChoice implements IAutocompleteChoice {

    private final String name;
    private final String value;

    public BasicAutocompleteChoice(String name, String value) {
        Checks.notNull(name, "Name");
        Checks.notNull(value, "Value");
        this.name = name;
        this.value = value;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getValue() {
        return value;
    }

    @NotNull
    @Override
    public Command.Choice toJDAChoice() {
        return new Command.Choice(name, value);
    }
}
