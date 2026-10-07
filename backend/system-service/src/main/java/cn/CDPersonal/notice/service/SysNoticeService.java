package cn.CDPersonal.notice.service;

import cn.CDPersonal.common.domain.entity.SysNotice;
import cn.CDPersonal.notice.mapper.SysNoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知公告管理。
 */
@Service
public class SysNoticeService {

    private final SysNoticeMapper noticeMapper;

    public SysNoticeService(SysNoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public List<SysNotice> selectNoticeList(SysNotice query) {
        return noticeMapper.selectNoticeList(query);
    }

    public SysNotice selectNoticeById(Long noticeId) {
        return noticeMapper.selectNoticeById(noticeId);
    }

    /** 首页用：最近 5 条已发布公告 */
    public List<SysNotice> selectNoticeTop() {
        return noticeMapper.selectNoticeTop();
    }

    @Transactional
    public int insertNotice(SysNotice notice) {
        return noticeMapper.insertNotice(notice);
    }

    @Transactional
    public int updateNotice(SysNotice notice) {
        return noticeMapper.updateNotice(notice);
    }

    @Transactional
    public int deleteNoticeByIds(Long[] noticeIds) {
        return noticeMapper.deleteNoticeByIds(noticeIds);
    }
}
