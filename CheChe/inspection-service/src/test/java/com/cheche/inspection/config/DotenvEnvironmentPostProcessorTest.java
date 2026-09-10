package com.cheche.inspection.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;

class DotenvEnvironmentPostProcessorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void findsDotenvInsideNestedCheCheDirectory() throws Exception {
        Path moduleDirectory = Files.createDirectories(tempDirectory.resolve("CheChe"));
        Files.writeString(moduleDirectory.resolve(".env"), "DOTENV_TEST_VALUE=loaded\n");
        String originalUserDirectory = System.getProperty("user.dir");

        try {
            System.setProperty("user.dir", tempDirectory.toString());
            StandardEnvironment environment = new StandardEnvironment();
            new DotenvEnvironmentPostProcessor().postProcessEnvironment(environment, null);
            assertEquals("loaded", environment.getProperty("DOTENV_TEST_VALUE"));
        } finally {
            System.setProperty("user.dir", originalUserDirectory);
        }
    }
}
