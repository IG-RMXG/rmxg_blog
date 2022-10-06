package com.example.rmxg_blog.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 管理员
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@ApiModel(value = "Admin对象", description = "管理员")
public class Admin {

    @ApiModelProperty("管理员id")
    private String adminId;

    @ApiModelProperty("管理员名称")
    private String adminName;

    @ApiModelProperty("管理员密码")
    private String adminPassword;
}
