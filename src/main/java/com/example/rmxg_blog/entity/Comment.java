package com.example.rmxg_blog.entity;

import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 文章评论
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@ApiModel(value = "Comment对象", description = "文章评论")
public class Comment {

    @ApiModelProperty("文章评论id")
    private String commentId;

    @ApiModelProperty("文章id")
    private String articleId;

    @ApiModelProperty("用户id（评论人）")
    private String userId;

    @ApiModelProperty("文章评论内容")
    private String commentContent;

    @ApiModelProperty("评论时间")
    private Timestamp commentTime;

    @ApiModelProperty("点赞次数")
    private Integer commentGoodNumber;
}
