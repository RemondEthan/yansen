package com.glodon.mordor.yansen.llm.provider;

import com.glodon.mordor.yansen.config.ModelSettings;
import com.glodon.mordor.yansen.llm.ModelProvider;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.OkHttpTransport;

import java.time.Duration;
import java.util.Objects;

/**
 * @author: Remond
 * @date: 2026-06-26
 * @description: {@link ModelProvider} for any OpenAI Chat-Completions-compatible endpoint
 * (MiniMax, DeepSeek, Qwen, Zhipu, OpenAI itself, local proxies, etc.).
 * New vendors are added by changing settings.models[*] — no code change required.
 */
public final class OpenAiCompatibleProvider implements ModelProvider {

    private static final String NAME = "openai-compatible";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Model create(ModelSettings settings) {
        Objects.requireNonNull(settings, "settings");
        requireNonBlank(settings.modelName(), settings.provider(), "modelName");
        requireNonBlank(settings.apiKey(), settings.provider(), "apiKey");
        requireNonBlank(settings.baseUrl(), settings.provider(), "baseUrl");

        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .modelName(settings.modelName())
                .apiKey(settings.apiKey())
                .baseUrl(settings.baseUrl())
                .stream(true);

        HttpTransportConfig transportConfig = buildTransportConfig(settings);
        if (transportConfig != null) {
            builder.httpTransport(new OkHttpTransport(transportConfig));
        }

        GenerateOptions generateOptions = buildGenerateOptions(settings);
        if (generateOptions != null) {
            builder.generateOptions(generateOptions);
        }

        return builder.build();
    }

    private static HttpTransportConfig buildTransportConfig(ModelSettings settings) {
        boolean hasCustom = settings.connectTimeoutSeconds() != null
                || settings.readTimeoutSeconds() != null
                || settings.writeTimeoutSeconds() != null;
        if (!hasCustom) return null;

        var cfgBuilder = HttpTransportConfig.builder();
        if (settings.connectTimeoutSeconds() != null) {
            cfgBuilder.connectTimeout(Duration.ofSeconds(settings.connectTimeoutSeconds()));
        }
        if (settings.readTimeoutSeconds() != null) {
            cfgBuilder.readTimeout(Duration.ofSeconds(settings.readTimeoutSeconds()));
        }
        if (settings.writeTimeoutSeconds() != null) {
            cfgBuilder.writeTimeout(Duration.ofSeconds(settings.writeTimeoutSeconds()));
        }
        return cfgBuilder.build();
    }

    private static GenerateOptions buildGenerateOptions(ModelSettings settings) {
        if (settings.maxRetries() == null) return null;

        return GenerateOptions.builder()
                .executionConfig(ExecutionConfig.builder()
                        .maxAttempts(settings.maxRetries())
                        .build())
                .build();
    }

    private static void requireNonBlank(String value, String provider, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Provider '" + provider + "' requires non-empty '" + field + "' in model settings");
        }
    }
}
