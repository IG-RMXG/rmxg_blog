package com.example.rmxg_blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用户收藏的文章
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@TableName("user_collection_article")
@ApiModel(value = "UserCollectionArticle对象", description = "用户收藏的文章")
public class UserCollectionArticle {

    @ApiModelProperty("用户收藏的文章id")
    private String userCollectionArticleId;

    @ApiModelProperty("用户id")
    private String userId;

    @ApiModelProperty("文章id")
    private String articleId;

    @ApiModelProperty("收藏时间")
    private Timestamp userCollectionArticleTime;
}
