package org.lockss.laaws.mdq.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * The information related to a resulting list of URLs
 */
@ApiModel(description = "The information related to a resulting list of URLs")
@Validated

public class UrlInfo   {
  @JsonProperty("params")
  @Valid
  private Map<String, String> params = new HashMap<>();

  @JsonProperty("urls")
  @Valid
  private List<String> urls = new ArrayList<>();

  public UrlInfo params(Map<String, String> params) {
    this.params = params;
    return this;
  }

  public UrlInfo putParamsItem(String key, String paramsItem) {
    this.params.put(key, paramsItem);
    return this;
  }

  /**
   * The parameters that define the resulting URLs
   * @return params
  **/
  @ApiModelProperty(required = true, value = "The parameters that define the resulting URLs")
  @NotNull


  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  public UrlInfo urls(List<String> urls) {
    this.urls = urls;
    return this;
  }

  public UrlInfo addUrlsItem(String urlsItem) {
    this.urls.add(urlsItem);
    return this;
  }

  /**
   * The URLs
   * @return urls
  **/
  @ApiModelProperty(required = true, value = "The URLs")
  @NotNull


  public List<String> getUrls() {
    return urls;
  }

  public void setUrls(List<String> urls) {
    this.urls = urls;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UrlInfo urlInfo = (UrlInfo) o;
    return Objects.equals(this.params, urlInfo.params) &&
        Objects.equals(this.urls, urlInfo.urls);
  }

  @Override
  public int hashCode() {
    return Objects.hash(params, urls);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UrlInfo {\n");
    
    sb.append("    params: ").append(toIndentedString(params)).append("\n");
    sb.append("    urls: ").append(toIndentedString(urls)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

