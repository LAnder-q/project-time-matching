package com.pmtool.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树形 VO
 */
@Data
public class DepartmentVO {

    private Long id;

    private String name;

    private String code;

    private Long parentId;

    private Integer sort;

    /** 子部门列表 */
    private List<DepartmentVO> children = new ArrayList<>();
}
