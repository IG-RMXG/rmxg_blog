package cn.CDPersonal.utils.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Excel数据格式处理适配器
 * 
 * 用法：在实体字段上写 @Excel(handler = 自定义实现.class, args = {...})，
 * 由读取该注解的导出/导入实现在运行时实例化并回调 format（实现类需保留无参构造）。
 *
 * @author ruoyi
 */
public interface ExcelHandlerAdapter
{
    /**
     * 格式化
     * 
     * @param value 单元格数据值
     * @param args excel注解args参数组
     * @param cell 单元格对象
     * @param wb 工作簿对象
     *
     * @return 处理后的值
     */
    // args 来自 @Excel.args（未配置时是空数组）；cell/wb 可用于直接改单元格样式。
    Object format(Object value, String[] args, Cell cell, Workbook wb);
}
