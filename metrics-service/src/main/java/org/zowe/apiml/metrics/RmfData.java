package org.zowe.apiml.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
public class RmfData {

    String title;
    String timestart;
    String timeend;
    List<String> columnhead;
    List<Map<String, String>> table;

}
