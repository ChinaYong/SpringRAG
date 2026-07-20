package com.aiplus.spring_rag.service;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

/**
 * 文件分割器，用于将文件内容进行分块，
 */
@Component
public class FileSplitter {

    // 文本分块
    public List<Document> splitText(List<Document> documents) {
        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(1000)
                .withMinChunkLengthToEmbed(5)
                .withMinChunkSizeChars(400)
                .withPunctuationMarks(List.of('.', '?', '!', '\n', ';', ':', '。'))
                .build();

        return textSplitter.split(documents);
    }
}
