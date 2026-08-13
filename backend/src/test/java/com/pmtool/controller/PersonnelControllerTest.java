package com.pmtool.controller;

import com.alibaba.excel.EasyExcel;
import com.pmtool.common.Result;
import com.pmtool.dto.PersonnelImportDTO;
import com.pmtool.entity.Personnel;
import com.pmtool.service.PersonnelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 人员 Excel 导入校验单元测试
 */
@ExtendWith(MockitoExtension.class)
class PersonnelControllerTest {

    @Mock
    private PersonnelService personnelService;

    private PersonnelController controller;

    @BeforeEach
    void setUp() {
        controller = new PersonnelController();
        ReflectionTestUtils.setField(controller, "personnelService", personnelService);
    }

    @Test
    @DisplayName("空文件被拒绝")
    void emptyFileRejected() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "people.xlsx", "application/octet-stream", new byte[0]);

        Result<Integer> result = controller.importExcel(empty);

        assertEquals(500, result.getCode());
        assertEquals("请选择要上传的文件", result.getMessage());
    }

    @Test
    @DisplayName("非 Excel 格式被拒绝")
    void wrongExtensionRejected() throws Exception {
        MockMultipartFile txt = new MockMultipartFile(
                "file", "people.txt", "text/plain", "abc".getBytes());

        Result<Integer> result = controller.importExcel(txt);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains(".xlsx"));
    }

    @Test
    @DisplayName("有效 Excel 导入成功并返回导入数量")
    void validExcelImports() throws Exception {
        List<PersonnelImportDTO> rows = new ArrayList<>();
        PersonnelImportDTO row = new PersonnelImportDTO();
        row.setEmpNo("EMP100");
        row.setName("测试人员");
        row.setPositions("运维工程师");
        row.setSkills("Linux");
        rows.add(row);

        Personnel saved = new Personnel();
        saved.setId(100L);
        when(personnelService.importPersonnel(anyList())).thenReturn(List.of(saved));

        Result<Integer> result = controller.importExcel(excelFile(rows));

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData());
        verify(personnelService).importPersonnel(anyList());
    }

    @Test
    @DisplayName("Excel 中工号全部为空时被拒绝")
    void excelWithoutEmpNoRejected() throws Exception {
        List<PersonnelImportDTO> rows = new ArrayList<>();
        PersonnelImportDTO row = new PersonnelImportDTO();
        row.setName("无名氏");
        rows.add(row);

        Result<Integer> result = controller.importExcel(excelFile(rows));

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("工号不能为空"));
    }

    private MockMultipartFile excelFile(List<PersonnelImportDTO> rows) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).head(PersonnelImportDTO.class).sheet("人员").doWrite(rows);
        return new MockMultipartFile(
                "file", "人员导入.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }
}
