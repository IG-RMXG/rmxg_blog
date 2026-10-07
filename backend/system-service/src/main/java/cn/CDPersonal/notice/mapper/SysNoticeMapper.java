package cn.CDPersonal.notice.mapper;

import cn.CDPersonal.common.domain.entity.SysNotice;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通知公告 Mapper（sys_notice）。
 *
 * 注意：notice_content 在库里是 longblob，Java 侧仍按 String 读写，
 * MyBatis 会走 setString/getString，与 RuoYi 一致。
 */
@Mapper
public interface SysNoticeMapper {

    @Select("""
            <script>
            SELECT notice_id, notice_title, notice_type, notice_content, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_notice
            <where>
              <if test="noticeTitle != null and noticeTitle != ''">
                AND notice_title LIKE CONCAT('%', #{noticeTitle}, '%')
              </if>
              <if test="createBy != null and createBy != ''">
                AND create_by LIKE CONCAT('%', #{createBy}, '%')
              </if>
              <if test="noticeType != null and noticeType != ''">
                AND notice_type = #{noticeType}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="params.beginTime != null and params.beginTime != ''">
                AND DATE(create_time) &gt;= DATE(#{params.beginTime})
              </if>
              <if test="params.endTime != null and params.endTime != ''">
                AND DATE(create_time) &lt;= DATE(#{params.endTime})
              </if>
            </where>
            ORDER BY notice_id DESC
            </script>
            """)
    List<SysNotice> selectNoticeList(SysNotice query);

    @Select("""
            SELECT notice_id, notice_title, notice_type, notice_content, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_notice
            WHERE notice_id = #{noticeId}
            """)
    SysNotice selectNoticeById(@Param("noticeId") Long noticeId);

    /** 首页用：最近 5 条已发布（status='0'）的公告 */
    @Select("""
            SELECT notice_id, notice_title, notice_type, notice_content, status,
                   create_by, create_time, update_by, update_time, remark
            FROM sys_notice
            WHERE status = '0'
            ORDER BY notice_id DESC
            LIMIT 5
            """)
    List<SysNotice> selectNoticeTop();

    @Insert("""
            INSERT INTO sys_notice
              (notice_title, notice_type, notice_content, status, create_by, create_time, remark)
            VALUES
              (#{noticeTitle}, #{noticeType}, #{noticeContent}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "noticeId")
    int insertNotice(SysNotice notice);

    @Update("""
            UPDATE sys_notice
            SET notice_title = #{noticeTitle},
                notice_type = #{noticeType},
                notice_content = #{noticeContent},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE notice_id = #{noticeId}
            """)
    int updateNotice(SysNotice notice);

    @Delete("""
            <script>
            DELETE FROM sys_notice WHERE notice_id IN
            <foreach collection="array" item="noticeId" open="(" separator="," close=")">
              #{noticeId}
            </foreach>
            </script>
            """)
    int deleteNoticeByIds(@Param("array") Long[] noticeIds);
}
