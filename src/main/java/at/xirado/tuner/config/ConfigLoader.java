package at.xirado.tuner.config;

import at.xirado.tuner.Application;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

/**
 * Utility class to load json files (or copy existing ones from the resource folder)
 */
public class ConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);

    /**
     * Loads a json file into a {@link DataObject DataObject}
     *
     * @param  fileName The relative (or absolute) path of the file
     *
     * @param  copyFromResources Whether the file should be copied from the resource folder if it hasn't been found
     *
     * @return {@link DataObject DataObject} that contains this JSON file's contents
     *
     * @throws IOException
     *         <ul>
     *             <li>If the file in question could not be found, and copyFromResources is false</li>
     *             <li>If the file in question could not be found, copyFromResources is true, and the file could not be found in the resources folder</li>
     *             <li>If the location of the JAR file cannot be parsed into a {@link java.net.URI URI}</li>
     *         </ul>
     */
    @NotNull
    public static DataObject loadFileAsJson(String fileName, boolean copyFromResources) throws IOException {
        File file = new File(fileName);

        if (!file.exists()) {
            if (!copyFromResources)
                throw new FileNotFoundException("File could not be found!");

            InputStream inputStream = Application.class.getResourceAsStream("/" + fileName);

            if (inputStream == null) {
                throw new FileNotFoundException("File \"" + fileName + "\" that was supposed to be copied from the resource folder could not be found!");
            }

            Path path;

            try {
                path = Paths.get(getJarPath() + "/" + fileName);
            } catch (URISyntaxException exception) {
                throw new RuntimeException("Could not parse jar location as URI!", exception);
            }

            Files.copy(inputStream, path);
        }

        return DataObject.fromJson(new FileInputStream(file));
    }

    private static String getJarPath() throws URISyntaxException {
        CodeSource codeSource = Application.class.getProtectionDomain().getCodeSource();
        File jarFile = new File(codeSource.getLocation().toURI().getPath());
        return jarFile.getParentFile().getPath();
    }
}
