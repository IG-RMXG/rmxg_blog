package cn.CDPersonal.config.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.controller.BaseController;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.common.domain.entity.SysConfig;
import cn.CDPersonal.config.service.SysConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 参数配置管理。
 *
 * 前端契约（web/src/api/system/config.js）：
 *   GET    /system/config/list               分页列表 -> {total, rows}
 *   GET    /system/config/{configId}         详情
 *   GET    /system/config/configKey/{key}    按键名取值（data 与 msg 都放值）
 *   POST   /system/config                    新增
 *   PUT    /system/config                    修改
 *   DELETE /system/config/{configIds}        删除（支持逗号分隔多个）
 *   DELETE /system/config/refreshCache       刷新缓存
 *
 * 注意：server.servlet.context-path 已是 /system，这里只写 /config。
 */
@RestController
@RequestMapping("/config")
public class SysConfigController extends BaseController {

    private final SysConfigService configService;

    public SysConfigController(SysConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/list")
    public TableDataInfo list(SysConfig query) {
        startPage();
        return PageUtils.getDataTable(configService.selectConfigList(query));
    }

    /**
     * 按参数键名取参数值。
     *
     * 约定：data 放字符串值本身；同时把 msg 也设成该值，
     * 因为部分 RuoYi 前端代码直接读 response.msg（如登录页取 initPassword）。
     */
    @GetMapping("/configKey/{configKey}")
    public ResponseVO<String> getConfigKey(@PathVariable String configKey) {
        String value = configService.selectConfigValueByKey(configKey);
        ResponseVO<String> result = ResponseVO.success(value);
        result.setMsg(value == null ? "" : value);
        return result;
    }

    @GetMapping("/{configId}")
    public ResponseVO<SysConfig> getInfo(@PathVariable Long configId) {
        return ResponseVO.success(configService.selectConfigById(configId));
    }

    @PostMapping
    public ResponseVO<Void> add(@Valid @RequestBody SysConfig config) {
        if (!configService.checkConfigKeyUnique(config)) {
            return ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "新增参数'" + config.getConfigName() + "'失败，参数键值已存在");
        }
        if (config.getConfigType() == null) {
            config.setConfigType("N");
        }
        config.setCreateBy(currentUserName());
        return configService.insertConfig(config) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@Valid @RequestBody SysConfig config) {
        if (!configService.checkConfigKeyUnique(config)) {
            return ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "修改参数'" + config.getConfigName() + "'失败，参数键值已存在");
        }
        config.setUpdateBy(currentUserName());
        return configService.updateConfig(config) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "修改失败");
    }

    @DeleteMapping("/refreshCache")
    public ResponseVO<Void> refreshCache() {
        // 当前未引入参数缓存（每次直接查库），无需真正刷新
        return ResponseVO.success();
    }

    @DeleteMapping("/{configIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] configIds) {
        SysConfig builtin = configService.findUndeletableConfig(configIds);
        if (builtin != null) {
            return ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "内置参数【" + builtin.getConfigName() + "】不能删除");
        }
        return configService.deleteConfigByIds(configIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "删除失败");
    }

    /** 当前登录用户名，用于 create_by / update_by 留痕 */
    private String currentUserName() {
        return LoginUserHolder.getUserName();
    }
}
