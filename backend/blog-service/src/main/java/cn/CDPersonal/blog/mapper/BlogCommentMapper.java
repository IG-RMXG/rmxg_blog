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
 */
@Mapper
public interface BlogCommentMapper {

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

    @Insert("""
            INSERT INTO blog_comment
              (article_id, parent_id, content, user_id, status, create_by, create_time, remark)
            VALUES
              (#{articleId}, #{parentId}, #{content}, #{userId}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "commentId")
    int insertComment(BlogComment comment);

    /** 仅更新评论状态（审核：正常/隐藏） */
    @Update("""
            UPDATE blog_comment
            SET status = #{status}, update_by = #{updateBy}, update_time = NOW()
            WHERE comment_id = #{commentId}
            """)
    int updateStatus(@Param("commentId") Long commentId,
                     @Param("status") String status,
                     @Param("updateBy") String updateBy);

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
