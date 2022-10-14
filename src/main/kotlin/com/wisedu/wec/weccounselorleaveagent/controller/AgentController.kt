package com.wisedu.wec.weccounselorleaveagent.controller

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Duration

/**
 *
 * @author wjfu@wisedu.com
 */
@RestController
class AgentController(
    @Value("\${agent.openapi.school-code:20180611}")
    private val schoolCode: String,
    @Value("\${agent.openapi.app-id:162129959205702212}")
    private val appId: String,
    @Value("\${agent.openapi.secret:rStA8NC3vYjjEKVSQyZDyv+xy+lih8YjeukHg8ncOIiUhoc3cki6EDYxRz9PBTp2R0TCEXm7wajAMKE0LrVO1osYbrr7dKnT}")
    private val secret: String,
    @Value("\${agent.openapi.domain-url:https://wecmpapi.wisedu.com/devopsConfig/getOpenApiDomain}")
    private val openapiDomainUrl: String
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * http的请求客户端
     */
    private val client = HttpClient(OkHttp) {
        engine {
            pipelining = true
            clientCacheSize = 50
            threadsCount = 100
            config {
                connectTimeout(Duration.ofSeconds(3))
            }
        }
        install(ContentNegotiation) {
            jackson()
        }
    }

    /**
     * 通过配置加载区域的API Domain信息
     * 如果无法获取Domain则记录日志 启动失败
     */
    private val apiDomain = runBlocking {
        val timestamp = System.currentTimeMillis()
        val response = client.post(openapiDomainUrl) {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    Pair("schoolCode", schoolCode),
                    Pair("appId", appId),
                    Pair("secret", secret),
                    Pair("timestamp", timestamp),
                    Pair("sign", md5("$appId$schoolCode$secret$timestamp"))
                )
            )
        }
        val objectNode = response.body<ObjectNode>()
        logger.info("获取请求区域的API域名信息：\n${objectNode.toPrettyString()}")
        if (objectNode["domain"] == null || objectNode["domain"].isNull) {
            logger.error("无法通过配置【appId=${appId},schoolCode=${schoolCode},secret=${secret}】获取到Openapi的域名信息，检查配置是否正确。")
            throw RuntimeException("无法获取到API域名信息，启动失败。")
        }
        return@runBlocking objectNode["domain"].asText()
    }

    /**
     * 缓存token并且让token在
     * 7200-200秒的时间过期重新获取
     */
    private val tokenCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(7000))
        .maximumSize(100)
        .build { key ->
            runBlocking {
                val tokenResponse = client.post("$apiDomain/baseApi/getToken") {
                    contentType(ContentType.Application.Json)
                    val timestamp = System.currentTimeMillis()
                    setBody(
                        mapOf(
                            Pair("schoolCode", schoolCode),
                            Pair("appId", key),
                            Pair("secret", secret),
                            Pair("timestamp", timestamp),
                            Pair("sign", md5("$key$schoolCode$secret$timestamp"))
                        )
                    )
                }
                val accessToken = tokenResponse.body<ObjectNode>()["accessToken"].asText()
                logger.info("Loading accessToken with expire 7200s: [$accessToken]")
                return@runBlocking accessToken
            }
        }

    /**
     * 通行日志上报
     *
     * @param report 上报日志
     * @return 代理的后台服务响应原信息
     */
    @PostMapping("/")
    fun report(@RequestBody report: ObjectNode) = runBlocking {
        try {
            val personId = report["person_id"]?.asText()
            val personName = report["person_name"]?.asText()
            val personCode = report["person_code"]?.asText()
            logger.info("获取用户【id=$personId,name=$personName,code=$personCode】的鉴权信息。")
            // 如果personCode为空则直接走例外放行
            if (personCode.isNullOrBlank()) {
                return@runBlocking mapOf<String, Any>(Pair("code", 1))
            }
            var retObject = proxy2Openapi(report)
            // 如果响应失效需要重新获取一次accessToken再做请求
            if (retObject.has("errCode")) {
                logger.error("请求失败：\n${retObject.toPrettyString()}")
                if (retObject["errCode"].asText() == "4105040001") {
                    // 刷新accessToken重试 如果还不行则认为有网络问题或其他的问题
                    tokenCache.refresh(appId)
                    retObject = proxy2Openapi(report)
                    if (retObject.has("errCode") && retObject["errCode"].asText() != "0") {
                        logger.error("获取服务器鉴权信息失败：\n${retObject.toPrettyString()}")
                        return@runBlocking mapOf<String, Any>(Pair("code", 4))
                    }
                }
            }
            return@runBlocking retObject
        } catch (e: Exception) {
            logger.error("请求服务异常：" + e.message, e)
            return@runBlocking mapOf<String, Any>(Pair("code", 1))
        }
    }

    /**
     * 代理到实际的API服务
     * @param report 请求实体
     * @return 上报鉴权的响应结果
     */
    private suspend fun proxy2Openapi(report: ObjectNode): ObjectNode {
        val postRet = client.post("$apiDomain/wec-apis/leave/report") {
            headers {
                set("appId", appId)
                set("accessToken", tokenCache[appId].orEmpty())
            }
            contentType(ContentType.Application.Json)
            setBody(report)
        }
        return postRet.body()
    }

    /**
     * MD5加密
     */
    fun md5(str: String): String = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8)).toHex()

    /**
     * ByteArray转字符串
     */
    fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

}