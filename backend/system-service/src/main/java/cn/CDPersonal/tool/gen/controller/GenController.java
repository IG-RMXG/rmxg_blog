package cn.CDPersonal.tool.gen.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseStatus;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.core.page.TableDataInfo;
import cn.CDPersonal.common.core.utils.PageUtils;
import cn.CDPersonal.tool.gen.domain.GenTable;
import cn.CDPersonal.tool.gen.service.GenService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 代码生成。
 *
 * 前端契约（web/src/api/tool/gen.js）——注意前缀是 /code 而不是 /system：
 *   GET    /code/gen/list                 已导入表分页
 *   GET    /code/gen/db/list              数据库里可导入的表（分页，前端读 rows/total）
 *   GET    /code/gen/{tableId}            表详情（含 columns）
 *   PUT    /code/gen                      修改生成配置
 *   POST   /code/gen/importTable          导入表（查询参数 tables=a,b,c）
 *   GET    /code/gen/preview/{tableId}    预览代码 -> {code,msg,data:{键:代码文本}}
 *   DELETE /code/gen/{tableIds}           删除
 *   GET    /code/gen/genCode/{tableName}  生成到自定义路径（未实现，见下）
 *   GET    /code/gen/synchDb/{tableName}  同步数据库结构
 *
 * 能力边界：除"生成到自定义路径"外均为真实现（预览是按表结构实时拼代码文本，
 * 不依赖 vm 模板文件）。genCode 需要 Velocity 模板与目标路径，本仓库没有模板目录，
 * 因此明确返回未实现而不是假装成功。
 */
@RestController
@RequestMapping("/code/gen")
public class GenController {

    private final GenService genService;

    public GenController(GenService genService) {
        this.genService = genService;
    }

    @GetMapping("/list")
    public TableDataInfo list(GenTable query) {
        PageUtils.startPage();
        return PageUtils.getDataTable(genService.selectGenTableList(query));
    }

    /** 数据库里还没导入的表 */
    @GetMapping("/db/list")
    public TableDataInfo dbList(@RequestParam(required = false) String tableName,
                                @RequestParam(required = false) String tableComment) {
        PageUtils.startPage();
        return PageUtils.getDataTable(genService.selectDbTableList(tableName, tableComment));
    }

    @GetMapping("/{tableId}")
    public ResponseVO<GenTable> getInfo(@PathVariable Long tableId) {
        GenTable table = genService.selectGenTableById(tableId);
        if (table == null) {
            return ResponseVO.error(ResponseStatus.NOT_FOUND.getCode(), "表信息不存在");
        }
        return ResponseVO.success(table);
    }

    @PutMapping
    public ResponseVO<Void> edit(@RequestBody GenTable genTable) {
        genTable.setUpdateBy(LoginUserHolder.getUserName());
        genService.updateGenTable(genTable);
        return ResponseVO.success();
    }

    /** 导入表：tables 是逗号分隔的表名 */
    @PostMapping("/importTable")
    public ResponseVO<Void> importTable(@RequestParam("tables") String tables) {
        if (tables == null || tables.isBlank()) {
            return ResponseVO.error(ResponseStatus.BAD_REQUEST.getCode(), "请选择要导入的表");
        }
        List<String> tableNames = Arrays.stream(tables.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        int count = genService.importGenTable(tableNames, LoginUserHolder.getUserName());
        if (count == 0) {
            return ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(),
                    "所选表已导入或不存在");
        }
        return ResponseVO.success();
    }

    @GetMapping("/preview/{tableId}")
    public ResponseVO<Map<String, String>> preview(@PathVariable Long tableId) {
        Map<String, String> code = genService.previewCode(tableId);
        if (code.isEmpty()) {
            return ResponseVO.error(ResponseStatus.NOT_FOUND.getCode(), "表信息不存在");
        }
        return ResponseVO.success(code);
    }

    /**
     * 生成到自定义路径：需要 Velocity 模板与目标目录，当前仓库没有模板文件，未实现。
     * 走这个接口前端会弹"系统接口异常"，属于预期行为；需要真正落盘生成时再补。
     */
    @GetMapping("/genCode/{tableName}")
    public ResponseVO<Void> genCode(@PathVariable String tableName) {
        return ResponseVO.error(ResponseStatus.NOT_IMPLEMENTED.getCode(),
                "生成到自定义路径未实现（缺少 vm 模板），请使用「预览」复制代码");
    }

    @GetMapping("/synchDb/{tableName}")
    public ResponseVO<Void> synchDb(@PathVariable String tableName) {
        genService.synchDb(tableName);
        return ResponseVO.success();
    }

    @DeleteMapping("/{tableIds}")
    public ResponseVO<Void> remove(@PathVariable Long[] tableIds) {
        return genService.deleteGenTableByIds(tableIds) > 0
                ? ResponseVO.success()
                : ResponseVO.error(ResponseStatus.INTERNAL_SERVER_ERROR.getCode(), "删除失败");
    }
}
