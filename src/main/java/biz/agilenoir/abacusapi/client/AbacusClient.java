package biz.agilenoir.abacusapi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

/**
 * A simple client for the Abacus API
 */
public class AbacusClient {
    private final String basePath;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public String getBasePath() { return basePath; }
    /**
     * Constructor
     * @param basePath The base path of the API
     */
    public AbacusClient(String basePath) {
        this.basePath = basePath;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Get the health status of the API
     * @return The health response
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public HealthResponse getHealth() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/api/health"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected status code: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), HealthResponse.class);
    }

    /**
     * Process an invoice
     * @param invoiceRequest The invoice request
     * @return The process response
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public ProcessResponse processInvoice(InvoiceRequest invoiceRequest) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/api/process"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invoiceRequest)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected status code: " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), ProcessResponse.class);
    }

    /**
     * Health response model
     */
    public static class HealthResponse {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "HealthResponse{" +
                    "status='" + status + '\'' +
                    '}';
        }
    }

    /**
     * Invoice request model
     */
    public static class InvoiceRequest {
        private String customer;
        private Double amount;
        private LocalDate date;

        public String getCustomer() {
            return customer;
        }

        public void setCustomer(String customer) {
            this.customer = customer;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }

    /**
     * Process response model
     */
    public static class ProcessResponse {
        private String transactionId;
        private String status;
        private String message;

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "ProcessResponse{" +
                    "transactionId='" + transactionId + '\'' +
                    ", status='" + status + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}