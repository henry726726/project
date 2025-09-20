package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // snake_case JSON -> camelCase 필드 자동 매핑
public class ComposeResponse {
  private String imageBase64;
  private Map<String, Object> layout;
  private Map<String, Object> copy;
  private Map<String, Object> meta;

  public String getImageBase64() { return imageBase64; }
  public void setImageBase64(String v) { this.imageBase64 = v; }

  public Map<String, Object> getLayout() { return layout; }
  public void setLayout(Map<String, Object> v) { this.layout = v; }

  public Map<String, Object> getCopy() { return copy; }
  public void setCopy(Map<String, Object> v) { this.copy = v; }

  public Map<String, Object> getMeta() { return meta; }
  public void setMeta(Map<String, Object> v) { this.meta = v; }
}
