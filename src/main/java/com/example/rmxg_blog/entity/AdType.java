package com.example.rmxg_blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 广告类型
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@TableName("ad_type")
@ApiModel(value = "AdType对象", description = "广告类型")
public class AdType {

    @ApiModelProperty("广告类型id")
    private String adTypeId;

    @ApiModelProperty("广告类型名称")
    private String adTypeTitle;

    @ApiModelProperty("广告类型标识（首页顶部广告，轮播图广告，文章详情广告）")
    private String adTypeTag;

    @ApiModelProperty("广告类型排序，越小越靠前")
    private Integer adTypeSort;

    @ApiModelProperty("广告类型添加时间")
    private Timestamp adTypeAddTime;
}
