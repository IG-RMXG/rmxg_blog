package cn.CDPersonal.auth.controller;

import cn.CDPersonal.auth.domain.UserInfo;
import cn.CDPersonal.auth.model.UserInfoVO;
import cn.CDPersonal.auth.service.SysUserService;
import cn.CDPersonal.auth.service.TokenService;
import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.controller.BaseController;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.core.utils.UploadPathUtils;
import cn.CDPersonal.common.domain.entity.SysPost;
import cn.CDPersonal.common.domain.entity.SysRole;
import cn.CDPersonal.common.domain.entity.SysUser;
import cn.CDPersonal.common.domain.vo.TreeSelect;
import cn.CDPersonal.user.service.SysUserManagementService;
import cn.CDPersonal.user.vo.UserAuthRoleVO;
import cn.CDPersonal.user.vo.UserDetailVO;
import cn.CDPersonal.user.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 用户接口（对外前缀 /system/user）。
 *
 * server.servlet.context-path 已经是 /system，所以这里只写 /user。
 *
 * 这里同时装着两类接口：
 *   1) 登录链路：GET /user/getInfo（前端 store/modules/user.js 读顶层字段，不能包 ResponseVO）
 *   2) 用户管理：/user/list、/user/{userId}、CRUD、authRole、deptTree、个人中心
 *
 * 响应结构的三处"反直觉"契约（写错页面直接空白）：
 *   - GET /user/ 与 GET /user/{userId}：data / posts / roles / postIds / roleIds 全在**顶层**；
 *   - GET /user/authRole/{userId}：user / roles 在**顶层**；
 *   - PUT /user/authRole：参数在 **query string** 里（@RequestParam，不是 @RequestBody）。
 */
@RestController
@RequestMapping("/user")
public class SysUserController extends BaseController {

    /** 头像允许的扩展名（按真实文件名取扩展名，取不到时统一存 .png） */
    private static final Set<String> AVATAR_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /** 头像落盘的物理目录（与 WebMvcConfig 的静态映射共用同一份解析） */
    private final Path uploadDir = UploadPathUtils.avatarDir();

    private final SysUserService sysUserService;
    private final TokenService tokenService;
    private final SysUserManagementService userManagementService;

    public SysUserController(SysUserService sysUserService,
                             TokenService tokenService,
                             SysUserManagementService userManagementService) {
        this.sysUserService = sysUserService;
        this.tokenService = tokenService;
        this.userManagementService = userManagementService;
        UploadPathUtils.ensureAvatarDir();
    }

    /**
     * 注意：这里刻意不返回 ResponseVO。
     *
     * 前端 store/modules/user.js 的 getInfo() 直接读顶层字段：
     *     const user = res.user
     *     this.roles = res.roles
     *     this.permissions = res.permissions
     * 如果把 user 裹进 ResponseVO.data 里，前端会拿到 undefined，
     * 进而把 roles 置成 ['ROLE_DEFAULT']、菜单权限全空。
     * 响应拦截器对没有 code 字段的响应按成功放行，所以直接返回 VO 即可。
     */
    @RequestMapping(value = "/getInfo", method = RequestMethod.GET)
    public UserInfoVO getInfo(HttpServletRequest request) {
        UserInfo user = tokenService.getLoginUser(request);
        if (user == null) {
            throw new IllegalStateException("登录状态已过期");
        }

        UserInfoVO vo = new UserInfoVO();
        // 不要把密码哈希回给前端
        user.setPassword(null);
        vo.setUser(user);
        vo.setRoles(sysUserService.selectRoleKeys(user.getUserId()));
        // admin 在 sys_role_menu 里没有记录，按角色查权限会得到空数组，必须走全量
        vo.setPermissions(sysUserService.isAdmin(user.getUserId())
                ? sysUserService.selectAllPerms()
                : sysUserService.selectPerms(user.getUserId()));
        vo.setIsDefaultModifyPwd(Boolean.FALSE);
        vo.setIsPasswordExpired(Boolean.FALSE);
        return vo;
    }

    // ==================== 个人中心 ====================

