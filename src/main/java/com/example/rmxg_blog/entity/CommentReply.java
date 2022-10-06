package com.example.rmxg_blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 评论回复
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@TableName("comment_reply")
@ApiModel(value = "CommentReply对象", description = "评论回复")
public class CommentReply {

    @ApiModelProperty("评论回复id")
    private String commentReplyId;

    @ApiModelProperty("评论id")
    private String commentId;

    @ApiModelProperty("回复评论的人id")
    private String replyUserId;

    @ApiModelProperty("继续回复的人id")
    private String secondlyUserId;

    @ApiModelProperty("评论回复内容")
    private String replyContent;

    @ApiModelProperty("评论回复的时间")
    private Timestamp commentReplyTime;
}
