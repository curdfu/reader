package com.htmake.reader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.htmake.reader.utils.BookSourceTypeFilter;
import io.legado.app.data.entities.BookSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookSourceImportFilterTests {

    @Test
    public void textTypeIncludesNullAndZero() {
        assertTrue(BookSourceTypeFilter.INSTANCE.isSupportedTextType(null));
        assertTrue(BookSourceTypeFilter.INSTANCE.isSupportedTextType(0));
    }

    @Test
    public void unsupportedTypesHaveStableBucketsAndReasons() {
        assertUnsupportedType(1, "audio", "audio source unsupported");
        assertUnsupportedType(2, "image", "image source unsupported");
        assertUnsupportedType(3, "file", "file source unsupported");
        assertUnsupportedType(99, "unknown", "unknown source type unsupported");
        assertUnsupportedType(-1, "unknown", "unknown source type unsupported");
    }

    @Test
    public void mixedTypeSampleKeepsOnlyTextSources() throws Exception {
        JsonArray sources = readMixedTypeSample();
        Gson gson = new Gson();
        int textSources = 0;
        int skippedSources = 0;
        Map<String, Integer> skippedByType = new HashMap<>();
        skippedByType.put("audio", 0);
        skippedByType.put("image", 0);
        skippedByType.put("file", 0);
        skippedByType.put("unknown", 0);

        for (JsonElement element : sources) {
            BookSource source = gson.fromJson(element, BookSource.class);
            if (BookSourceTypeFilter.INSTANCE.isSupportedTextType(source.getBookSourceType())) {
                textSources++;
                continue;
            }

            skippedSources++;
            String bucket = BookSourceTypeFilter.INSTANCE.bucket(source.getBookSourceType());
            skippedByType.put(bucket, skippedByType.get(bucket) + 1);
        }

        assertEquals(4, sources.size());
        assertEquals(2, textSources);
        assertEquals(2, skippedSources);
        assertEquals(Integer.valueOf(1), skippedByType.get("audio"));
        assertEquals(Integer.valueOf(1), skippedByType.get("image"));
        assertEquals(Integer.valueOf(0), skippedByType.get("file"));
        assertEquals(Integer.valueOf(0), skippedByType.get("unknown"));
    }

    private static void assertUnsupportedType(Integer type, String bucket, String reason) {
        assertFalse(BookSourceTypeFilter.INSTANCE.isSupportedTextType(type));
        assertEquals(bucket, BookSourceTypeFilter.INSTANCE.bucket(type));
        assertEquals(reason, BookSourceTypeFilter.INSTANCE.reason(type));
    }

    private static JsonArray readMixedTypeSample() throws Exception {
        InputStream stream = BookSourceImportFilterTests.class
                .getResourceAsStream("/booksource/mixed_types.json");
        assertNotNull(stream);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonArray();
        }
    }
}
