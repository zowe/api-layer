package org.zowe.apiml.metrics.services.zebra;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.metrics.RmfData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ZebraMetricsService {

    private final String zebraBaseUrl;
    private final RestTemplate restTemplate;

    public RmfData getRmfData(String lpar, String report) {
        String url = UriComponentsBuilder.fromUriString(zebraBaseUrl).pathSegment(lpar).pathSegment("rmf3").pathSegment(report).toUriString();

        RmfData metricsData = restTemplate.getForObject(url, RmfData.class);
        Map<String, String> map = new HashMap<>();
        map.put("STRPJOB", "1");
        //RmfData metricsData = new RmfData("STOR (Storage Delays)", "03/31/2022 16:08:20", "03/31/2022 16:10:00", null, Collections.singletonList(map));


        if (metricsData != null) {
            metricsData.setTimestart(formatZebraTimeStampsToISO8061(metricsData.getTimestart()));
            metricsData.setTimeend(formatZebraTimeStampsToISO8061(metricsData.getTimeend()));
        }

        return metricsData;
    }

    private String formatZebraTimeStampsToISO8061(String timeStamp) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);

        final int numberOfTimeElements = 6;
        int[] timeStampElements = new int[numberOfTimeElements];

        Matcher matcher = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE).matcher(timeStamp);
        for (int i = 0; matcher.find(); i++) {
            timeStampElements[i] = Integer.parseInt(matcher.group(0));
        }

        int month = timeStampElements[0] - 1;
        int day = timeStampElements[1];
        int year = timeStampElements[2];
        int hour = timeStampElements[3];
        int minute = timeStampElements[4];
        int second = timeStampElements[5];

        GregorianCalendar convertedTimeStamp = new GregorianCalendar(year, month, day, hour, minute, second);

        return df.format(convertedTimeStamp.getTime());
    }


}
