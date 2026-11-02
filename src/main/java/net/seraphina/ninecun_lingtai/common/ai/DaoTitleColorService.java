package net.seraphina.ninecun_lingtai.common.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.ai.AiProvider;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleColors;
import net.seraphina.ninecun_lingtai.common.daotitle.DaoTitleManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerDaoTitleData;
import net.seraphina.ninecun_lingtai.utility.AiConfigUtility;
import net.seraphina.ninecun_lingtai.utility.AiResponseUtility;
import net.seraphina.ninecun_lingtai.utility.DaoTitleUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class DaoTitleColorService {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15L))
            .build();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ninecun-lingtai-dao-title-color");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<UUID, AtomicLong> VERSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, ScheduledFuture<?>> PENDING = new ConcurrentHashMap<>();

    private DaoTitleColorService() {
    }

    public static void schedule(ServerPlayer player) {
        UUID playerId = player.getUUID();
        String title = DaoTitleUtility.getDisplayTitle(player);
        if (!title.isBlank()) {
            DaoTitleColors pendingColors = fallbackColors(title);
            PlayerDaoTitleData.setColors(player, pendingColors);
            player.refreshDisplayName();
            player.refreshTabListName();
            DaoTitleManager.syncToAll(player);
            ModMain.LOGGER.info("Requested dao title colors for {}", player.getGameProfile().getName());
        }
        AtomicLong version = VERSIONS.computeIfAbsent(playerId, ignored -> new AtomicLong());
        long requestVersion = version.incrementAndGet();
        ScheduledFuture<?> previous = PENDING.put(playerId, SCHEDULER.schedule(
                () -> request(player, requestVersion, title),
                300L,
                TimeUnit.MILLISECONDS
        ));
        if (previous != null) {
            previous.cancel(false);
        }
    }

    private static void request(ServerPlayer player, long requestVersion, String title) {
        UUID playerId = player.getUUID();
        AtomicLong version = VERSIONS.get(playerId);
        if (version == null || version.get() != requestVersion) {
            return;
        }
        if (title.isBlank()) {
            return;
        }
        requestColors(title).whenComplete((colors, throwable) -> player.server.execute(() -> {
            AtomicLong currentVersion = VERSIONS.get(playerId);
            if (currentVersion == null || currentVersion.get() != requestVersion) {
                return;
            }
            ServerPlayer onlinePlayer = player.server.getPlayerList().getPlayer(playerId);
            if (onlinePlayer == null) {
                return;
            }
            DaoTitleColors appliedColors = colors;
            if (throwable != null || colors == null) {
                ModMain.LOGGER.warn("Failed to generate dao title colors for {}", onlinePlayer.getGameProfile().getName(), throwable);
                appliedColors = fallbackColors(title);
            }
            PlayerDaoTitleData.setColors(onlinePlayer, appliedColors);
            ModMain.LOGGER.info("Applied dao title colors for {}: {} -> {}", onlinePlayer.getGameProfile().getName(), appliedColors.firstColor(), appliedColors.secondColor());
            onlinePlayer.refreshDisplayName();
            onlinePlayer.refreshTabListName();
            DaoTitleManager.syncToAll(onlinePlayer);
            PENDING.remove(playerId);
        }));
    }

    private static CompletableFuture<DaoTitleColors> requestColors(String title) {
        String apiKey;
        String model;
        try {
            apiKey = AiConfigUtility.requireApiKey();
            model = AiConfigUtility.requireModel();
        } catch (IllegalStateException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        AiProvider provider = AiConfigUtility.getProvider();
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        JsonArray messages = new JsonArray();
        messages.add(createMessage(
                "system",
                "你是 Minecraft 道号颜色设计器。根据玩家道号的意境设计一组适合深色背景文本显示的二元渐变颜色。必须只返回 JSON，不要 Markdown，不要解释。JSON 结构必须是 {\"firstColor\":\"#RRGGBB\",\"secondColor\":\"#RRGGBB\"}。两个颜色必须不同，必须是六位十六进制 RGB。"
        ));
        messages.add(createMessage("user", "玩家道号：" + title));
        requestBody.add("messages", messages);
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);
        HttpRequest request = HttpRequest.newBuilder(provider.getChatCompletionsEndpoint())
                .timeout(Duration.ofSeconds(60L))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(DaoTitleColorService::parseResponse);
    }

    private static DaoTitleColors fallbackColors(String title) {
        return switch (Math.floorMod(title.hashCode(), 6)) {
            case 0 -> new DaoTitleColors(0xD85C7A, 0xF5C16C);
            case 1 -> new DaoTitleColors(0x5C8DDE, 0x8BDBFF);
            case 2 -> new DaoTitleColors(0x7A63D8, 0xD58BFF);
            case 3 -> new DaoTitleColors(0x42A889, 0xB7E36D);
            case 4 -> new DaoTitleColors(0xD47B43, 0xFFE08A);
            default -> new DaoTitleColors(0x4CB7B7, 0xB6F0D0);
        };
    }

    private static JsonObject createMessage(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static DaoTitleColors parseResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = response.body();
            if (body.length() > 512) {
                body = body.substring(0, 512);
            }
            throw new IllegalStateException("AI color request failed with status " + response.statusCode() + ": " + body);
        }
        JsonObject responseObject = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = responseObject.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("AI color response has no choices");
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            throw new IllegalStateException("AI color response has no message content");
        }
        String content = message.get("content").getAsString();
        JsonObject colors = JsonParser.parseString(AiResponseUtility.removeMarkdownFence(content.trim())).getAsJsonObject();
        int firstColor = parseColor(colors.has("firstColor") ? colors.get("firstColor") : colors.get("first_color"));
        int secondColor = parseColor(colors.has("secondColor") ? colors.get("secondColor") : colors.get("second_color"));
        if (firstColor == secondColor) {
            throw new IllegalStateException("AI returned identical colors");
        }
        return new DaoTitleColors(firstColor, secondColor);
    }

    private static int parseColor(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalStateException("AI color is missing");
        }
        String value = element.getAsString().trim();
        if (!value.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalStateException("AI returned invalid color");
        }
        return Integer.parseInt(value.substring(1), 16);
    }

}
