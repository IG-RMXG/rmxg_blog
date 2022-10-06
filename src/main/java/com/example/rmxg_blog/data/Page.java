package com.example.rmxg_blog.data;

import lombok.Data;

/**
 * @author 泥偶
 */
@Data
public class Page {

    /**
     * 当前页码和每页显示的数据数量
     */
    private Integer pageNumber = 1;
    private Integer pageSize = 10;
}