    /**
     * GET /system/user/profile
     * 前端 profile/index.vue 读 response.data / response.roleGroup / response.postGroup。
     */
    @GetMapping("/profile")
    public UserProfileVO getProfile() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            return UserProfileVO.error("登录状态已过期");
        }
        SysUser user = userManagementService.selectUserById(userId);
        if (user == null) {
            return UserProfileVO.error("用户不存在");
        }
        return UserProfileVO.of(user,
                userManagementService.selectRoleGroup(userId),
                userManagementService.selectPostGroup(userId));
    }

    /**
     * PUT /system/user/profile
     * 只允许改昵称 / 手机 / 邮箱 / 性别，userId 一律取当前登录人（防止越权改别人）。
     */
    @RequestMapping(value = "/profile", method = RequestMethod.PUT)
    public ResponseVO<Void> updateProfile(@RequestBody SysUser body) {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            return ResponseVO.error(500, "登录状态已过期");
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setNickName(body.getNickName());
        user.setPhonenumber(body.getPhonenumber());
        user.setEmail(body.getEmail());
        user.setSex(body.getSex());
        return userManagementService.updateUserProfile(user) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    /**
     * PUT /system/user/profile/updatePwd
     * 前端 api/system/user.js 的 updateUserPwd() 用 data: {oldPassword, newPassword}
     * 且拦截器默认 Content-Type=application/json，所以这里收 JSON body。
     * 同时保留 @RequestParam 形式的兼容分支：表单/query 提交也能用。
     */
    @RequestMapping(value = "/profile/updatePwd", method = RequestMethod.PUT)
    public ResponseVO<Void> updatePwd(@RequestBody(required = false) Map<String, Object> body,
                                      @RequestParam(value = "oldPassword", required = false) String oldPasswordParam,
                                      @RequestParam(value = "newPassword", required = false) String newPasswordParam) {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            return ResponseVO.error(500, "登录状态已过期");
        }
        String oldPassword = oldPasswordParam;
        String newPassword = newPasswordParam;
        if (body != null) {
            if (oldPassword == null && body.get("oldPassword") != null) {
                oldPassword = String.valueOf(body.get("oldPassword"));
            }
            if (newPassword == null && body.get("newPassword") != null) {
                newPassword = String.valueOf(body.get("newPassword"));
            }
        }
        String error = userManagementService.updateUserPassword(userId, oldPassword, newPassword);
        return error == null ? ResponseVO.success() : ResponseVO.error(500, error);
    }

    /**
     * POST /system/user/profile/avatar
     * 前端用 FormData（字段名 avatarfile）上传裁剪后的头像，读 response.imgUrl。
     * 这里简化实现：落盘到本地 upload 目录，直接把可访问路径回给前端。
     * 落盘失败也只返回业务错误，绝不抛 500 让页面崩。
     */
    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileVO uploadAvatar(@RequestParam(value = "avatarfile", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return UserProfileVO.error("上传头像图片异常，请重新上传");
        }
        try {
            Files.createDirectories(uploadDir);
            String ext = resolveExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = uploadDir.resolve(fileName).normalize();
            if (!target.startsWith(uploadDir)) {
                return UserProfileVO.error("文件名非法");
            }
            file.transferTo(target);

            String url = UploadPathUtils.AVATAR_URL_PREFIX + fileName;
            Long userId = LoginUserHolder.getUserId();
            if (userId != null) {
                userManagementService.updateUserAvatar(userId, url);
            }
            return UserProfileVO.ofAvatar(url);
        } catch (IOException | IllegalStateException e) {
            return UserProfileVO.error("上传头像失败：" + e.getMessage());
        }
    }

    /** 从原始文件名里取安全的小写扩展名，不合法则退回 png */
    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return "png";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "png";
        }
        String ext = originalFilename.substring(dot + 1).toLowerCase();
        return AVATAR_EXTENSIONS.contains(ext) ? ext : "png";
    }

    // ==================== 用户管理 ====================

    /**
     * GET /system/user/list —— 分页，前端读 response.rows / response.total。
     * 支持 userName（模糊）、phonenumber（模糊）、status、deptId（含子部门）。
     */
    @GetMapping("/list")
    public TableDataInfo list(SysUser query) {
        startPage();
        return PageUtils.getDataTable(userManagementService.selectUserList(query));
    }

    /**
     * GET /system/user/ 与 GET /system/user/{userId}
     *
     * ⚠️ 新增用户时前端 getUser() 不传 id，请求的是 "/system/user/"（带尾斜杠），
     * 所以两个路径必须映射到同一个方法，且响应体是"平铺"的 UserDetailVO。
     */
    @GetMapping({"", "/", "/{userId}"})
    public UserDetailVO getUser(@PathVariable(value = "userId", required = false) Long userId) {
        List<SysPost> posts = userManagementService.selectPostAll();
        List<SysRole> roles = userManagementService.selectRoleAll();
        if (userId == null) {
            return UserDetailVO.forCreate(posts, roles);
        }
        SysUser user = userManagementService.selectUserById(userId);
        if (user == null) {
            return UserDetailVO.error("用户不存在");
        }
        return UserDetailVO.forEdit(user, posts, roles,
                userManagementService.selectPostIdsByUserId(userId),
                userManagementService.selectRoleIdsByUserId(userId));
    }

    @GetMapping("/deptTree")
    public ResponseVO<List<TreeSelect>> deptTree() {
        return ResponseVO.success(userManagementService.selectDeptTree());
    }

    /** POST /system/user —— 新增，密码空则用默认 123456（BCrypt 存储） */
    @PostMapping
    public ResponseVO<Void> add(@RequestBody SysUser user) {
        if (!userManagementService.checkUserNameUnique(user)) {
            return ResponseVO.error(500, "新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        if (!userManagementService.checkPhoneUnique(user)) {
            return ResponseVO.error(500, "新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        if (!userManagementService.checkEmailUnique(user)) {
            return ResponseVO.error(500, "新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(LoginUserHolder.getUserName());
        return userManagementService.insertUser(user) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    /** PUT /system/user —— 修改，同时重写 sys_user_role / sys_user_post */
    @PutMapping
    public ResponseVO<Void> edit(@RequestBody SysUser user) {
        if (user.getUserId() == null) {
            return ResponseVO.error(500, "用户ID不能为空");
        }
        if (!userManagementService.checkPhoneUnique(user)) {
            return ResponseVO.error(500, "修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        if (!userManagementService.checkEmailUnique(user)) {
            return ResponseVO.error(500, "修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(LoginUserHolder.getUserName());
        return userManagementService.updateUser(user) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    /** DELETE /system/user/{userIds} —— 批量软删，admin(userId=1) 不允许删 */
    @DeleteMapping("/{userIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] userIds) {
        for (Long userId : userIds) {
            if (userId != null && userId == 1L) {
                return ResponseVO.error(500, "不允许删除超级管理员用户");
            }
        }
        return userManagementService.deleteUserByIds(userIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }

    /** PUT /system/user/resetPwd —— JSON body {userId, password} */
    @PutMapping("/resetPwd")
    public ResponseVO<Void> resetPwd(@RequestBody SysUser user) {
        if (user.getUserId() == null) {
            return ResponseVO.error(500, "用户ID不能为空");
        }
        return userManagementService.resetPassword(user.getUserId(), user.getPassword()) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "重置密码失败");
    }

    /** PUT /system/user/changeStatus —— JSON body {userId, status} */
    @PutMapping("/changeStatus")
    public ResponseVO<Void> changeStatus(@RequestBody SysUser user) {
        if (user.getUserId() == null) {
            return ResponseVO.error(500, "用户ID不能为空");
        }
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            return ResponseVO.error(500, "状态不能为空");
        }
        user.setUpdateBy(LoginUserHolder.getUserName());
        return userManagementService.updateUserStatus(user) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改状态失败");
    }

    /**
     * GET /system/user/authRole/{userId}
     * 前端读顶层的 response.user / response.roles（roles 每项带 flag）。
     */
    @GetMapping("/authRole/{userId}")
    public UserAuthRoleVO authRole(@PathVariable Long userId) {
        SysUser user = userManagementService.selectUserById(userId);
        if (user == null) {
            return UserAuthRoleVO.success(new SysUser(), new ArrayList<>());
        }
        return UserAuthRoleVO.success(user, userManagementService.buildAuthRoleList(userId));
    }

    /**
     * PUT /system/user/authRole
     *
     * ⚠️ 前端 updateAuthRole() 用 params 传参（query string），
     * 不是 JSON body，所以这里必须用 @RequestParam：
     *   userId=2&roleIds=2,3
     * roleIds 是逗号分隔字符串，空串表示清空全部角色。
     */
    @PutMapping("/authRole")
    public ResponseVO<Void> updateAuthRole(@RequestParam("userId") Long userId,
                                           @RequestParam(value = "roleIds", required = false) String roleIds) {
        if (userId == null) {
            return ResponseVO.error(500, "用户ID不能为空");
        }
        userManagementService.insertUserAuth(userId, parseLongArray(roleIds));
        return ResponseVO.success();
    }

    /** 逗号分隔 id 串 → Long[]；非法片段直接忽略，不抛异常 */
    private Long[] parseLongArray(String csv) {
        if (csv == null || csv.isBlank()) {
            return new Long[0];
        }
        List<Long> ids = new ArrayList<>();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(trimmed));
            } catch (NumberFormatException ignored) {
                // 忽略脏数据
            }
        }
        return ids.toArray(new Long[0]);
    }

    /**
     * POST /system/user/importData
     * 页面上的"导入"按钮会往这里传 xlsx。Excel 导入未实现，
     * 这里返回明确的业务错误（前端 handleFileSuccess 会弹 response.msg），
     * 而不是让请求 404/500。
     */
    @PostMapping("/importData")
    public ResponseVO<Void> importData() {
        return ResponseVO.error(501, "用户导入功能尚未实现");
    }
}
