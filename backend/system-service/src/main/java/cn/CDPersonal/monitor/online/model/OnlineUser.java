package cn.CDPersonal.monitor.online.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * 在线用户列表行，对应前端 monitor/online/index.vue 的表格列。
 *
 * 纯展示 VO：由 Redis 会话（TokenSession）与 sys_user 临时拼装，不落库、没有对应表；
 * 字段名需与前端表格列的 prop 保持一致。
 */
public class OnlineUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话标识，强退时用 */
    private String tokenId;
    private Long userId;
    /** 登录名，取 sys_user.user_name；用户已被删除时由控制器填“未知用户” */
    private String userName;
    /** 部门名称（当前留空，不影响列表） */
    private String deptName;
    /** 登录主机地址，会话未记录时为空串 */
    private String ipaddr;
    /** 已格式化为 yyyy-MM-dd HH:mm:ss 的字符串（不是时间戳），前端 parseTime 仅做展示兜底 */
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
