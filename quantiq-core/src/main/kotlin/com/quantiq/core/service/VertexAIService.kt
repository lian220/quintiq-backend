package com.quantiq.core.service

import com.google.cloud.aiplatform.v1.*
import com.google.cloud.storage.Storage
import com.quantiq.core.adapter.output.notification.slack.SlackApiClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value as SpringValue
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Google Vertex AI CustomJob 관리 서비스
 * GCS에 업로드된 패키지를 사용하여 Vertex AI CustomJob 실행
 */
@Service
class VertexAIService(
    private val jobServiceClient: JobServiceClient,
    private val storageClient: Storage,
    private val slackApiClient: SlackApiClient,
    @SpringValue("\${gcp.project-id}") private val projectId: String,
    @SpringValue("\${gcp.region}") private val region: String,
    @SpringValue("\${gcp.vertex-ai.staging-bucket}") private val stagingBucket: String,
    @SpringValue("\${gcp.vertex-ai.model-bucket}") private val modelBucket: String,
    @SpringValue("\${gcp.vertex-ai.package-uri:}") private val packageUri: String?,
    @SpringValue("\${gcp.vertex-ai.job-name}") private val jobName: String,
    @SpringValue("\${gcp.vertex-ai.machine-type}") private val machineType: String,
    @SpringValue("\${gcp.vertex-ai.accelerator-type}") private val acceleratorType: String,
    @SpringValue("\${gcp.vertex-ai.accelerator-count}") private val acceleratorCount: Int,
    @SpringValue("\${gcp.vertex-ai.max-wait-duration}") private val maxWaitDuration: Int,
    @SpringValue("\${gcp.vertex-ai.timeout}") private val timeout: Int,
    @SpringValue("\${gcp.vertex-ai.container-uri}") private val containerUri: String,
    @SpringValue("\${spring.data.mongodb.uri}") private val mongodbUri: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * .env.prod 파일에서 환경변수 읽기
     */
    private fun loadEnvFile(): Map<String, String> {
        // 프로젝트 루트 디렉토리에서 .env.prod 찾기
        val currentDir = java.io.File(System.getProperty("user.dir"))
        val envFile = if (currentDir.name == "quantiq-core") {
            // quantiq-core 디렉토리에서 실행된 경우, 부모 디렉토리로 이동
            java.io.File(currentDir.parentFile, ".env.prod")
        } else {
            java.io.File(currentDir, ".env.prod")
        }

        if (!envFile.exists()) {
            logger.warn(".env.prod 파일을 찾을 수 없습니다: ${envFile.absolutePath}")
            return emptyMap()
        }

        val envVars = mutableMapOf<String, String>()
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    envVars[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        logger.info(".env.prod 파일에서 ${envVars.size}개 환경변수 로드 완료: ${envFile.absolutePath}")
        return envVars
    }

    /**
     * Vertex AI CustomJob 생성 및 실행
     * GCS에 업로드된 패키지를 사용하여 Vertex AI에서 직접 실행
     *
     * @return Job 이름
     */
    fun createAndRunCustomJob(): String {
        val requestId = UUID.randomUUID().toString()

        logger.info("=" .repeat(60))
        logger.info("🚀 Vertex AI 주가 예측 작업 시작")
        logger.info("Request ID: $requestId")
        logger.info("=" .repeat(60))

        try {
            // 1. Slack 시작 알림 및 threadTs 받기
            val threadTs = slackApiClient.notifyVertexAIJobStarted(requestId, jobName)

            // 2. 패키지 URI 결정
            val resolvedPackageUri = resolvePackageUri()
            logger.info("패키지 URI: $resolvedPackageUri")

            // 3. 환경 변수 설정 (threadTs 포함)
            val envVars = buildEnvironmentVariables(threadTs)

            // 4. CustomJob 생성
            val customJob = buildCustomJob(resolvedPackageUri, envVars)

            // 5. Job 실행
            val parent = LocationName.of(projectId, region).toString()
            val createdJob = jobServiceClient.createCustomJob(parent, customJob)
            val fullJobName = createdJob.name

            logger.info("=" .repeat(60))
            logger.info("✅ Vertex AI CustomJob 생성 완료")
            logger.info("Job Name: $fullJobName")
            logger.info("Job State: ${createdJob.state}")
            logger.info("Slack ThreadTs: $threadTs")
            logger.info("Console: https://console.cloud.google.com/vertex-ai/training/custom-jobs/${fullJobName}?project=$projectId")
            logger.info("=" .repeat(60))

            return fullJobName

        } catch (e: Exception) {
            logger.error("❌ Vertex AI CustomJob 실행 실패", e)
            slackApiClient.notifyVertexAIJobFailed(requestId, jobName, e.message ?: "Unknown error", null)
            throw RuntimeException("Vertex AI CustomJob 실행 실패", e)
        }
    }

    /**
     * 버킷 존재 확인 및 생성
     */
    private fun ensureBucketExists(bucketName: String) {
        val bucket = storageClient.get(bucketName)
        if (bucket == null) {
            logger.warn("버킷이 없습니다. 수동으로 생성해주세요: $bucketName")
            throw IllegalStateException("GCS 버킷이 존재하지 않습니다: $bucketName")
        }
        logger.info("✅ 버킷 확인 완료: $bucketName")
    }

    /**
     * 최신 패키지 URI 조회
     */
    private fun resolvePackageUri(): String {
        // 환경 변수로 명시된 경우
        if (!packageUri.isNullOrBlank()) {
            return packageUri
        }

        // GCS에서 최신 버전 조회
        val bucket = storageClient.get(stagingBucket)
        val versionBlob = bucket.get("predict-package-version.json")

        if (versionBlob == null) {
            throw IllegalStateException("GCS에 패키지 버전 정보가 없습니다. upload_to_gcs.py를 먼저 실행하세요.")
        }

        val versionJson = versionBlob.getContent().toString(Charsets.UTF_8)
        val versionData = com.fasterxml.jackson.databind.ObjectMapper().readTree(versionJson)
        val latestVersion = versionData.get("version").asInt()

        val packagePath = "packages/predict-package-v$latestVersion.tar.gz"
        val packageBlob = bucket.get(packagePath)

        if (packageBlob == null) {
            throw IllegalStateException("GCS에 패키지 파일이 없습니다: $packagePath")
        }

        logger.info("✅ 최신 버전 패키지 발견: v$latestVersion")
        return "gs://$stagingBucket/$packagePath"
    }

    /**
     * 환경 변수 빌드
     * ⚠️ VERTEX AI 전용: 무조건 .env.prod 파일만 사용
     * Spring 설정은 절대 사용하지 않음 (로컬 MongoDB 방지)
     */
    private fun buildEnvironmentVariables(threadTs: String?): Map<String, String> {
        val envVars = mutableMapOf(
            "GCS_BUCKET" to modelBucket,
            "GCP_PROJECT_ID" to projectId,
            "FINE_TUNE_MODE" to "true",
            "FINE_TUNE_EPOCHS" to "5",
            "FULL_TRAIN_EPOCHS" to "50"
        )

        // Slack threadTs 추가
        threadTs?.let {
            envVars["SLACK_THREAD_TS"] = it
            logger.info("SLACK_THREAD_TS 환경변수 추가됨: $it")
        }

        // ⚠️ CRITICAL: .env.prod 파일에서만 MongoDB 환경변수 로드
        logger.info("=" .repeat(60))
        logger.info("⚠️ VERTEX AI 전용: .env.prod 파일만 사용")
        logger.info("=" .repeat(60))

        val envFile = loadEnvFile()

        if (envFile.isEmpty()) {
            logger.error("❌ .env.prod 파일을 읽을 수 없습니다!")
            throw IllegalStateException(".env.prod 파일을 찾을 수 없거나 비어있습니다. Vertex AI 실행 불가.")
        }

        // MongoDB 필수 환경변수 로드 (.env.prod에서만)
        val mongoUrl = envFile["MONGO_URL"]
            ?: throw IllegalStateException("❌ .env.prod에 MONGO_URL이 없습니다!")

        val mongoUser = envFile["MONGO_USER"]
            ?: throw IllegalStateException("❌ .env.prod에 MONGO_USER가 없습니다!")

        val mongoPassword = envFile["MONGO_PASSWORD"]
            ?: throw IllegalStateException("❌ .env.prod에 MONGO_PASSWORD가 없습니다!")

        val mongoDatabase = envFile["MONGODB_DATABASE"] ?: "stock_trading"

        // 환경변수 추가
        envVars["MONGO_URL"] = mongoUrl
        envVars["MONGO_USER"] = mongoUser
        envVars["MONGO_PASSWORD"] = mongoPassword
        envVars["MONGODB_DATABASE"] = mongoDatabase

        logger.info("✅ MONGO_URL: $mongoUrl")
        logger.info("✅ MONGO_USER: $mongoUser")
        logger.info("✅ MONGO_PASSWORD: ***")
        logger.info("✅ MONGODB_DATABASE: $mongoDatabase")

        // Slack 환경변수 추가
        envFile["SLACK_BOT_TOKEN"]?.let {
            envVars["SLACK_BOT_TOKEN"] = it
            logger.info("✅ SLACK_BOT_TOKEN 환경변수 추가됨 (***)")
        }
        envFile["SLACK_CHANNEL"]?.let {
            envVars["SLACK_CHANNEL"] = it
            logger.info("✅ SLACK_CHANNEL 환경변수 추가됨: $it")
        }

        logger.info("=" .repeat(60))
        logger.info("📋 총 환경 변수 ${envVars.size}개 설정 완료")
        logger.info("=" .repeat(60))

        return envVars
    }

    /**
     * MongoDB URI 파싱
     * mongodb://username:password@host:port/database?options
     * mongodb+srv://username:password@host/database?options
     */
    private data class MongoDbUriParts(
        val host: String,
        val username: String,
        val password: String,
        val database: String,
        val isSrv: Boolean = false
    )

    private fun parseMongoDbUri(uri: String): MongoDbUriParts {
        // mongodb:// 또는 mongodb+srv:// 제거
        val isSrv = uri.startsWith("mongodb+srv://")
        val cleanUri = uri.removePrefix("mongodb://").removePrefix("mongodb+srv://")

        var username = ""
        var password = ""
        var host = ""
        var database = "stock_trading" // 기본값

        // @ 기준으로 인증 부분과 호스트 부분 분리
        if ("@" in cleanUri) {
            val parts = cleanUri.split("@", limit = 2)
            val authPart = parts[0]
            val hostPart = parts[1]

            // 인증 정보 파싱
            if (":" in authPart) {
                val authParts = authPart.split(":", limit = 2)
                username = java.net.URLDecoder.decode(authParts[0], "UTF-8")
                password = java.net.URLDecoder.decode(authParts[1], "UTF-8")
            }

            // 호스트 및 데이터베이스 파싱
            val hostAndDb = hostPart.split("?")[0] // 옵션 제거
            val hostDbParts = hostAndDb.split("/")
            host = hostDbParts[0]

            // mongodb+srv:// 프로토콜은 포트를 포함할 수 없으므로 포트 제거
            if (isSrv && ":" in host) {
                host = host.substringBefore(":")
                logger.warn("⚠️ mongodb+srv:// URI에서 포트 번호 제거됨: $host")
            }

            if (hostDbParts.size > 1 && hostDbParts[1].isNotEmpty()) {
                database = hostDbParts[1]
            }
        } else {
            // @ 없으면 인증 없음
            val hostAndDb = cleanUri.split("?")[0]
            val hostDbParts = hostAndDb.split("/")
            host = hostDbParts[0]

            // mongodb+srv:// 프로토콜은 포트를 포함할 수 없으므로 포트 제거
            if (isSrv && ":" in host) {
                host = host.substringBefore(":")
                logger.warn("⚠️ mongodb+srv:// URI에서 포트 번호 제거됨: $host")
            }

            if (hostDbParts.size > 1 && hostDbParts[1].isNotEmpty()) {
                database = hostDbParts[1]
            }
        }

        return MongoDbUriParts(host, username, password, database, isSrv)
    }

    /**
     * 비밀번호 마스킹
     */
    private fun maskPassword(uri: String): String {
        return uri.replace(Regex(":([^@]+)@"), ":***@")
    }

    /**
     * CustomJob 빌드
     *
     * TODO: Command Line Arguments 파라미터 지원 추가
     *       - args: List<String> 파라미터 추가
     *       - pythonPackageSpec.addAllArgs(args) 호출
     *       - API에서 동적 파라미터 전달 가능하도록 수정
     */
    private fun buildCustomJob(packageUri: String, envVars: Map<String, String>): CustomJob {
        // 환경 변수를 EnvVar 리스트로 변환
        val envVarList = envVars.map { (key, value) ->
            EnvVar.newBuilder()
                .setName(key)
                .setValue(value)
                .build()
        }

        // PythonPackageSpec 생성
        val pythonPackageSpec = PythonPackageSpec.newBuilder()
            .setExecutorImageUri(containerUri)
            .addPackageUris(packageUri)
            .setPythonModule("aiplatform_custom_trainer_script.task")
            .addAllEnv(envVarList)
            .build()

        // MachineSpec 생성
        val machineSpec = MachineSpec.newBuilder()
            .setMachineType(machineType)
            .setAcceleratorType(AcceleratorType.valueOf(acceleratorType))
            .setAcceleratorCount(acceleratorCount)
            .build()

        // WorkerPoolSpec 생성
        val workerPoolSpec = WorkerPoolSpec.newBuilder()
            .setMachineSpec(machineSpec)
            .setReplicaCount(1)
            .setPythonPackageSpec(pythonPackageSpec)
            .build()

        // JobSpec 생성
        val jobSpec = CustomJobSpec.newBuilder()
            .addWorkerPoolSpecs(workerPoolSpec)
            .build()

        // CustomJob 생성
        return CustomJob.newBuilder()
            .setDisplayName(jobName)
            .setJobSpec(jobSpec)
            .build()
    }

    /**
     * Job 완료 대기 (비동기)
     */
    private fun waitForJobCompletion(jobId: String) {
        logger.info("Job 완료 대기 중... (최대 ${timeout}초)")

        val startTime = System.currentTimeMillis()
        val timeoutMillis = timeout * 1000L

        while (true) {
            val job = jobServiceClient.getCustomJob(jobId)
            val state = job.state

            logger.info("Job 상태: $state")

            when (state) {
                JobState.JOB_STATE_SUCCEEDED -> {
                    logger.info("✅ Job 성공적으로 완료")
                    return
                }
                JobState.JOB_STATE_FAILED -> {
                    logger.error("❌ Job 실패: ${job.error}")
                    throw RuntimeException("Vertex AI Job 실패: ${job.error}")
                }
                JobState.JOB_STATE_CANCELLED -> {
                    logger.warn("⚠️ Job 취소됨")
                    throw RuntimeException("Vertex AI Job 취소됨")
                }
                else -> {
                    // 계속 대기
                    if (System.currentTimeMillis() - startTime > timeoutMillis) {
                        logger.error("❌ Job 타임아웃 (${timeout}초 초과)")
                        throw RuntimeException("Vertex AI Job 타임아웃")
                    }

                    Thread.sleep(30000) // 30초 대기
                }
            }
        }
    }

    /**
     * Job 상태 조회
     */
    fun getJobState(jobId: String): JobState {
        val job = jobServiceClient.getCustomJob(jobId)
        return job.state
    }

    /**
     * Job 취소
     */
    fun cancelJob(jobId: String) {
        jobServiceClient.cancelCustomJob(jobId)
        logger.info("Job 취소 요청 완료: $jobId")
    }
}
