package cn.CDPersonal.user.service;

import cn.CDPersonal.common.core.utils.TreeSelectUtils;
import cn.CDPersonal.common.domain.entity.SysDept;
import cn.CDPersonal.common.domain.entity.SysPost;
import cn.CDPersonal.common.domain.entity.SysRole;
import cn.CDPersonal.common.domain.entity.SysUser;
import cn.CDPersonal.common.domain.vo.TreeSelect;
import cn.CDPersonal.post.mapper.SysPostMapper;
import cn.CDPersonal.dept.mapper.SysDeptMapper;
import cn.CDPersonal.role.mapper.SysRoleMapper;
import cn.CDPersonal.user.mapper.SysDeptQueryMapper;
import cn.CDPersonal.user.mapper.SysUserManagementMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理业务（/system/user/**）。
 *
 * 与 auth 模块的 SysUserService 分工：
 *   - cn.CDPersonal.auth.service.SysUserService：登录、getInfo、权限，只读；
 *   - 本类：用户 CRUD、关联表、个人中心。
 * 两边都用 sys_user 表，但方法不重叠，不互相调用以免循环依赖。
 */
@Service
public class SysUserManagementService {

    /** 新增用户不填密码时的默认值（与 RuoYi sys.user.initPassword 一致） */
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserManagementMapper userMapper;
    private final SysDeptQueryMapper deptQueryMapper;
    private final SysDeptMapper deptMapper;
    private final SysPostMapper postMapper;
    private final SysRoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SysUserManagementService(SysUserManagementMapper userMapper,
                                    SysDeptQueryMapper deptQueryMapper,
                                    SysDeptMapper deptMapper,
                                    SysPostMapper postMapper,
                                    SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.deptQueryMapper = deptQueryMapper;
        this.deptMapper = deptMapper;
        this.postMapper = postMapper;
        this.roleMapper = roleMapper;
    }

    // ==================== 查询 ====================

    /**
     * 用户分页列表（调用方已 startPage）。
     *
     * deptId 要含子部门：先查 sys_dept 得到"本部门 + 所有子孙"的 id 集合，
     * 再作为 IN 条件交给 SQL，避免在用户表上做 FIND_IN_SET 全表扫描。
     */
    public List<SysUser> selectUserList(SysUser query) {
        applyDeptScope(query);
        return userMapper.selectUserList(query);
    }

    public SysUser selectUserById(Long userId) {
        return userMapper.selectUserById(userId);
    }

    public List<SysRole> selectRolesByUserId(Long userId) {
        return userMapper.selectRolesByUserId(userId);
    }

    public List<Long> selectRoleIdsByUserId(Long userId) {
        return userMapper.selectRoleIdsByUserId(userId);
    }

    public List<Long> selectPostIdsByUserId(Long userId) {
        return userMapper.selectPostIdsByUserId(userId);
    }

    public List<SysPost> selectPostsByUserId(Long userId) {
        return userMapper.selectPostsByUserId(userId);
    }

    /** 全部岗位（新增/修改用户时的岗位下拉） */
    public List<SysPost> selectPostAll() {
        return postMapper.selectPostList(new SysPost());
    }

    /** 全部有效角色（新增/修改用户时的角色下拉） */
    public List<SysRole> selectRoleAll() {
        return roleMapper.selectRoleAll();
    }

    /** 角色授权用户列表：allocated=true 已分配，false 未分配 */
    public List<SysUser> selectUserListByRole(SysUser query, Long roleId, boolean allocated) {
        return userMapper.selectUserListByRole(query, roleId, allocated);
    }

    // ==================== 唯一性校验 ====================

    /** 用户名是否唯一（新增时 userId 为空；修改时命中的必须是自己） */
    public boolean checkUserNameUnique(SysUser user) {
        SysUser exist = userMapper.checkUserNameUnique(user.getUserName());
        return exist == null || exist.getUserId().equals(user.getUserId());
    }

    public boolean checkPhoneUnique(SysUser user) {
        if (user.getPhonenumber() == null || user.getPhonenumber().isBlank()) {
            return true;
        }
        SysUser exist = userMapper.checkPhoneUnique(user.getPhonenumber());
        return exist == null || exist.getUserId().equals(user.getUserId());
    }

