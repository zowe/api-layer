package org.zowe.apiml.metrics.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.metrics.services.zebra.RmfData;
import org.zowe.apiml.metrics.services.zebra.ZebraMetricsService;

import javax.ws.rs.QueryParam;

@RestController
@RequiredArgsConstructor
public class MetricServiceController {

    private final ZebraMetricsService zebraMetricsService;

    @GetMapping("/zebra/persistent-system-metrics")
    public RmfData getPersistentSystemMetrics(@QueryParam("lpar") String lpar, @QueryParam("report") String report) {
        return zebraMetricsService.getRmfData(lpar, report);
    }


}
