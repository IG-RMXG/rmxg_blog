package cn.CDPersonal.gateway.controller;

import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.gateway.model.params.CaptchaParam;
import cn.CDPersonal.gateway.model.vo.CaptchaVO;
import cn.CDPersonal.gateway.service.CaptchaService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("")
    public ResponseVO<CaptchaVO> get() throws IOException {
        CaptchaVO captcha = captchaService.getCaptcha();
        return ResponseVO.success(captcha);
    }

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
