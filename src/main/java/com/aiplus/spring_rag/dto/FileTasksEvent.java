package com.aiplus.spring_rag.dto;

import java.util.Set;

public record FileTasksEvent(
        Set<String> tasks) {

}
