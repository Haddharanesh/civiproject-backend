package com.civic.civicbackend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Service for complaint classification and duplicate detection
 * Calls the Python FastAPI service running on localhost:8000
 */
@Service
public class AiService {

    private final RestTemplate restTemplate;
    
    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public AiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Request DTO for AI analysis
     */
    public static class AnalyzeRequest {
        private String text;
        private java.util.List<String> existing_texts;

        public AnalyzeRequest(String text, java.util.List<String> existing_texts) {
            this.text = text;
            this.existing_texts = existing_texts;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public java.util.List<String> getExisting_texts() { return existing_texts; }
        public void setExisting_texts(java.util.List<String> existing_texts) { this.existing_texts = existing_texts; }
    }

    /**
     * Response DTO from AI analysis
     */
    public static class AnalyzeResponse {
        private String predicted_department;
        private Boolean is_duplicate;
        private Integer duplicate_index;

        public String getPredicted_department() { return predicted_department; }
        public void setPredicted_department(String predicted_department) { this.predicted_department = predicted_department; }
        public Boolean getIs_duplicate() { return is_duplicate; }
        public void setIs_duplicate(Boolean is_duplicate) { this.is_duplicate = is_duplicate; }
        public Integer getDuplicate_index() { return duplicate_index; }
        public void setDuplicate_index(Integer duplicate_index) { this.duplicate_index = duplicate_index; }
    }

    /**
     * Result wrapper with fallback values
     */
    public static class AiAnalysisResult {
        private String predictedDepartment;
        private boolean isDuplicate;
        private Integer duplicateIndex;

        public AiAnalysisResult() {
            // Default fallback values
            this.predictedDepartment = "General";
            this.isDuplicate = false;
            this.duplicateIndex = null;
        }

        public AiAnalysisResult(String predictedDepartment, boolean isDuplicate, Integer duplicateIndex) {
            this.predictedDepartment = predictedDepartment;
            this.isDuplicate = isDuplicate;
            this.duplicateIndex = duplicateIndex;
        }

        public String getPredictedDepartment() { return predictedDepartment; }
        public void setPredictedDepartment(String predictedDepartment) { this.predictedDepartment = predictedDepartment; }
        public boolean isDuplicate() { return isDuplicate; }
        public void setDuplicate(boolean duplicate) { isDuplicate = duplicate; }
        public Integer getDuplicateIndex() { return duplicateIndex; }
        public void setDuplicateIndex(Integer duplicateIndex) { this.duplicateIndex = duplicateIndex; }
    }

    /**
     * Analyze complaint text to classify department and detect duplicates
     * Returns fallback values if AI service is unavailable
     * 
     * @param text The complaint description
     * @param existingTexts List of existing complaint descriptions for duplicate detection
     * @return AiAnalysisResult with predicted department and duplicate info
     */
    public AiAnalysisResult analyzeComplaint(String text, java.util.List<String> existingTexts) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            AnalyzeRequest request = new AnalyzeRequest(text, existingTexts);
            HttpEntity<AnalyzeRequest> entity = new HttpEntity<>(request, headers);
            
            String url = aiServiceUrl + "/analyze";
            ResponseEntity<AnalyzeResponse> response = restTemplate.postForEntity(
                url, 
                entity, 
                AnalyzeResponse.class
            );
            
            if (response.getBody() != null) {
                AnalyzeResponse body = response.getBody();
                return new AiAnalysisResult(
                    body.getPredicted_department(),
                    body.getIs_duplicate() != null ? body.getIs_duplicate() : false,
                    body.getDuplicate_index()
                );
            }
        } catch (Exception e) {
            // AI service unavailable - return safe fallback
            System.err.println("AI Service unavailable: " + e.getMessage());
        }
        
        // Return safe fallback
        return new AiAnalysisResult();
    }
}

