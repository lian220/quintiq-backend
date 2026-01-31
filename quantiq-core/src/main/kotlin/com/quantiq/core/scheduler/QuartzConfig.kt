package com.quantiq.core.scheduler

import com.quantiq.core.adapter.input.scheduler.EconomicDataUpdateJobAdapter
import org.quartz.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

/**
 * Quartz 스케줄러 설정
 * 모든 Job과 Trigger를 등록합니다.
 */
@Configuration
class QuartzConfig {

    // ========================
    // 1. 경제 데이터 업데이트 (06:05)
    // ========================
    @Bean
    fun economicDataUpdateJobDetail(): JobDetail {
        return JobBuilder.newJob(EconomicDataUpdateJobAdapter::class.java)
            .withIdentity("economicDataUpdateJob")
            .storeDurably()
            .build()
    }

    @Bean
    fun economicDataUpdateTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(economicDataUpdateJobDetail())
            .withIdentity("economicDataUpdateTrigger")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 5 6 * * ?")
                    .inTimeZone(TimeZone.getTimeZone("Asia/Seoul"))
            )
            .build()
    }

    // ========================
    // 2. 병렬 분석 (23:05)
    // ========================
    @Bean
    fun parallelAnalysisJobDetail(): JobDetail {
        return JobBuilder.newJob(ParallelAnalysisJob::class.java)
            .withIdentity("parallelAnalysisJob")
            .storeDurably()
            .build()
    }

    @Bean
    fun parallelAnalysisTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(parallelAnalysisJobDetail())
            .withIdentity("parallelAnalysisTrigger")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 5 23 * * ?")
                    .inTimeZone(TimeZone.getTimeZone("Asia/Seoul"))
            )
            .build()
    }

}
