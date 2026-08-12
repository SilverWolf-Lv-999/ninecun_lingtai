package net.seraphina.ninecun_lingtai.api.ai;

import java.net.URI;

public enum AiProvider {
    OPENAI(URI.create("https://api.openai.com/v1/chat/completions")),
    DEEPSEEK(URI.create("https://api.deepseek.com/chat/completions"));

    private final URI chatCompletionsEndpoint;

    AiProvider(URI chatCompletionsEndpoint) {
        this.chatCompletionsEndpoint = chatCompletionsEndpoint;
    }

    public URI getChatCompletionsEndpoint() {
        return this.chatCompletionsEndpoint;
    }
}
