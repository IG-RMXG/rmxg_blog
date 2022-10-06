package com.example.rmxg_blog.entity;

import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 文章
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@ApiModel(value = "Article对象", description = "文章")
public class Article {

    @ApiModelProperty("文章id")
    private String articleId;

    @ApiModelProperty("文章分类id")
    private String articleTypeId;

    @ApiModelProperty("用户id")
    private String userId;

    @ApiModelProperty("文章封面")
    private String articleCoverUrl;

    @ApiModelProperty("文章标题")
    private String articleTitle;

    @ApiModelProperty("文章添加时间")
    private Timestamp articleAddTime;

    @ApiModelProperty("文章内容")
    private String articleContext;

    @ApiModelProperty("点赞次数")
    private Integer articleGoodNumber;

    @ApiModelProperty("观看次数")
    private Integer articleLookNumber;

    @ApiModelProperty("是否是热门文章 0否，1是")
    private Integer articleHot;

    @ApiModelProperty("收藏次数")
    private Integer articleCollectionNumber;
}
