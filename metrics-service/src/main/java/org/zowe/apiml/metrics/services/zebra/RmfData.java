package org.zowe.apiml.metrics.services.zebra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RmfData {

    String title;
    String timestart;
    String timeend;
    List<String> columnhead;
    List<Map<String, String>> table;

}
