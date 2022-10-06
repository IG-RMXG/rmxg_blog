package com.example.rmxg_blog.entity;

import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 友情连接
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@ApiModel(value = "Link对象", description = "友情连接")
public class Link {

    @ApiModelProperty("友情连接id")
    private String linkId;

    @ApiModelProperty("友情连接标题")
    private String linkTitle;

    @ApiModelProperty("友情连接的地址")
    private String linkUrl;

    @ApiModelProperty("友情连接logo")
    private String linkLogoUrl;

    @ApiModelProperty("友情连接排序，越小越考前")
    private Integer linkSort;

    @ApiModelProperty("添加友情连接的时间")
    private Timestamp linkAddTime;
}