    public boolean checkEmailUnique(SysUser user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return true;
        }
        SysUser exist = userMapper.checkEmailUnique(user.getEmail());
        return exist == null || exist.getUserId().equals(user.getUserId());
    }

    // ==================== 写库 ====================

    /**
     * 新增用户。
     * 密码为空时用默认密码 123456，统一 BCrypt 加密后入库；
     * 同时写入 sys_user_role / sys_user_post。
     */
    @Transactional
    public int insertUser(SysUser user) {
        fillDefaults(user);
        user.setPassword(encode(user.getPassword()));
        int rows = userMapper.insertUser(user);
        insertUserRole(user.getUserId(), user.getRoleIds());
        insertUserPost(user.getUserId(), user.getPostIds());
        return rows;
    }

    /**
     * 修改用户基础信息并重写角色 / 岗位关联。
     * 不改密码（密码只走 resetPwd / updatePwd）。
     */
    @Transactional
    public int updateUser(SysUser user) {
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("0");
        }
        int rows = userMapper.updateUser(user);
        // 前端提交时一定带 roleIds / postIds（可能为空数组，表示"清空"），
        // 所以这里是无条件重写而不是"有值才写"。
        if (user.getRoleIds() != null) {
            userMapper.deleteUserRolesByUserId(user.getUserId());
            insertUserRole(user.getUserId(), user.getRoleIds());
        }
        if (user.getPostIds() != null) {
            userMapper.deleteUserPostsByUserId(user.getUserId());
            insertUserPost(user.getUserId(), user.getPostIds());
        }
        return rows;
    }

    /** 软删用户（del_flag='2'），同时清掉角色 / 岗位关联 */
    @Transactional
    public int deleteUserByIds(Long[] userIds) {
        userMapper.deleteUserRolesByUserIds(userIds);
        for (Long userId : userIds) {
            userMapper.deleteUserPostsByUserId(userId);
        }
        return userMapper.softDeleteUserByIds(userIds);
    }

    /** 重置密码，BCrypt 加密 */
    @Transactional
    public int resetPassword(Long userId, String password) {
        String raw = (password == null || password.isBlank()) ? DEFAULT_PASSWORD : password;
        return userMapper.updateUserPassword(userId, encode(raw));
    }

    /** 修改状态 */
    @Transactional
    public int updateUserStatus(SysUser user) {
        return userMapper.updateUserStatus(user);
    }

    /** 修改个人资料（昵称 / 手机 / 邮箱 / 性别 / 头像），不允许改用户名和部门 */
    @Transactional
    public int updateUserProfile(SysUser user) {
        return userMapper.updateUserProfile(user);
    }

    /**
     * 修改自己的密码。
     *
     * 旧密码校验必须兼容明文存储（本仓库早期用明文写过 password），
     * 和 auth 模块登录时的判断保持一致。
     *
     * @return null 表示成功，否则是给前端弹窗用的错误原因
     */
    @Transactional
    public String updateUserPassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            return "登录状态已过期";
        }
        if (newPassword == null || newPassword.isBlank()) {
            return "新密码不能为空";
        }
        if (oldPassword == null || oldPassword.isBlank()) {
            return "旧密码不能为空";
        }
        SysUser current = userMapper.selectUserById(userId);
        if (current == null) {
            return "用户不存在";
        }
        if (!matches(oldPassword, current.getPassword())) {
            return "修改密码失败，旧密码错误";
        }
        if (matches(newPassword, current.getPassword())) {
            return "新密码不能与旧密码相同";
        }
        userMapper.updateUserPassword(userId, encode(newPassword));
        return null;
    }

    /** 更新头像地址 */
    @Transactional
    public int updateUserAvatar(Long userId, String avatar) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setAvatar(avatar);
        return userMapper.updateUserProfile(keepProfileFields(userId, user));
    }

    /**
     * 头像单独更新时会误清其它资料字段，
     * 所以先把库里的昵称/手机/邮箱/性别读出来补齐再 update。
     */
    private SysUser keepProfileFields(Long userId, SysUser patch) {
        SysUser current = userMapper.selectUserById(userId);
        if (current != null) {
            if (patch.getNickName() == null) {
                patch.setNickName(current.getNickName());
            }
            if (patch.getEmail() == null) {
                patch.setEmail(current.getEmail());
            }
            if (patch.getPhonenumber() == null) {
                patch.setPhonenumber(current.getPhonenumber());
            }
            if (patch.getSex() == null) {
                patch.setSex(current.getSex());
            }
        }
        return patch;
    }

    /**
     * 给某个用户重新授权角色。
     * 先删 sys_user_role 里该用户的全部记录，再批量插入（roleIds 为空 = 清空）。
     */
    @Transactional
    public int insertUserAuth(Long userId, Long[] roleIds) {
        userMapper.deleteUserRolesByUserId(userId);
        if (roleIds == null || roleIds.length == 0) {
            return 0;
        }
        return userMapper.batchInsertUserRoles(userId, roleIds);
    }

    /** 给"分配角色"页面用：全部角色 + 标记该用户是否已有 */
    public List<SysRole> buildAuthRoleList(Long userId) {
        List<SysRole> all = selectRoleAll();
        List<Long> owned = userMapper.selectRoleIdsByUserId(userId);
        for (SysRole role : all) {
            role.setFlag(owned != null && owned.contains(role.getRoleId()));
        }
        return all;
    }

    /** 角色名拼接，个人中心 show 用 */
    public String selectRoleGroup(Long userId) {
        List<SysRole> roles = userMapper.selectRolesByUserId(userId);
        return joinNames(roles == null ? null : roles.stream().map(SysRole::getRoleName).toList());
    }

    /** 岗位名拼接，个人中心 show 用 */
    public String selectPostGroup(Long userId) {
        List<SysPost> posts = userMapper.selectPostsByUserId(userId);
        return joinNames(posts == null ? null : posts.stream().map(SysPost::getPostName).toList());
    }

    // ==================== 部门树 ====================

    /**
     * 部门树（GET /system/user/deptTree）。
     *
     * 前端 el-tree 用 props { label: 'label', children: 'children' }，
     * node-key 是 id，但表单要回填 form.deptId，所以节点同时带 deptId / deptName。
     * 组树逻辑与角色的 deptTree 共用 TreeSelectUtils，避免两处实现走偏。
     */
    public List<TreeSelect> selectDeptTree() {
        return TreeSelectUtils.buildDeptTree(deptMapper.selectDeptList(new SysDept()));
    }

    // ==================== 内部工具 ====================

    /** 把 deptId 展开成"本部门 + 子孙部门"集合塞进 params，供 SQL 的 IN 使用 */
    private void applyDeptScope(SysUser query) {
        if (query.getDeptId() == null) {
            return;
        }
        List<Long> scope = deptQueryMapper.selectDeptIdAndChildren(query.getDeptId());
        if (scope == null || scope.isEmpty()) {
            scope = List.of(query.getDeptId());
        }
        Map<String, Object> params = query.getParams();
        if (params == null) {
            params = new HashMap<>();
            query.setParams(params);
        }
        params.put("deptIdScope", scope);
    }

    private void fillDefaults(SysUser user) {
        if (user.getUserType() == null || user.getUserType().isBlank()) {
            user.setUserType("00");
        }
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("0");
        }
        if (user.getSex() == null || user.getSex().isBlank()) {
            user.setSex("2");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(DEFAULT_PASSWORD);
        }
    }

    private void insertUserRole(Long userId, Long[] roleIds) {
        if (roleIds != null && roleIds.length > 0) {
            userMapper.batchInsertUserRoles(userId, roleIds);
        }
    }

    private void insertUserPost(Long userId, Long[] postIds) {
        if (postIds != null && postIds.length > 0) {
            userMapper.batchInsertUserPosts(userId, postIds);
        }
    }

    private String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /** 兼容 BCrypt 哈希与历史明文密码，逻辑与 auth 模块登录校验一致 */
    private boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    private String joinNames(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        return String.join(",", names);
    }
}
