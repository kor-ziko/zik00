package com.zik00.shop.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.zik00.shop.config.WebClientOrigins;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ClientErrorController implements ErrorController {
    private static final byte[] API_ERROR =
            "{\"messages\":[\"요청을 처리하지 못했습니다.\"]}".getBytes(StandardCharsets.UTF_8);

    private final String clientErrorUrl;

    public ClientErrorController(WebClientOrigins webClientOrigins) {
        this.clientErrorUrl = webClientOrigins.clientBaseUrl() + "/error?status=";
    }

    @RequestMapping("/error")
    public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int status = resolveStatus(request);
        String failedPath = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (failedPath != null && failedPath.startsWith("/api/")) {
            writeApiError(response, status);
            return;
        }
        response.sendRedirect(clientErrorUrl + status);
    }

    private int resolveStatus(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (value instanceof Integer status && status >= 400 && status <= 599) {
            return status;
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private void writeApiError(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(API_ERROR.length);
        response.setHeader("Cache-Control", "no-store");
        response.getOutputStream().write(API_ERROR);
    }
}
