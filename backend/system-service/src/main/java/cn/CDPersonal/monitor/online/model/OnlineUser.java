package cn.CDPersonal.monitor.online.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * 在线用户列表行，对应前端 monitor/online/index.vue 的表格列。
 */
public class OnlineUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话标识，强退时用 */
    private String tokenId;
    private Long userId;
    private String userName;
    /** 部门名称（当前留空，不影响列表） */
    private String deptName;
    private String ipaddr;
    private String loginTime;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public String getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }
}
