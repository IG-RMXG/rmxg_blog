package cn.CDPersonal.blog.mapper;

import cn.CDPersonal.blog.domain.BlogComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 博客评论 Mapper（blog_comment）
 *
 * 状态只有两种：0=正常（前台展示）、1=隐藏（审核后置为该值，门户与详情页都不再返回）。
 * 删除为物理删除；文章表上的 comment_count 是冗余计数，由 Service 在评论增删的同一事务里维护。
 * parent_id 为 0 表示顶级评论（表结构约定，无外键约束），回复时指向被回复的评论 id。
 */
@Mapper
public interface BlogCommentMapper {

    /**
     * 评论查询列表（管理端与文章详情共用）。
     *
     * 同一方法服务两个入口：管理端不传 status 看全部；登录态的文章详情接口固定传 status="0" 只看正常评论
     * （门户侧走 BlogPortalMapper 的独立查询，不经过这里）。
     * 关联文章与用户都用 LEFT JOIN，只为回显标题和昵称，被删用户不会导致评论行消失。
     * 排序 comment_id DESC 即最新在前，不用 create_time 以避免同秒评论顺序不稳定。
     */
    @Select("""
            <script>
            SELECT c.comment_id, c.article_id, c.parent_id, c.content, c.user_id, c.status,
                   c.create_by, c.create_time, c.remark,
                   a.title AS article_title,
                   u.nick_name AS nick_name,
                   u.user_name AS user_name
            FROM blog_comment c
            LEFT JOIN blog_article a ON a.article_id = c.article_id
            LEFT JOIN sys_user u ON u.user_id = c.user_id
            <where>
              <if test="articleId != null">
                AND c.article_id = #{articleId}
              </if>
              <if test="status != null and status != ''">
                AND c.status = #{status}
              </if>
              <if test="content != null and content != ''">
                AND c.content LIKE CONCAT('%', #{content}, '%')
              </if>
            </where>
            ORDER BY c.comment_id DESC
            </script>
            """)
    List<BlogComment> selectCommentList(BlogComment query);

    @Select("""
            SELECT c.comment_id, c.article_id, c.parent_id, c.content, c.user_id, c.status,
                   c.create_by, c.create_time, c.remark,
                   a.title AS article_title,
                   u.nick_name AS nick_name,
                   u.user_name AS user_name
            FROM blog_comment c
            LEFT JOIN blog_article a ON a.article_id = c.article_id
            LEFT JOIN sys_user u ON u.user_id = c.user_id
            WHERE c.comment_id = #{commentId}
            """)
    BlogComment selectCommentById(@Param("commentId") Long commentId);

    // 发表评论：parent_id / status 由 Service 兜底后传入，create_time 用数据库 NOW()，自增主键回填 commentId。
    @Insert("""
            INSERT INTO blog_comment
              (article_id, parent_id, content, user_id, status, create_by, create_time, remark)
            VALUES
              (#{articleId}, #{parentId}, #{content}, #{userId}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "commentId")
    int insertComment(BlogComment comment);

    /** 仅更新评论状态（审核：正常/隐藏） */
    // 不碰 content / user_id / article_id，所以审核动作不会影响 article.comment_count，无需重算计数。
    @Update("""
            UPDATE blog_comment
            SET status = #{status}, update_by = #{updateBy}, update_time = NOW()
            WHERE comment_id = #{commentId}
            """)
    int updateStatus(@Param("commentId") Long commentId,
                     @Param("status") String status,
                     @Param("updateBy") String updateBy);

    // 物理批量删除；不会级联删除 parent_id 指向它的回复，评论数的回减由 Service 逐条另行完成。
    @Delete("""
            <script>
            DELETE FROM blog_comment WHERE comment_id IN
            <foreach collection="array" item="commentId" open="(" separator="," close=")">
              #{commentId}
            </foreach>
            </script>
            """)
    int deleteCommentByIds(@Param("array") Long[] commentIds);
}
