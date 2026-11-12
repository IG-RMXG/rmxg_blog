package cn.CDPersonal.monitor.log.service;

import cn.CDPersonal.common.domain.entity.SysLogininfor;
import cn.CDPersonal.common.domain.entity.SysOperLog;
import cn.CDPersonal.monitor.log.mapper.SysLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 日志管理：操作日志 + 登录日志。
 *
 * 本类只做 Mapper 透传，不组装条件、不做权限校验：分页由 Controller 先调 PageUtils.startPage()，
 * 过滤条件随查询对象传入；日志写入需要业务显式调用 insert*，本服务没有内置 AOP 自动记录。
 * 除查询外都标注 @Transactional（单表操作；被上层事务调用时加入同一事务）。
 * 两张表都没有逻辑删除，delete / clean 为物理删除，清空不可恢复。
 */
@Service
public class SysLogService {

    private final SysLogMapper logMapper;

    public SysLogService(SysLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    // ==================== 操作日志 ====================

    public List<SysOperLog> selectOperLogList(SysOperLog query) {
        return logMapper.selectOperLogList(query);
    }

    @Transactional
    public int deleteOperLogByIds(Long[] operIds) {
        return logMapper.deleteOperLogByIds(operIds);
    }

    // 物理清空整表，不可恢复
    @Transactional
    public int cleanOperLog() {
        return logMapper.cleanOperLog();
    }

    @Transactional
    public int insertOperLog(SysOperLog operLog) {
        return logMapper.insertOperLog(operLog);
    }

    // ==================== 登录日志 ====================

    public List<SysLogininfor> selectLogininforList(SysLogininfor query) {
        return logMapper.selectLogininforList(query);
    }

    @Transactional
    public int deleteLogininforByIds(Long[] infoIds) {
        return logMapper.deleteLogininforByIds(infoIds);
    }

    @Transactional
    public int cleanLogininfor() {
        return logMapper.cleanLogininfor();
    }

    @Transactional
    public int insertLogininfor(SysLogininfor logininfor) {
        return logMapper.insertLogininfor(logininfor);
    }
}
