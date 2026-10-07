package cn.CDPersonal.dept.controller;

import cn.CDPersonal.auth.util.LoginUserHolder;
import cn.CDPersonal.common.core.ResponseVO;
import cn.CDPersonal.common.domain.entity.SysDept;
import cn.CDPersonal.dept.service.SysDeptService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理。
 *
 * 前端契约（web/src/api/system/dept.js）：
 *   GET    /system/dept/list                扁平数组 -> {code,msg,data:[...]}，前端自己 handleTree
 *   GET    /system/dept/list/exclude/{id}   同上，但排除该部门及其子孙（"上级部门"下拉）
 *   GET    /system/dept/{deptId}            详情
 *   POST   /system/dept                     新增
 *   PUT    /system/dept                     修改
 *   DELETE /system/dept/{deptId}            删除（软删）
 *
 * 注意：server.servlet.context-path 已是 /system，这里只写 /dept。
 * 列表**不能**用 TableDataInfo：前端读的是 response.data，包成 {total,rows} 页面会空白。
 */
@RestController
@RequestMapping("/dept")
public class SysDeptController {

    private final SysDeptService deptService;

    public SysDeptController(SysDeptService deptService) {
        this.deptService = deptService;
    }

    /** 部门列表：非分页，扁平数组，前端建树 */
    @GetMapping("/list")
    public ResponseVO<List<SysDept>> list(SysDept query) {
        return ResponseVO.success(deptService.selectDeptList(query));
    }

    /** 排除某部门及其所有子孙，用于"上级部门"下拉，避免把自己选成自己的上级 */
    @GetMapping("/list/exclude/{deptId}")
    public ResponseVO<List<SysDept>> excludeChild(@PathVariable Long deptId) {
        return ResponseVO.success(deptService.selectDeptListExcludeChildren(deptId));
    }

    @GetMapping("/{deptId}")
    public ResponseVO<SysDept> getInfo(@PathVariable Long deptId) {
        return ResponseVO.success(deptService.selectDeptById(deptId));
    }

    @PostMapping
    public ResponseVO<Void> add(@Validated @RequestBody SysDept dept) {
        if (dept.getParentId() != null && dept.getParentId() != 0L
                && deptService.selectDeptById(dept.getParentId()) == null) {
            return ResponseVO.error(500, "新增部门'" + dept.getDeptName() + "'失败，上级部门不存在");
        }
        dept.setCreateBy(LoginUserHolder.getUserName());
        return deptService.insertDept(dept) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "新增失败");
    }

    @PutMapping
    public ResponseVO<Void> edit(@Validated @RequestBody SysDept dept) {
        if (dept.getDeptId() == null) {
            return ResponseVO.error(500, "部门ID不能为空");
        }
        if (deptService.isParentIllegal(dept.getDeptId(), dept.getParentId())) {
            return ResponseVO.error(500,
                    "修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己或自己的下级部门");
        }
        dept.setUpdateBy(LoginUserHolder.getUserName());
        return deptService.updateDept(dept) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "修改失败");
    }

    /** 删除部门：有下级部门 / 部门下有用户都不允许删 */
    @DeleteMapping("/{deptId}")
    public ResponseVO<Void> remove(@PathVariable Long deptId) {
        if (deptService.hasChildByDeptId(deptId)) {
            return ResponseVO.error(500, "存在下级部门,不允许删除");
        }
        if (deptService.hasUserByDeptId(deptId)) {
            return ResponseVO.error(500, "部门存在用户,不允许删除");
        }
        return deptService.deleteDeptById(deptId) > 0
                ? ResponseVO.success()
                : ResponseVO.error(500, "删除失败");
    }
}
