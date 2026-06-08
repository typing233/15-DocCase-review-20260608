package com.doccase.email.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCreateDTO {
    private String title;
    private String description;
    private List<Long> tagIds;
    private Map<String, Object> metadata;
}
