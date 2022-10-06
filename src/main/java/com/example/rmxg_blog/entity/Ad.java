package com.example.rmxg_blog.entity;

import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 广告
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@ApiModel(value = "Ad对象", description = "广告")
public class Ad {

    @ApiModelProperty("广告id")
    private String adId;

    @ApiModelProperty("广告类型")
    private String adTypeId;

    @ApiModelProperty("广告标题")
    private String adTitle;

    @ApiModelProperty("广告的图片url地址")
    private String adImgUrl;

    @ApiModelProperty("广告跳转连接")
    private String adLinkUrl;

    @ApiModelProperty("广告排序，越小越排在前面")
    private Integer adSort;

    @ApiModelProperty("广告开始时间")
    private Timestamp adBeginTime;

    @ApiModelProperty("广告结束时间")
    private Timestamp adEndTime;

    @ApiModelProperty("添加广告的时间")
    private Timestamp adAddTime;
}
