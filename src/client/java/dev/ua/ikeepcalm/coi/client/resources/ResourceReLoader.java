package dev.ua.ikeepcalm.coi.client.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NonNull;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class ResourceReLoader extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> implements PreparableReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER_NAME = "jsons";

    private Map<Identifier, JsonElement> loadedData = new HashMap<>();
    public JsonObject ings;
    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, @NonNull ProfilerFiller profiler) {
        Map<Identifier, JsonElement> map = new HashMap<>();

        resourceManager.listResources(FOLDER_NAME, path -> path.getPath().endsWith(".json")).forEach((resourceLocation, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                JsonElement jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (jsonElement != null) {
                    map.put(resourceLocation, jsonElement);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse client JSON resource: " + resourceLocation + " - " + e.getMessage());
            }
        });

        return map;
    }
    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, @NonNull ResourceManager resourceManager, @NonNull ProfilerFiller profiler) {
        this.loadedData = prepared;
        System.out.println("Successfully loaded " + loadedData.size() + " client JSON files!");

        loadedData.forEach((_, element) -> {
            if (element.isJsonObject()) {
                var jsonObject = element.getAsJsonObject();
                setJson(jsonObject);
            }
        });
    }

    private void setJson(JsonObject jsonObject) {
        this.ings = jsonObject;
    }

    public JsonObject getJson() {
        return this.ings;
    }
}