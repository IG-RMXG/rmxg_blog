package com.example.rmxg_blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 文章对应标签
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@TableName("article_tag_list")
@ApiModel(value = "ArticleTagList对象", description = "文章对应标签")
public class ArticleTagList {

    @ApiModelProperty("文章对应标签id")
    private String articleTagListId;

    @ApiModelProperty("文章id")
    private String articleId;

    @ApiModelProperty("文章标签id")
    private String articleTagId;
}
