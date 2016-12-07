package fi.aalto.openoranges.project2.openocranges;




public class OcrResult {
    private String extractedText;
    private String processingStarted;
    private String processingFinished;
    private String thumbnailUrl;
    private String imageUrl;
    private String createdAt;
    private double processingTime;

    public OcrResult(String extractedText, String processingStarted, String procesingFinished, double processingTime, String thumbnail_url, String image_url,String createdAt) {
        super();
        this.extractedText = extractedText;
        this.processingStarted = processingStarted;
        this.processingFinished= procesingFinished;
        this.processingTime=processingTime;
        this.thumbnailUrl = thumbnail_url;
        this.imageUrl = image_url;
        this.createdAt=createdAt;
    }

    public String getExtractedText() {
        return extractedText;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public String getProcessingStarted() {
        return processingStarted;
    }

    public String getProcessingFinished() {
        return processingFinished;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getProcessingTime() {
        return processingTime;
    }
}
