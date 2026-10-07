package cn.CDPersonal.monitor.online.controller;

import cn.CDPersonal.auth.domain.TokenSession;
import cn.CDPersonal.auth.domain.UserInfo;
import cn.CDPersonal.auth.mapper.SysUserMapper;
import cn.CDPersonal.auth.service.TokenService;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.monitor.online.model.OnlineUser;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 在线用户监控。
 *
 * 前端契约（web/src/api/monitor/online.js）：
 *   GET    /system/online/list        分页，参数 ipaddr / userName
 *   DELETE /system/online/{tokenId}   强退
 *
 * 在线状态以 Redis 里的 login_tokens:* 为准，不落库。
 */
@RestController
@RequestMapping("/online")
public class SysUserOnlineController {

    private final TokenService tokenService;
    private final SysUserMapper sysUserMapper;

    public SysUserOnlineController(TokenService tokenService, SysUserMapper sysUserMapper) {
        this.tokenService = tokenService;
        this.sysUserMapper = sysUserMapper;
    }

    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String ipaddr,
                             @RequestParam(required = false) String userName) {
        List<TokenService.TokenEntry> entries = tokenService.listSessions();

        // 批量取用户信息，避免逐条查库
        Set<Long> userIds = new HashSet<>();
        for (TokenService.TokenEntry e : entries) {
            if (e.session() != null && e.session().getUserId() != null) {
                userIds.add(e.session().getUserId());
            }
        }
        Map<Long, UserInfo> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (UserInfo u : sysUserMapper.selectByIds(userIds)) {
                userMap.put(u.getUserId(), u);
            }
        }

        List<OnlineUser> all = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (TokenService.TokenEntry e : entries) {
            TokenSession s = e.session();
            if (s == null || s.getUserId() == null) {
                continue;
            }
            UserInfo u = userMap.get(s.getUserId());
            OnlineUser ou = new OnlineUser();
            ou.setTokenId(e.tokenId());
            ou.setUserId(s.getUserId());
            ou.setUserName(u == null ? "未知用户" : u.getUserName());
            ou.setIpaddr(s.getIpaddr() == null ? "" : s.getIpaddr());
            ou.setLoginTime(s.getLoginTime() == null ? null : fmt.format(new Date(s.getLoginTime())));
            // 部门名：在线用户走的是 sys_user.dept_id，这里不额外查库，
            // 前端该列留空即可，不影响列表可用性
            all.add(ou);
        }

        // 过滤（在线数据量小，内存过滤足够）
        List<OnlineUser> filtered = all.stream()
                .filter(o -> ipaddr == null || ipaddr.isBlank() || o.getIpaddr().contains(ipaddr))
                .filter(o -> userName == null || userName.isBlank() || o.getUserName().contains(userName))
                .sorted(Comparator.comparing(OnlineUser::getLoginTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // 复用 PageUtils 的分页参数解析：先开启分页再构造 TableDataInfo，
        // 这里数据已在内存，所以直接手工切片，避免对 List 调用 PageHelper
        int pageNum = parseInt(getParam("pageNum"), 1);
        int pageSize = parseInt(getParam("pageSize"), 10);
        int from = Math.min((pageNum - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        TableDataInfo info = new TableDataInfo();
        info.setCode(200);
        info.setMsg("查询成功");
        info.setTotal(filtered.size());
        info.setRows(filtered.subList(from, to));
        return info;
    }

    @DeleteMapping("/{tokenId}")
    public ResponseVO<Void> forceLogout(@PathVariable String tokenId) {
        boolean removed = tokenService.deleteSession(tokenId);
        return removed
                ? ResponseVO.success()
                : ResponseVO.error(500, "该会话已失效");
    }

    private String getParam(String name) {
        var attrs = (org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest().getParameter(name);
    }

    private int parseInt(String v, int def) {
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            int n = Integer.parseInt(v.trim());
            return n <= 0 ? def : n;
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
