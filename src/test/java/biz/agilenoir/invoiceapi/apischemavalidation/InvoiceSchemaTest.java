package biz.agilenoir.invoiceapi.apischemavalidation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static io.restassured.RestAssured.*;

import biz.agilenoir.invoiceapi.InvoiceMicroservice;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;

public class InvoiceSchemaTest {
    private static final int portNumber = 8090;
    private static final String BASE_URL = "http://localhost:" + portNumber;
    private static WireMockServer wireMockServer;

    private static void setupVirtualAbacusService() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        System.out.println("WireMock server started on port: " + wireMockServer.port());
    }

    private static void teardownVirtualAbacusService() {
        wireMockServer.stop();
        System.out.println("WireMock server stopped");
    }

    @BeforeAll
    public static void setup() {
        setupVirtualAbacusService();
        // Start the API server
        System.out.println("Starting API server for OpenAPI validation tests...");
        Thread serverThread = new Thread(() -> {

            try {
                String [] configuration = new String [2];
                configuration[InvoiceMicroservice.ConfigurationArgumentIndices.INVOICE_SERVICE_PORT] = Integer.toString(portNumber);
                configuration[InvoiceMicroservice.ConfigurationArgumentIndices.ABACUS_SERVICE_PORT] = Integer.toString(wireMockServer.port());
                InvoiceMicroservice.main(configuration);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        // Wait for server to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Configure RestAssured
        baseURI = BASE_URL;
        System.out.println("Testing endpoint at " + BASE_URL);
    }

    @AfterAll
    public static void tearDown() {
        teardownVirtualAbacusService();
        System.out.println("OpenAPI validation tests completed");
    }

    @Test
    @DisplayName("Validate Health Endpoint Response Against Schema")
    void validateHealthEndpointAgainstSchema() {
        given()
            .baseUri(BASE_URL)
        .when()
            .get("/api/health")
        .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/health-schema.json"));
    }

    @Test
    @DisplayName("Validate Get All Invoices Endpoint Response Against Schema")
    void validateGetAllInvoicesAgainstSchema() {
        given()
            .baseUri(BASE_URL)
        .when()
            .get("/api/invoices")
        .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/invoice-list-schema.json"));
    }

    @Test
    @DisplayName("Validate Get Invoice By ID Endpoint Response Against Schema")
    void validateGetInvoiceByIdAgainstSchema() {
        given()
            .baseUri(BASE_URL)
        .when()
            .get("/api/invoices?id=INV-001")
        .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/invoice-schema.json"));
    }

    @Test
    @DisplayName("Validate Create Invoice Endpoint Response Against Schema which depends on Abacus service.")
    void validateCreateInvoiceAgainstSchema() {
        stubFor(post(urlEqualTo("/api/process"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactionId\": \"TRX-12345\", \"status\": \"ACCEPTED\", \"message\": \"Invoice processed successfully\"}")));

        given()
            .baseUri(BASE_URL)
            .contentType("application/json")
            .body("{\n" +
                  "  \"customer\": \"New Customer\",\n" +
                  "  \"amount\": 500.00,\n" +
                  "  \"date\": \"2023-03-01\"\n" +
                  "}")
        .when()
            .post("/api/invoices")
        .then()
            .statusCode(201)
            .body(matchesJsonSchemaInClasspath("schemas/invoice-schema.json"));
    }
}
