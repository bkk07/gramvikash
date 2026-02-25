package com.learn.lld.gramvikash.diagnostic.controller;

import com.learn.lld.gramvikash.common.exception.ApiResponse;
import com.learn.lld.gramvikash.diagnostic.dto.DiagnosticRequest;
import com.learn.lld.gramvikash.diagnostic.dto.DiagnosticResponse;
import com.learn.lld.gramvikash.diagnostic.service.DiagnosticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
public class DiagnosticController {

    private final DiagnosticService diagnosticService;

    /**
     * Web diagnosis endpoint.
     * Accepts user text + optional image → classifies → RAG → LLM → response.
     */
    @PostMapping("/web")
    public ResponseEntity<ApiResponse> diagnoseWeb(
            @RequestParam("userQuery") String userQuery,
            @RequestParam(value = "farmerId", required = false) Long farmerId,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        DiagnosticRequest request = DiagnosticRequest.builder()
                .userQuery(userQuery)
                .farmerId(farmerId)
                .language(language)
                .region(region)
                .build();

        DiagnosticResponse response = diagnosticService.diagnoseWeb(request, image);
        return ResponseEntity.ok(new ApiResponse(200, "Diagnosis completed", response));
    }

    /**
     * Retrieve diagnosis history for a farmer.
     */
    @GetMapping("/history/{farmerId}")
    public ResponseEntity<ApiResponse> getDiagnosisHistory(@PathVariable Long farmerId) {
        return ResponseEntity.ok(new ApiResponse(
                200, "History retrieved", diagnosticService.getHistory(farmerId)
        ));
    }
}
