package com.example.rmxg_blog.entity;

import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@ApiModel(value = "User对象", description = "用户")
public class User {

    @ApiModelProperty("用户id")
    private String userId;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("用户密码")
    private String userPassword;

    @ApiModelProperty("是否可以发布文章 0不能，1可以发布")
    private Integer userPublishArticle;

    @ApiModelProperty("是否冻结，0正常，1冻结（冻结后无法登陆）")
    private Integer userFrozen;

    @ApiModelProperty("注册时间")
    private Timestamp userRegisterTime;
}
