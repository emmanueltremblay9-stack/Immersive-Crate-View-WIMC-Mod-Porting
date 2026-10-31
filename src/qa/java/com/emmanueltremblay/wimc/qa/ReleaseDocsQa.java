package com.emmanueltremblay.wimc.qa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReleaseDocsQa {
    private static final String MOD_ID = "immersive_crate_view_wimc_mod_porting";
    private static final String CURRENT_INDUSTRIAL_CRATES_TAG = MOD_ID + ":industrial_crates";
    private static final String LEGACY_INDUSTRIAL_CRATES_TAG = "wimc:industrial_crates";
    private static final String CURRENT_LOGO = "src/main/resources/" + MOD_ID + "_logo.png";
    private static final String LEGACY_LOGO = "src/main/resources/wimc_logo.png";
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*]\\(([^)]+)\\)");

    private ReleaseDocsQa() {
    }

    public static void main(String[] args) throws IOException {
        Path root = args.length > 0 ? Path.of(args[0]) : Path.of("").toAbsolutePath();
        Map<String, String> properties = readProperties(root.resolve("gradle.properties"));
        String modName = requireProperty(properties, "mod_name");
        String modVersion = requireProperty(properties, "mod_version");

        validateReadme(root);
        validateCurseForgeChangelog(root, modName, modVersion);
    }

    private static void validateReadme(Path root) throws IOException {
        Path readme = root.resolve("README.md");
        String text = read(readme);

        require(text.contains(CURRENT_INDUSTRIAL_CRATES_TAG), "README is missing current tag " + CURRENT_INDUSTRIAL_CRATES_TAG);
        require(!text.contains(LEGACY_INDUSTRIAL_CRATES_TAG), "README still references legacy tag " + LEGACY_INDUSTRIAL_CRATES_TAG);
        require(text.contains(CURRENT_LOGO), "README is missing current logo path " + CURRENT_LOGO);
        require(!text.contains(LEGACY_LOGO), "README still references deleted legacy logo path " + LEGACY_LOGO);

        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(text);
        while (matcher.find()) {
            String imagePath = matcher.group(1);
            if (!imagePath.startsWith("http://") && !imagePath.startsWith("https://")) {
                require(Files.isRegularFile(root.resolve(imagePath)), "README image path does not exist: " + imagePath);
            }
        }
    }

    private static void validateCurseForgeChangelog(Path root, String modName, String modVersion) throws IOException {
        Path changelog = root.resolve("docs/curseforge_changelog.md");
        String text = read(changelog);

        require(text.startsWith("# " + modName + " " + modVersion), "CurseForge changelog title must match mod_name and mod_version");
        require(text.contains(CURRENT_INDUSTRIAL_CRATES_TAG), "CurseForge changelog is missing current tag " + CURRENT_INDUSTRIAL_CRATES_TAG);
        require(!text.contains(LEGACY_INDUSTRIAL_CRATES_TAG), "CurseForge changelog still references legacy tag " + LEGACY_INDUSTRIAL_CRATES_TAG);
        require(!text.contains("# " + modName + " 0.1.0"), "CurseForge changelog still advertises stale 0.1.0 release title");
        require(!Files.exists(root.resolve("docs/curseforge_changelog_0.1.0.md")), "stale versioned CurseForge changelog path still exists");
    }

    private static Map<String, String> readProperties(Path path) throws IOException {
        String text = read(path);
        Map<String, String> properties = new HashMap<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            properties.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
        }
        return properties;
    }

    private static String requireProperty(Map<String, String> properties, String key) {
        String value = properties.get(key);
        require(value != null && !value.isBlank(), "gradle.properties is missing " + key);
        return value;
    }

    private static String read(Path path) throws IOException {
        require(Files.isRegularFile(path), "missing release doc: " + path);
        return Files.readString(path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
