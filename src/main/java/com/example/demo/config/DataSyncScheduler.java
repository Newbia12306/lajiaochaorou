package com.example.demo.config;

import com.example.demo.service.DataSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataSyncScheduler.class);
    private final DataSyncService dataSyncService;

    public DataSyncScheduler(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    @Scheduled(fixedRate = 1800000)
    public void scheduleDataSync() {
        log.info("触发定时数据同步任务");
        dataSyncService.syncAllDishesToElasticsearch();
    }
}
