package biz.agilenoir.invoiceapi.invoiceapi.contract;

import biz.agilenoir.invoiceapi.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Simple API Test Client
 * This class provides basic functionality to test the API endpoints.
 * In a real-world scenario, you would use a testing framework like RestAssured.
 */
public class ApiTestClient {
    private static final String BASE_URL = "http://localhost:8090";

    public static void main(String[] args) {
        try {
            // Start the API server
            System.out.println("Starting API server...");
            Thread serverThread = new Thread(() -> {
                try {
                    Main.main(new String[]{});
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            });
            serverThread.start();

            // Wait for server to start
            Thread.sleep(1000);
            System.out.println("Testing endpoint at " + BASE_URL);

            // Test health endpoint
            System.out.println("\nTesting health endpoint:");
            String healthResponse = sendGetRequest("/api/health");
            System.out.println("Response: " + healthResponse);
            assert healthResponse.contains("UP") : "Health check failed";

            // Test get all invoices
            System.out.println("\nTesting get all invoices:");
            String invoicesResponse = sendGetRequest("/api/invoices");
            System.out.println("Response: " + invoicesResponse);
            assert invoicesResponse.contains("INV-001") : "Get all invoices failed";

            // Test get invoice by ID
            System.out.println("\nTesting get invoice by ID:");
            String invoiceResponse = sendGetRequest("/api/invoices?id=INV-001");
            System.out.println("Response: " + invoiceResponse);
            assert invoiceResponse.contains("Acme Corp") : "Get invoice by ID failed";

            // Test get non-existent invoice
            System.out.println("\nTesting get non-existent invoice:");
            String notFoundResponse = sendGetRequest("/api/invoices?id=INV-999");
            System.out.println("Response: " + notFoundResponse);
            assert notFoundResponse.contains("not found") : "Get non-existent invoice failed";

            // In a real application, we would shut down the server here
            // For simplicity, we'll just exit
            System.out.println("\nAll tests passed");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Send a GET request to the specified endpoint
     */
    private static String sendGetRequest(String endpoint) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();

        BufferedReader in;
        if (responseCode >= 200 && responseCode < 300) {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        }

        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
}