package org.openoa.controller; // 必须与目录结构匹配

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/drive") // 与前端约定的接口前缀
public class DingTalkController {

    // 从配置文件读取钉钉凭证
    @Value("${dingtalk.corpid}")
    private String corpId;
    @Value("${dingtalk.corpsecret}")
    private String corpSecret;

    // 注入RestTemplate（需确保Spring上下文已配置）
    private final RestTemplate restTemplate;
    public DingTalkController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String accessToken;
    private long tokenExpireTime = 0;

    // 自动刷新AccessToken
    private String getAccessToken() {
        if (System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }
        // 请求钉钉获取Token
        String tokenUrl = String.format(
            "https://oapi.dingtalk.com/gettoken?corpid=%s&corpsecret=%s",
            corpId, corpSecret
        );
        Map<String, Object> tokenResp = restTemplate.getForObject(tokenUrl, Map.class);
        accessToken = tokenResp.get("access_token").toString();
        tokenExpireTime = System.currentTimeMillis() + 7000 * 1000; // 提前200秒刷新
        return accessToken;
    }

    // 获取钉盘文件列表（支持分页）
    @GetMapping("/spaces/{spaceId}/files")
    public Map<String, Object> getFileList(
        @PathVariable String spaceId,
        @RequestParam(defaultValue = "0") int offset,  // 分页偏移
        @RequestParam(defaultValue = "100") int limit   // 分页大小
    ) {
        String url = String.format(
            "https://api.dingtalk.com/v1.0/drive/spaces/%s/files?offset=%d&limit=%d",
            spaceId, offset, limit
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-acs-dingtalk-access-token", getAccessToken());
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, Map.class
        );
        return response.getBody();
    }

    // 获取文件下载链接
    @GetMapping("/files/{fileId}/download")
    public Map<String, String> getDownloadUrl(@PathVariable String fileId) {
        String url = String.format(
            "https://api.dingtalk.com/v1.0/drive/files/%s/download",
            fileId
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-acs-dingtalk-access-token", getAccessToken());
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, Map.class
        );
        Map<String, String> result = new HashMap<>();
        result.put("downloadUrl", response.getBody().get("downloadUrl").toString());
        return result;
    }
}