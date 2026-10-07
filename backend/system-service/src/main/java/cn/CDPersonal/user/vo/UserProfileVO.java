package cn.CDPersonal.user.vo;

import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.domain.entity.SysUser;

import java.io.Serial;
import java.io.Serializable;

/**
 * 个人中心（/system/user/profile）相关接口的响应体。
 *
 * GET  /system/user/profile         前端读 response.data / roleGroup / postGroup
 * POST /system/user/profile/avatar  前端读 response.imgUrl 并直接塞进 store.avatar
 *
 * code / msg 与 ResponseVO 一致，走同一个响应拦截器。
 */
public class UserProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    /** 当前登录用户 */
    private SysUser data;

    /** 角色名，多个用逗号连接（如 "超级管理员"） */
    private String roleGroup;

    /** 岗位名，多个用逗号连接（如 "董事长,项目经理"） */
    private String postGroup;

    /** 头像访问地址，仅上传接口使用 */
    private String imgUrl;

    private Long timestamp;

    public UserProfileVO() {
        this.code = ResponseStatus.SUCCESS.getCode();
        this.msg = ResponseStatus.SUCCESS.getMessage();
        this.timestamp = System.currentTimeMillis();
    }

    public static UserProfileVO of(SysUser user, String roleGroup, String postGroup) {
        UserProfileVO vo = new UserProfileVO();
        vo.setData(user);
        vo.setRoleGroup(roleGroup);
        vo.setPostGroup(postGroup);
        return vo;
    }

    public static UserProfileVO ofAvatar(String imgUrl) {
        UserProfileVO vo = new UserProfileVO();
        vo.setImgUrl(imgUrl);
        return vo;
    }

    public static UserProfileVO error(String message) {
        UserProfileVO vo = new UserProfileVO();
        vo.setCode(ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        vo.setMsg(message);
        return vo;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public SysUser getData() {
        return data;
    }

    public void setData(SysUser data) {
        this.data = data;
    }

    public String getRoleGroup() {
        return roleGroup;
    }

    public void setRoleGroup(String roleGroup) {
        this.roleGroup = roleGroup;
    }

    public String getPostGroup() {
        return postGroup;
    }

    public void setPostGroup(String postGroup) {
        this.postGroup = postGroup;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
