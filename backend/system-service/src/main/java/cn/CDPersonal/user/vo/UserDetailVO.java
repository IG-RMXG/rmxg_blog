package cn.CDPersonal.user.vo;

import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.domain.entity.SysPost;
import cn.CDPersonal.common.domain.entity.SysRole;
import cn.CDPersonal.common.domain.entity.SysUser;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /system/user/ 和 GET /system/user/{userId} 的响应体。
 *
 * 这是本组接口里最反直觉的一个：前端读的是**同层的** data / posts / roles /
 * postIds / roleIds，而不是把岗位角色塞进 data 里面：
 *
 *   getUser().then(response => {
 *     form.value        = response.data      // 用户字段
 *     postOptions.value = response.posts     // 全部岗位（下拉项）
 *     roleOptions.value = response.roles     // 全部角色（下拉项）
 *     form.value.postIds = response.postIds  // 该用户已选的岗位
 *     form.value.roleIds = response.roleIds  // 该用户已选的角色
 *   })
 *
 * 所以不能直接用 ResponseVO<SysUser>，必须自定义一个把这几项平铺的 VO。
 * code / msg 字段保持和 ResponseVO 一致，前端响应拦截器按 code 判断成败。
 */
public class UserDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    /** 用户对象；新增用户时为空对象（前端只读 posts / roles） */
    private SysUser data;

    /** 全部可选岗位 */
    private List<SysPost> posts = new ArrayList<>();

    /** 全部可选角色 */
    private List<SysRole> roles = new ArrayList<>();

    /** 该用户已分配的岗位 id */
    private List<Long> postIds = new ArrayList<>();

    /** 该用户已分配的角色 id */
    private List<Long> roleIds = new ArrayList<>();

    private Long timestamp;

    public UserDetailVO() {
        this.code = ResponseStatus.SUCCESS.getCode();
        this.msg = ResponseStatus.SUCCESS.getMessage();
        this.timestamp = System.currentTimeMillis();
    }

    /** 新增用户：只回岗位和角色下拉项 */
    public static UserDetailVO forCreate(List<SysPost> posts, List<SysRole> roles) {
        UserDetailVO vo = new UserDetailVO();
        vo.setData(new SysUser());
        vo.setPosts(posts == null ? new ArrayList<>() : posts);
        vo.setRoles(roles == null ? new ArrayList<>() : roles);
        return vo;
    }

    /** 修改用户：回用户字段 + 已选岗位角色 + 全部下拉项 */
    public static UserDetailVO forEdit(SysUser user, List<SysPost> posts, List<SysRole> roles,
                                       List<Long> postIds, List<Long> roleIds) {
        UserDetailVO vo = new UserDetailVO();
        vo.setData(user == null ? new SysUser() : user);
        vo.setPosts(posts == null ? new ArrayList<>() : posts);
        vo.setRoles(roles == null ? new ArrayList<>() : roles);
        vo.setPostIds(postIds == null ? new ArrayList<>() : postIds);
        vo.setRoleIds(roleIds == null ? new ArrayList<>() : roleIds);
        return vo;
    }

    public static UserDetailVO error(String message) {
        UserDetailVO vo = new UserDetailVO();
        vo.setCode(ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        vo.setMsg(message);
        vo.setData(new SysUser());
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

    public List<SysPost> getPosts() {
        return posts;
    }

    public void setPosts(List<SysPost> posts) {
        this.posts = posts;
    }

    public List<SysRole> getRoles() {
        return roles;
    }

    public void setRoles(List<SysRole> roles) {
        this.roles = roles;
    }

    public List<Long> getPostIds() {
        return postIds;
    }

    public void setPostIds(List<Long> postIds) {
        this.postIds = postIds;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
