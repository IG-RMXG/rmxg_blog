package cn.CDPersonal.uat.controller;

import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.uat.service.UatFakeLoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * UAT 验收接口（对外前缀 /uat/**，经 UAT 网关 8890 转发到本服务 8020）。
 *
 * 前端调用示例：POST /uat-api/uat/fake-login  body {"role":"admin"}
 *
 * 这些接口刻意不做登录校验——假登录本来就是给未登录的验收人员用的。
 * 安全边界：本服务只在 UAT 环境部署，且 DEV 网关不路由 /uat/**（默认 404）。
 */
@RestController
@RequestMapping("/uat")
public class UatController {

    private final UatFakeLoginService fakeLoginService;

    public UatController(UatFakeLoginService fakeLoginService) {
        this.fakeLoginService = fakeLoginService;
    }

    /**
     * 假登录：按角色签发真实可用的 token。
     * body：{"role":"admin"|"user"}
     */
    @PostMapping("/fake-login")
    public ResponseVO<Map<String, Object>> fakeLogin(@RequestBody Map<String, String> body) {
        try {
            return ResponseVO.success(fakeLoginService.fakeLogin(body == null ? null : body.get("role")));
        } catch (IllegalArgumentException e) {
            return ResponseVO.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseVO.error(500, e.getMessage());
        }
    }

    /** 环境自检：库 / Redis / 测试账号就绪情况，排查 UAT 起不来时先看这里 */
    @GetMapping("/status")
    public ResponseVO<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "uat-service");
        status.put("db", fakeLoginService.dbOk());
        status.put("redis", fakeLoginService.redisOk());
        status.put("accounts", fakeLoginService.accountStatus());
        return ResponseVO.success(status);
    }
}
