package org.openoa.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉文件访问控制器：用于获取钉盘文件列表和下载链接
 * 存放路径：antflow-web/src/main/java/org/openoa/controller/DingTalkController.java
 */
@RestController
@RequestMapping("/api/drive") // 前端调用的接口前缀
public class DingTalkController {

    // 从配置文件读取企业凭证（需在application.yml中配置）
    @Value("${dingtalk.corpid}")
    private String corpId;

    @Value("${dingtalk.corpsecret}")
    private String corpSecret;

    // 注入RestTemplate（已在WebConfig中配置）
    private final RestTemplate restTemplate;
    public DingTalkController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 缓存的AccessToken和过期时间
    private String accessToken;
    private long tokenExpireTime = 0;


    /**
     * 获取AccessToken（自动刷新）
     * 如果你使用个人钉盘Token，可直接返回个人Token（替换下面的逻辑）
     */
    private String getAccessToken() {
        // 1. 检查Token是否未过期，直接返回
        if (System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        // 2. Token过期，重新获取（企业级方式）
        try {
            String tokenUrl = String.format(
                "https://oapi.dingtalk.com/gettoken?corpid=%s&corpsecret=%s",
                corpId, corpSecret // 从配置文件读取的凭证
            );
            // 调用钉钉接口获取Token
            Map<String, Object> tokenResp = restTemplate.getForObject(tokenUrl, Map.class);
            
            // 处理响应：若成功则更新Token和过期时间
            if (tokenResp.get("errcode").toString().equals("0")) {
                accessToken = tokenResp.get("access_token").toString();
                // 有效期7200秒，提前200秒刷新
                tokenExpireTime = System.currentTimeMillis() + 7000 * 1000;
                return accessToken;
            } else {
                // 打印错误信息（方便调试）
                System.err.println("获取Token失败：" + tokenResp.get("errmsg"));
                return null;
            }
        } catch (Exception e) {
            System.err.println("获取Token时发生异常：" + e.getMessage());
            return null;
        }
    }


    /**
     * 获取钉盘文件列表（支持分页）
     * 前端调用示例：/api/drive/spaces/你的spaceId/files?offset=0&limit=100
     */
    @GetMapping("/spaces/{spaceId}/files")
    public Map<String, Object> getFileList(
            @PathVariable String spaceId, // 钉盘空间ID（必须修改为你的实际spaceId）
            @RequestParam(defaultValue = "0") int offset, // 分页起始位置（从0开始）
            @RequestParam(defaultValue = "100") int limit) { // 每页数量（最大100）
        
        // 1. 检查Token是否有效
        String token = getAccessToken();
        if (token == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("errcode", -1);
            error.put("errmsg", "获取AccessToken失败，请检查凭证");
            return error;
        }

        // 2. 调用钉钉API获取文件列表
        try {
            String url = String.format(
                "https://api.dingtalk.com/v1.0/drive/spaces/%s/files?offset=%d&limit=%d",
                spaceId, offset, limit
            );
            // 设置请求头（携带Token）
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-acs-dingtalk-access-token", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            // 发送请求并返回结果
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("errcode", -2);
            error.put("errmsg", "获取文件列表失败：" + e.getMessage());
            return error;
        }
    }


    /**
     * 获取文件下载链接
     * 前端调用示例：/api/drive/files/文件ID/download
     */
    @GetMapping("/files/{fileId}/download")
    public Map<String, Object> getDownloadUrl(@PathVariable String fileId) {
        // 1. 检查Token是否有效
        String token = getAccessToken();
        if (token == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("errcode", -1);
            error.put("errmsg", "获取AccessToken失败，请检查凭证");
            return error;
        }

        // 2. 调用钉钉API获取下载链接
        try {
            String url = String.format(
                "https://api.dingtalk.com/v1.0/drive/files/%s/download",
                fileId
            );
            // 设置请求头（携带Token）
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-acs-dingtalk-access-token", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            // 发送请求并返回结果
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("errcode", -3);
            error.put("errmsg", "获取下载链接失败：" + e.getMessage());
            return error;
        }
    }
}