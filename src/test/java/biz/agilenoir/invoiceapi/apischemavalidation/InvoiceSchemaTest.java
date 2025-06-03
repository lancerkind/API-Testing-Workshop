package biz.agilenoir.invoiceapi.apischemavalidation;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static io.restassured.RestAssured.*;

import biz.agilenoir.invoiceapi.Main;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;

public class InvoiceSchemaTest {
    private static final int portNumber = 8090;
    private static final String BASE_URL = "http://localhost:" + portNumber;
    private static Thread serverThread;

    @BeforeAll
    public static void setup() {
        // Start the API server
        System.out.println("Starting API server for OpenAPI validation tests...");
        serverThread = new Thread(() -> {

            try {
                Main.main(new String[]{String.valueOf(portNumber)});
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
    @DisplayName("Validate Create Invoice Endpoint Response Against Schema")
    void validateCreateInvoiceAgainstSchema() {
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
