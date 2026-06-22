package com.flowmind.form.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库字段批量导入请求。
 */
@Data
public class DatabaseFieldImportRequest {

    private String sourceTable;

    private List<DatabaseFieldImportItem> columns = new ArrayList<>();
}
