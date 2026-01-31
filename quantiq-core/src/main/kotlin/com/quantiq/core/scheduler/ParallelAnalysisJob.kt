package com.quantiq.core.scheduler

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 병렬 분석 Job
 * 매일 23:05에 실행 (기술적 지표 + 감정 분석)
 */
@Component
class ParallelAnalysisJob : Job {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun execute(context: JobExecutionContext?) {
        try {
            logger.info("=".repeat(60))
            logger.info("병렬 분석 시작 (23:05)")
            logger.info("=".repeat(60))

            // TODO: 병렬 분석 로직 구현
            // - 기술적 지표 분석
            // - 감정 분석
            // parallelAnalysisService.runParallelAnalysis()

            logger.info("병렬 분석 완료")
            logger.info("=".repeat(60))
        } catch (e: Exception) {
            logger.error("병렬 분석 실패", e)
            throw JobExecutionException(e)
        }
    }
}
