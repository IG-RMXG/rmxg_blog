package com.example.rmxg_blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author rmxg
 * @since 2022-09-30 02:35:46
 */
@Getter
@Setter
@TableName("upload_file_list")
@ApiModel(value = "UploadFileList对象", description = "")
public class UploadFileList {

    @ApiModelProperty("上传文件的列表")
    private String uploadFileListId;

    @ApiModelProperty("文件大小，作为文件唯一标识")
    private Long fileSize;

    @ApiModelProperty("文件路径url")
    private String fileUrl;

    @ApiModelProperty("文件上传时间")
    private Timestamp uploadFileTime;
}
