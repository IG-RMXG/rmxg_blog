package cn.CDPersonal.gateway.controller;

import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.gateway.model.params.CaptchaParam;
import cn.CDPersonal.gateway.model.vo.CaptchaVO;
import cn.CDPersonal.gateway.service.CaptchaService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 拼图验证码接口，路径 /captcha。
 *
 * <p>这两个接口必须能在登录前访问（网关的 ServiceForwardController 也未包含 /captcha 前缀），
 * 是登录页获取验证码的唯一入口：
 * <ul>
 *   <li>GET  /captcha：生成底图与拼图块（Base64），并返回一个 5 分钟内有效的 token；</li>
 *   <li>POST /captcha：回传 token 与拖动后的 x 坐标，校验成功返回 code=200，失败返回 code=400。</li>
 * </ul>
 *
 * <p>失败不区分「token 过期」与「位置不对」，统一下发「验证失败」，避免给撞库方额外信息。
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    // 每次调用都会重新抠图并写一条 5 分钟 TTL 的 Redis 记录，前端不要在页面上反复轮询
    @GetMapping("")
    public ResponseVO<CaptchaVO> get() throws IOException {
        CaptchaVO captcha = captchaService.getCaptcha();
        return ResponseVO.success(captcha);
    }

    /**
     * 校验滑动位置。
     *
     * <p>HTTP 状态恒为 200，成败只看响应体 code（200 通过 / 400 失败），
     * 前端据此决定是否继续提交登录请求。
     */
    @PostMapping("")
    public ResponseVO<String> check(@RequestBody CaptchaParam captchaParam) {
        boolean checked = captchaService.checkCaptcha(captchaParam);
        if( checked) {
            return ResponseVO.success("验证成功", null);
        }else{
            return ResponseVO.error(ResponseStatus.BAD_REQUEST.getCode(), "验证失败");
        }
    }


}
