package cn.CDPersonal.common.domain.entity;

import cn.CDPersonal.common.domain.model.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 登录日志 sys_logininfor。
 * 该表时间是 access_time，不是 create_time。
 */
public class SysLogininfor extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 访问ID */
    private Long infoId;
    /** 用户账号 */
    private String userName;
    /** 登录IP地址 */
    private String ipaddr;
    /** 登录状态 0成功 1失败 */
    private String status;
    /** 提示信息 */
    private String msg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date accessTime;

    public Long getInfoId() {
        return infoId;
    }

    public void setInfoId(Long infoId) {
        this.infoId = infoId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Date getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(Date accessTime) {
        this.accessTime = accessTime;
    }
}
