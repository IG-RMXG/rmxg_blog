package com.example.rmxg_blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 文章分类i
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@TableName("article_type")
@ApiModel(value = "ArticleType对象", description = "文章分类i")
public class ArticleType {

    @ApiModelProperty("文章分类id")
    private String articleTypeId;

    @ApiModelProperty("文章分类父id")
    private String articleTypeParentId;

    @ApiModelProperty("文章分类名称")
    private String articleTypeName;

    @ApiModelProperty("文章分类排序，越小越靠前")
    private Integer articleTypeSort;

    @ApiModelProperty("添加时间")
    private Timestamp articleTypeAddTime;
}
