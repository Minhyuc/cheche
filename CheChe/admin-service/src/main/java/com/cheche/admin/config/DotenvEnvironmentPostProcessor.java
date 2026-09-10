package com.cheche.admin.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.*;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path directory = findDotenvDirectory();
        if (directory == null) return;
        Dotenv dotenv = Dotenv.configure().directory(directory.toString())
                .ignoreIfMalformed().ignoreIfMissing().load();
        Map<String, Object> values = new LinkedHashMap<>();
        dotenv.entries().forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        MapPropertySource source = new MapPropertySource("checheDotenv", values);
        MutablePropertySources sources = environment.getPropertySources();
        if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, source);
        } else {
            sources.addFirst(source);
        }
    }

    private Path findDotenvDirectory() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int depth = 0; depth < 5 && directory != null; depth++, directory = directory.getParent()) {
            if (Files.isRegularFile(directory.resolve(".env"))) return directory;
            if (Files.isRegularFile(directory.resolve("CheChe/.env"))) return directory.resolve("CheChe");
        }
        return null;
    }
}
