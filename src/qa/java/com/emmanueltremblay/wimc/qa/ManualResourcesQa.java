package com.emmanueltremblay.wimc.qa;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManualResourcesQa {
    private static final String MOD_ID = "immersive_crate_view_wimc_mod_porting";
    private static final String EDCT_MOD_ID = "immersive_engineer_decor_controls_tool_reforged";
    private static final String LEGACY_EDCT_MOD_ID = "engineers_decor_reforged";
    private static final String EDCT_LABELED_CRATE_ID = EDCT_MOD_ID + ":labeled_crate";
    private static final String LEGACY_EDCT_LABELED_CRATE_ID = LEGACY_EDCT_MOD_ID + ":labeled_crate";
    private static final String IE_MOD_ID = "immersiveengineering";
    private static final Set<String> WIMC_ENTRIES = Set.of(
            "crate_preview_overview",
            "supported_crates",
            "preview_settings"
    );
    private static final Map<String, Set<String>> REQUIRED_ANCHORS = Map.of(
            "crate_preview_overview", Set.of("crate_icons"),
            "supported_crates", Set.of("crate_recipe", "reinforced_crate_recipe", "crate_icons"),
            "preview_settings", Set.of("settings_icon")
    );
    private static final Set<String> ALLOWED_IE_LINKS = Set.of("storage_crates");
    private static final Set<String> ALLOWED_MANUAL_RECIPES = Set.of(
            "immersiveengineering:crafting/crate",
            "immersiveengineering:crafting/reinforced_crate"
    );
    private static final Set<String> ALLOWED_SPECIAL_TYPES = Set.of("crafting", "item_display");
    private static final Pattern ANCHOR_PATTERN = Pattern.compile("<&([A-Za-z0-9_:\\-./]+)>");
    private static final Pattern LINK_PATTERN = Pattern.compile("<link;([^;>]+);([^;>]+)(?:;([^>]+))?>");
    private static final Pattern LOGO_FILE_PATTERN = Pattern.compile("(?m)^\\s*logoFile\\s*=\\s*\"([^\"]+)\"");

    private ManualResourcesQa() {
    }

    public static void main(String[] args) throws IOException {
        Path root = args.length > 0 ? Path.of(args[0]) : Path.of("").toAbsolutePath();
        Path resources = root.resolve("src/main/resources");

        validateModMetadataTemplate(root, resources);
        validateAutoload(resources);
        validateLocalization(resources);
        validateIndustrialCrateTag(resources);
        for (String entry : WIMC_ENTRIES) {
            validateEntry(resources, entry);
        }
    }

    private static void validateModMetadataTemplate(Path root, Path resources) throws IOException {
        Path templatePath = root.resolve("src/main/templates/META-INF/neoforge.mods.toml");
        require(Files.isRegularFile(templatePath), "missing NeoForge metadata template: " + templatePath);

        String template = Files.readString(templatePath);
        Matcher matcher = LOGO_FILE_PATTERN.matcher(template);
        require(matcher.find(), "NeoForge metadata template is missing logoFile");

        String logoFile = matcher.group(1);
        require(
                !logoFile.equals("wimc_logo.png"),
                "metadata logoFile still uses the legacy WIMC filename"
        );
        require(
                Files.isRegularFile(resources.resolve(logoFile)),
                "metadata logoFile does not exist in main resources: " + logoFile
        );
        require(
                template.contains("modId=\"" + EDCT_MOD_ID + "\""),
                "NeoForge metadata template is missing the current EDCT optional dependency"
        );
        require(
                template.contains("modId=\"" + LEGACY_EDCT_MOD_ID + "\""),
                "NeoForge metadata template is missing the legacy EDCT optional dependency"
        );
        requireClientOptionalDependency(template, IE_MOD_ID);
        requireClientOptionalDependency(template, EDCT_MOD_ID);
        requireClientOptionalDependency(template, LEGACY_EDCT_MOD_ID);
    }

    private static void requireClientOptionalDependency(String template, String dependencyModId) {
        String marker = "modId=\"" + dependencyModId + "\"";
        int markerIndex = template.indexOf(marker);
        require(markerIndex >= 0, "NeoForge metadata template is missing dependency " + dependencyModId);

        int nextDependencyIndex = template.indexOf("[[dependencies.", markerIndex + marker.length());
        String dependencyBlock = nextDependencyIndex >= 0
                ? template.substring(markerIndex, nextDependencyIndex)
                : template.substring(markerIndex);
        require(dependencyBlock.contains("type=\"optional\""), dependencyModId + " dependency must be optional");
        require(dependencyBlock.contains("side=\"CLIENT\""), dependencyModId + " dependency must be client-side only");
    }

    private static void validateAutoload(Path resources) throws IOException {
        JsonObject autoload = readJson(resources.resolve("assets/immersiveengineering/manual/autoload.json"));
        require(autoload.has("autoload_priority"), "manual autoload is missing autoload_priority");
        require(autoload.get("autoload_priority").getAsInt() >= 2, "manual autoload should load after IE's base manual");
        require(autoload.has("storage_transport"), "manual autoload must attach under IE storage_transport");
        require(autoload.size() == 2, "manual autoload should only define autoload_priority and storage_transport");

        JsonObject storage = autoload.getAsJsonObject("storage_transport");
        require(storage.has("category_weight"), "storage_transport category stub missing category_weight");
        require(storage.has("entry_list"), "storage_transport category stub missing entry_list");
        require(storage.get("category_weight").getAsInt() == 20, "storage_transport category weight should match IE");
        require(storage.getAsJsonArray("entry_list").isEmpty(), "storage_transport entry_list should not duplicate IE entries");
        String subcategory = MOD_ID + ":factory_inventory_inspector";
        require(storage.has(subcategory), "manual autoload missing WIMC subcategory");

        JsonObject category = storage.getAsJsonObject(subcategory);
        require(category.has("category_weight"), "manual category missing category_weight");
        JsonArray entries = category.getAsJsonArray("entry_list");
        require(entries != null, "manual category missing entry_list");
        require(entries.size() == WIMC_ENTRIES.size(), "manual category should list every WIMC entry exactly once");

        Set<String> seen = new HashSet<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            require(entry.has("source"), "manual entry list object missing source");
            require(entry.has("weight"), "manual entry list object missing weight");
            String source = entry.get("source").getAsString();
            require(source.startsWith(MOD_ID + ":"), "manual entry source must be namespaced to " + MOD_ID + ": " + source);
            String path = source.substring((MOD_ID + ":").length());
            require(WIMC_ENTRIES.contains(path), "unknown WIMC manual entry in autoload: " + source);
            require(seen.add(path), "duplicate WIMC manual entry in autoload: " + source);
        }
        require(seen.equals(WIMC_ENTRIES), "manual autoload does not list the expected WIMC entries");
    }

    private static void validateLocalization(Path resources) throws IOException {
        JsonObject lang = readJson(resources.resolve("assets/" + MOD_ID + "/lang/en_us.json"));
        require(
                lang.has("manual." + MOD_ID + ".factory_inventory_inspector"),
                "missing manual." + MOD_ID + ".factory_inventory_inspector localization"
        );
    }

    private static void validateIndustrialCrateTag(Path resources) throws IOException {
        JsonObject tag = readJson(resources.resolve("data/" + MOD_ID + "/tags/item/industrial_crates.json"));
        JsonArray values = tag.getAsJsonArray("values");
        require(values != null && !values.isEmpty(), "industrial crate tag must list supported crate ids");

        boolean hasCurrentEdctCrate = false;
        boolean hasLegacyEdctCrate = false;
        for (JsonElement value : values) {
            String id;
            if (value.isJsonPrimitive()) {
                id = value.getAsString();
            } else {
                id = requiredString(value.getAsJsonObject(), "id", "industrial crate tag entry");
            }
            hasCurrentEdctCrate |= EDCT_LABELED_CRATE_ID.equals(id);
            hasLegacyEdctCrate |= LEGACY_EDCT_LABELED_CRATE_ID.equals(id);
        }

        require(hasCurrentEdctCrate, "industrial crate tag is missing current EDCT labeled crate id");
        require(!hasLegacyEdctCrate, "industrial crate tag still uses legacy EDCT labeled crate id");
    }

    private static void validateEntry(Path resources, String entry) throws IOException {
        Path jsonPath = resources.resolve("assets/" + MOD_ID + "/manual/" + entry + ".json");
        Path textPath = resources.resolve("assets/" + MOD_ID + "/manual/en_us/" + entry + ".txt");
        JsonObject json = readJson(jsonPath);
        String text = Files.readString(textPath);

        validateTextShape(entry, text);
        validateAnchors(entry, json, text);
        validateLinks(entry, text);
        validateSpecials(entry, json);
        if ("supported_crates".equals(entry)) {
            validateEdctCompatibilityIsTextOnly(json, text);
        }
    }

    private static void validateEdctCompatibilityIsTextOnly(JsonObject json, String text) {
        require(!json.toString().contains(EDCT_LABELED_CRATE_ID), "supported crates manual must not resolve EDCT items directly");
        require(!text.contains("<&edct_labeled_crate_recipe>"), "supported crates manual must not anchor an EDCT recipe");
        require(text.contains("EDCT is loaded"), "supported crates manual must retain the textual EDCT compatibility note");
    }

    private static void validateTextShape(String entry, String text) {
        require(!text.contains("`"), entry + " uses Markdown backticks, which IE manual text does not render as code");
        String[] lines = text.split("\\R", 3);
        require(lines.length == 3, entry + " must contain title, subtitle, and body lines");
        require(!lines[0].isBlank(), entry + " title is blank");
        require(!lines[1].isBlank(), entry + " subtitle is blank");
        require(!lines[2].isBlank(), entry + " body is blank");

        String[] pages = lines[2].split("<np>");
        for (String page : pages) {
            require(page.length() <= 750, entry + " has an oversized manual page segment");
        }
    }

    private static void validateAnchors(String entry, JsonObject json, String text) {
        Set<String> anchors = new HashSet<>();
        Matcher matcher = ANCHOR_PATTERN.matcher(text);
        while (matcher.find()) {
            anchors.add(matcher.group(1));
        }

        Set<String> required = REQUIRED_ANCHORS.get(entry);
        require(required != null, "no required anchor contract for " + entry);
        for (String anchor : required) {
            require(json.has(anchor), entry + " JSON is missing special anchor " + anchor);
            require(anchors.contains(anchor), entry + " text is missing anchor marker <&" + anchor + ">");
        }
    }

    private static void validateLinks(String entry, String text) {
        Matcher matcher = LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            String target = matcher.group(1);
            if (target.startsWith(MOD_ID + ":")) {
                String path = target.substring((MOD_ID + ":").length());
                require(WIMC_ENTRIES.contains(path), entry + " links to unknown WIMC manual entry " + target);
            } else {
                require(ALLOWED_IE_LINKS.contains(target), entry + " links to unexpected IE manual entry " + target);
            }
        }
    }

    private static void validateSpecials(String entry, JsonObject json) {
        for (Map.Entry<String, JsonElement> special : json.entrySet()) {
            require(special.getValue().isJsonObject(), entry + " special " + special.getKey() + " must be an object");
            JsonObject specialObject = special.getValue().getAsJsonObject();
            String type = requiredString(specialObject, "type", entry + " special " + special.getKey());
            require(ALLOWED_SPECIAL_TYPES.contains(type), entry + " special " + special.getKey() + " has unexpected type " + type);

            if ("crafting".equals(type)) {
                String recipe = requiredString(specialObject, "recipe", entry + " crafting special " + special.getKey());
                require(recipe.contains(":"), entry + " recipe reference must be namespaced: " + recipe);
                require(ALLOWED_MANUAL_RECIPES.contains(recipe), entry + " references unexpected manual recipe " + recipe);
            } else if ("item_display".equals(type)) {
                validateItemDisplay(entry, special.getKey(), specialObject);
            }
        }
    }

    private static void validateItemDisplay(String entry, String anchor, JsonObject specialObject) {
        if (specialObject.has("id")) {
            String id = specialObject.get("id").getAsString();
            require(id.contains(":"), entry + " item_display " + anchor + " id must be namespaced");
            return;
        }

        JsonArray items = specialObject.getAsJsonArray("items");
        require(items != null && !items.isEmpty(), entry + " item_display " + anchor + " must list at least one item");
        for (JsonElement item : items) {
            require(item.isJsonPrimitive() && item.getAsJsonPrimitive().isString(), entry + " item_display " + anchor + " item must be a string");
            String id = item.getAsString();
            require(id.contains(":"), entry + " item_display " + anchor + " item must be namespaced: " + id);
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        require(Files.isRegularFile(path), "missing JSON file: " + path);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static String requiredString(JsonObject json, String key, String context) {
        require(json.has(key), context + " is missing " + key);
        require(json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isString(), context + " " + key + " must be a string");
        return json.get(key).getAsString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
