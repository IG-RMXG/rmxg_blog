package cn.CDPersonal.monitor.log.service;

import cn.CDPersonal.common.domain.entity.SysLogininfor;
import cn.CDPersonal.common.domain.entity.SysOperLog;
import cn.CDPersonal.monitor.log.mapper.SysLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 日志管理：操作日志 + 登录日志。
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
