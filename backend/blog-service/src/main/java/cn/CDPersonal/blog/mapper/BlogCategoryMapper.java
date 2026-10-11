package cn.CDPersonal.blog.mapper;

import cn.CDPersonal.blog.domain.BlogCategory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 博客分类 Mapper（blog_category）
 */
@Mapper
public interface BlogCategoryMapper {

    @Select("""
            <script>
            SELECT c.category_id, c.category_name, c.sort, c.status,
                   c.create_by, c.create_time, c.update_by, c.update_time, c.remark,
                   (SELECT COUNT(*) FROM blog_article a WHERE a.category_id = c.category_id) AS article_count
            FROM blog_category c
            <where>
              <if test="categoryName != null and categoryName != ''">
                AND c.category_name LIKE CONCAT('%', #{categoryName}, '%')
              </if>
              <if test="status != null and status != ''">
                AND c.status = #{status}
              </if>
            </where>
            ORDER BY c.sort ASC, c.category_id ASC
            </script>
            """)
    List<BlogCategory> selectCategoryList(BlogCategory query);

    /** 启用的分类（文章编辑页下拉用） */
    @Select("""
            SELECT category_id, category_name, sort, status
            FROM blog_category
            WHERE status = '0'
            ORDER BY sort ASC, category_id ASC
            """)
    List<BlogCategory> selectEnabledList();

    @Select("""
            SELECT c.category_id, c.category_name, c.sort, c.status,
                   c.create_by, c.create_time, c.update_by, c.update_time, c.remark,
                   (SELECT COUNT(*) FROM blog_article a WHERE a.category_id = c.category_id) AS article_count
            FROM blog_category c
            WHERE c.category_id = #{categoryId}
            """)
    BlogCategory selectCategoryById(@Param("categoryId") Long categoryId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM blog_category
            WHERE category_name = #{categoryName}
            <if test="categoryId != null">AND category_id != #{categoryId}</if>
            </script>
            """)
    int checkNameUnique(BlogCategory category);

    @Insert("""
            INSERT INTO blog_category
              (category_name, sort, status, create_by, create_time, remark)
            VALUES
              (#{categoryName}, #{sort}, #{status}, #{createBy}, NOW(), #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "categoryId")
    int insertCategory(BlogCategory category);

    @Update("""
            UPDATE blog_category
            SET category_name = #{categoryName},
                sort = #{sort},
                status = #{status},
                update_by = #{updateBy},
                update_time = NOW(),
                remark = #{remark}
            WHERE category_id = #{categoryId}
            """)
    int updateCategory(BlogCategory category);

    @Delete("""
            <script>
            DELETE FROM blog_category WHERE category_id IN
            <foreach collection="array" item="categoryId" open="(" separator="," close=")">
              #{categoryId}
            </foreach>
            </script>
            """)
    int deleteCategoryByIds(@Param("array") Long[] categoryIds);
}
