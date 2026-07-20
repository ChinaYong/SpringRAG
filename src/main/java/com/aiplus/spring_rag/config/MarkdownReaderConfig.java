package com.aiplus.spring_rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;

@Configuration
public class MarkdownReaderConfig {

    @Bean("markdownDocumentReaderConfig")
    public MarkdownDocumentReaderConfig markdownDocumentReader() {

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(false)
                .withIncludeBlockquote(true)
                .withIncludeCodeBlock(true)
                .build();

        return config;
    }
}
