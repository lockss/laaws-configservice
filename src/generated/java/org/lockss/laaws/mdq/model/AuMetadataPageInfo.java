package org.lockss.laaws.mdq.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.lockss.metadata.ItemMetadata;
import org.lockss.laaws.mdq.model.PageInfo;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * The display page of AU metadata
 */
@ApiModel(description = "The display page of AU metadata")
@Validated

public class AuMetadataPageInfo   {
  @JsonProperty("items")
  @Valid
  private List<ItemMetadata> items = new ArrayList<>();

  @JsonProperty("pageInfo")
  private PageInfo pageInfo = null;

  public AuMetadataPageInfo items(List<ItemMetadata> items) {
    this.items = items;
    return this;
  }

  public AuMetadataPageInfo addItemsItem(ItemMetadata itemsItem) {
    this.items.add(itemsItem);
    return this;
  }

  /**
   * The metadata for the AU items in the page
   * @return items
  **/
  @ApiModelProperty(required = true, value = "The metadata for the AU items in the page")
  @NotNull

  @Valid

  public List<ItemMetadata> getItems() {
    return items;
  }

  public void setItems(List<ItemMetadata> items) {
    this.items = items;
  }

  public AuMetadataPageInfo pageInfo(PageInfo pageInfo) {
    this.pageInfo = pageInfo;
    return this;
  }

  /**
   * Get pageInfo
   * @return pageInfo
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public PageInfo getPageInfo() {
    return pageInfo;
  }

  public void setPageInfo(PageInfo pageInfo) {
    this.pageInfo = pageInfo;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuMetadataPageInfo auMetadataPageInfo = (AuMetadataPageInfo) o;
    return Objects.equals(this.items, auMetadataPageInfo.items) &&
        Objects.equals(this.pageInfo, auMetadataPageInfo.pageInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, pageInfo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuMetadataPageInfo {\n");
    
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    pageInfo: ").append(toIndentedString(pageInfo)).append("\n");
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

