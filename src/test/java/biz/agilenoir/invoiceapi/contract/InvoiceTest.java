package biz.agilenoir.invoiceapi.contract;

import biz.agilenoir.invoiceapi.Main;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * API Test Client using RestAssured
 * This class provides functionality to test the API endpoints using RestAssured.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InvoiceTest {
    private static final int portNumber = 8091;
    private static final String BASE_URL = "http://localhost:" + portNumber;
    private static Thread serverThread;

    @BeforeAll
    public static void setup() {
        // Start the API server
        System.out.println("Starting API server...");
        serverThread = new Thread(() -> {
            String[] args = new String[1];
            args[0] = String.valueOf(portNumber);
            try {
                Main.main(args);
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
        RestAssured.baseURI = BASE_URL;
        System.out.println("Testing endpoint at " + BASE_URL);
    }

    @AfterAll
    public static void tearDown() {
        // In a real application, we would shut down the server here
        System.out.println("Tests completed");
    }

    @Test
    @Order(1)
    @DisplayName("Test health endpoint")
    public void testHealthEndpoint() {
        System.out.println("\nTesting health endpoint:");
        
        given()
            .when()
            .get("/api/health")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", equalTo("UP"));
    }

    @Test
    @Order(2)
    @DisplayName("Test get all invoices")
    public void testGetAllInvoices() {
        System.out.println("\nTesting get all invoices:");

        Response response =
                given()
                        .when()
                        .get("/api/invoices");

        // Print the response body
        System.out.println("Response Body:");
        System.out.println(response.getBody().asString());


        given()
            .when()
            .get("/api/invoices")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", greaterThanOrEqualTo(2))
            .body("findAll { it.id == 'INV-001' }.size()", equalTo(1))
            .body("findAll { it.id == 'INV-002' }.size()", equalTo(1));
    }

    @Test
    @Order(3)
    @DisplayName("Test get invoice by ID")
    public void testGetInvoiceById() {
        System.out.println("\nTesting get invoice by ID:");
        
        given()
            .when()
            .get("/api/invoices?id=INV-001")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo("INV-001"))
            .body("customer", equalTo("Acme Corp"))
            .body("status", equalTo("PAID"));
    }

    @Test
    @Order(4)
    @DisplayName("Test get non-existent invoice")
    public void testGetNonExistentInvoice() {
        System.out.println("\nTesting get non-existent invoice:");
        
        given()
            .when()
            .get("/api/invoices?id=INV-999")
            .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .body("error", containsString("not found"));
    }

    @Test
    @Order(5)
    @DisplayName("Test create new invoice")
    public void testCreateInvoice() {
        System.out.println("\nTesting create new invoice:");
        
        given()
            .when()
            .post("/api/invoices")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("id", notNullValue())
            .body("customer", equalTo("New Customer"))
            .body("status", equalTo("NEW"));
    }
}