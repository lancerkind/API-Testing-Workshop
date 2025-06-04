package biz.agilenoir.abacusapi.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the AbacusAPI client using WireMock
 */
public class AbacusClientWireMockDemoTest {
    private WireMockServer wireMockServer;
    private AbacusClient abacusClient;

    @BeforeEach
    void setup() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Configure the client to use the WireMock server URL
        abacusClient = new AbacusClient("http://localhost:" + wireMockServer.port());

        System.out.println("WireMock server started on port: " + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        System.out.println("WireMock server stopped");
    }

    @Test
    @DisplayName("Test health endpoint")
    void testHealthEndpoint() {
        // Setup mock response
        stubFor(get(urlEqualTo("/api/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"UP\"}")));

        try {
            // Call the API
            AbacusClient.HealthResponse healthResponse = abacusClient.getHealth();

            // Verify the request was made
            verify(getRequestedFor(urlEqualTo("/api/health")));

            // Verify the response
            assertNotNull(healthResponse);
            assertEquals("UP", healthResponse.getStatus());
            System.out.println("Health response: " + healthResponse);
        } catch (IOException | InterruptedException e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test process invoice endpoint")
    void testProcessInvoice() {
        // Setup mock response
        stubFor(post(urlEqualTo("/api/process"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactionId\": \"TRX-12345\", \"status\": \"ACCEPTED\", \"message\": \"Invoice processed successfully\"}")));

        try {
            // Create invoice request
            AbacusClient.InvoiceRequest invoiceRequest = new AbacusClient.InvoiceRequest();
            invoiceRequest.setCustomer("Test Customer");
            invoiceRequest.setAmount(100.0);
            invoiceRequest.setDate(LocalDate.now());

            // Call the API
            AbacusClient.ProcessResponse response = abacusClient.processInvoice(invoiceRequest);

            // Verify the request was made
            verify(postRequestedFor(urlEqualTo("/api/process"))
                    .withHeader("Content-Type", containing("application/json")));

            // Verify the response
            assertNotNull(response);
            assertEquals("TRX-12345", response.getTransactionId());
            assertEquals("ACCEPTED", response.getStatus());
            assertEquals("Invoice processed successfully", response.getMessage());

            System.out.println("Process response: " + response);
        } catch (IOException | InterruptedException e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
}
