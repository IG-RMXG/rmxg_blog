package cn.CDPersonal.blog.mapper;

import cn.CDPersonal.blog.domain.BlogTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 博客标签 Mapper（blog_tag）
 */
@Mapper
public interface BlogTagMapper {

    @Select("""
            <script>
            SELECT t.tag_id, t.tag_name, t.sort,
                   t.create_by, t.create_time, t.update_by, t.update_time, t.remark,
                   (SELECT COUNT(*) FROM blog_article_tag at2 WHERE at2.tag_id = t.tag_id) AS article_count
            FROM blog_tag t
            <where>
              <if test="tagName != null and tagName != ''">
                AND t.tag_name LIKE CONCAT('%', #{tagName}, '%')
              </if>
            </where>
            ORDER BY t.sort ASC, t.tag_id ASC
            </script>
            """)
    List<BlogTag> selectTagList(BlogTag query);

    /** 全部标签（文章编辑页下拉用） */
    @Select("""
            SELECT tag_id, tag_name, sort
            FROM blog_tag
            ORDER BY sort ASC, tag_id ASC
            """)
    List<BlogTag> selectAllList();

    @Select("""
            SELECT t.tag_id, t.tag_name, t.sort,
                   t.create_by, t.create_time, t.update_by, t.update_time, t.remark,
                   (SELECT COUNT(*) FROM blog_article_tag at2 WHERE at2.tag_id = t.tag_id) AS article_count
            FROM blog_tag t
            WHERE t.tag_id = #{tagId}
            """)
    BlogTag selectTagById(@Param("tagId") Long tagId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM blog_tag
            WHERE tag_name = #{tagName}
            <if test="tagId != null">AND tag_id != #{tagId}</if>
            </script>
            """)
    int checkNameUnique(BlogTag tag);

    @Insert("""
            INSERT INTO blog_tag
              (tag_name, sort, create_by, create_time, remark)
            VALUES
              (#{tagName}, #{sort}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "tagId")
    int insertTag(BlogTag tag);

    @Update("""
            UPDATE blog_tag
            SET tag_name = #{tagName},
                sort = #{sort},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE tag_id = #{tagId}
            """)
    int updateTag(BlogTag tag);

    @Delete("""
            <script>
            DELETE FROM blog_tag WHERE tag_id IN
            <foreach collection="array" item="tagId" open="(" separator="," close=")">
              #{tagId}
            </foreach>
            </script>
            """)
    int deleteTagByIds(@Param("array") Long[] tagIds);

    /** 删除标签时同步清理文章-标签关联 */
    @Delete("""
            <script>
            DELETE FROM blog_article_tag WHERE tag_id IN
            <foreach collection="array" item="tagId" open="(" separator="," close=")">
              #{tagId}
            </foreach>
            </script>
            """)
    int deleteArticleTagByTagIds(@Param("array") Long[] tagIds);
}
