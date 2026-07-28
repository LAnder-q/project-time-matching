package com.pmtool.controller;

import com.pmtool.common.PageResult;
import com.pmtool.common.Result;
import com.pmtool.entity.Personnel;
import com.pmtool.service.PersonnelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人员管理 Controller
 * 注意：路径前缀 /api 由 context-path 提供，Controller 内不加 /api
 */
@RestController
@RequestMapping("/personnel")
public class PersonnelController {

    @Autowired
    private PersonnelService personnelService;

    /**
     * 分页查询人员
     */
    @GetMapping
    public Result<PageResult<Personnel>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String position) {
        return Result.ok(personnelService.pageQuery(pageNum, pageSize, name, position));
    }

    /**
     * 查询全部人员（不分页，供下拉选择使用）
     */
    @GetMapping("/list")
    public Result<List<Personnel>> list() {
        return Result.ok(personnelService.list());
    }

    /**
     * 根据ID查询人员
     */
    @GetMapping("/{id}")
    public Result<Personnel> getById(@PathVariable Long id) {
        return Result.ok(personnelService.getById(id));
    }

    /**
     * 新增人员
     */
    @PostMapping
    public Result<Personnel> create(@RequestBody Personnel personnel) {
        personnelService.save(personnel);
        return Result.ok(personnel);
    }

    /**
     * 更新人员
     */
    @PutMapping("/{id}")
    public Result<Personnel> update(@PathVariable Long id, @RequestBody Personnel personnel) {
        personnel.setId(id);
        personnelService.updateById(personnel);
        return Result.ok(personnel);
    }

    /**
     * 删除人员（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        personnelService.removeById(id);
        return Result.ok(null);
    }

    /**
     * 批量导入人员（工号存在则更新，不存在则新增）
     */
    @PostMapping("/batch")
    public Result<List<Personnel>> batchImport(@RequestBody List<Personnel> personnelList) {
        return Result.ok(personnelService.importPersonnel(personnelList));
    }
}
