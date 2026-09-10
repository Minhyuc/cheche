package com.cheche.login.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenvDirectory = findDotenvDirectory();
        if (dotenvDirectory == null) return;

        Dotenv dotenv = Dotenv.configure()
                .directory(dotenvDirectory.toString())
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
        Map<String, Object> values = new LinkedHashMap<>();
        dotenv.entries().forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        addAfterSystemEnvironment(environment.getPropertySources(), values);
    }

    private Path findDotenvDirectory() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int depth = 0; depth < 5 && directory != null; depth++, directory = directory.getParent()) {
            if (Files.isRegularFile(directory.resolve(".env"))) return directory;
            if (Files.isRegularFile(directory.resolve("CheChe/.env"))) return directory.resolve("CheChe");
        }
        return null;
    }

    private void addAfterSystemEnvironment(MutablePropertySources sources, Map<String, Object> values) {
        MapPropertySource dotenv = new MapPropertySource("checheDotenv", values);
        if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, dotenv);
        } else {
            sources.addFirst(dotenv);
        }
    }
}
