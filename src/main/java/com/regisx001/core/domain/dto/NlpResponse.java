package com.regisx001.core.domain.dto;

import java.util.List;

public record NlpResponse(String intent, List<String> entities) {
}