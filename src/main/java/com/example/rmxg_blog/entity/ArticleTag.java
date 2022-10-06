package com.example.rmxg_blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 文章标签
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@TableName("article_tag")
@ApiModel(value = "ArticleTag对象", description = "文章标签")
public class ArticleTag {

    @ApiModelProperty("文章标签id")
    private String articleTagId;

    @ApiModelProperty("标签名称")
    private String articleTagName;

    @ApiModelProperty("添加时间")
    private Timestamp articleTagAddTime;
}
