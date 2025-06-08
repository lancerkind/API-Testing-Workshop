package biz.agilenoir.invoiceapi;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import biz.agilenoir.abacusapi.client.AbacusClient;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple Invoice Microservice Application
 * This application demonstrates a basic REST API using Java's built-in HttpServer.
 * It implements a simple invoice management system as described in the README.
 */
public class InvoiceMicroservice {

    public static class InvoiceRequest {
        private String id;
        private String customer;
        private Double amount;
        private LocalDate date;
        private String status;

        public String getId() { return id; }
        public void setId(String id) {  this.id = id;  }
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
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }


    public static class ConfigurationArgumentIndices {
        public static final int ARRAY_SIZE = 2;  // If adding more indices, increase this number
        public static final int INVOICE_SERVICE_PORT = 0;
        public static final int ABACUS_SERVICE_PORT = 1;
    }

    // In-memory storage for invoices
    private static final List<Map<String, Object>> invoices = new ArrayList<>();
    private int portNumber;
    private static int abacusPortNumber;

    /**
     * Service entry point. AKA a "main."
     * @param configurationSettings first array element is port number for InvoiceMicroservice. The second element is for Abacus.
     * @throws IOException Raised if there is a network problem.
     */
    public static void main(String[] configurationSettings) throws IOException {
        InvoiceMicroservice invoiceMicroservice = new InvoiceMicroservice();
        processConfigurationSettings(configurationSettings, invoiceMicroservice);

        // Initialize with some sample data
        initializeSampleData();

        // Create HTTP server on port
        HttpServer server = HttpServer.create(new InetSocketAddress(invoiceMicroservice.portNumber), 0);

        // Define API endpoints
        server.createContext("/api/invoices", new InvoiceHandler());
        server.createContext("/api/health", new HealthCheckHandler());

        // Set executor and start server
        server.setExecutor(null);
        server.start();

        System.out.println("API Server started on port " + invoiceMicroservice.portNumber );
        System.out.println("Available endpoints:");
        System.out.println("  GET  /api/health - Health check endpoint");
        System.out.println("  GET  /api/invoices - List all invoices");
        System.out.println("  GET  /api/invoices?id={id} - Get invoice by ID");
        System.out.println("  POST /api/invoices - Create a new invoice (send JSON in request body)");
    }

    private static void processConfigurationSettings(String[] args, InvoiceMicroservice invoiceMicroservice) {
        if (args.length < 1) invoiceMicroservice.portNumber = 8090;
        if( args.length >= 1 ) invoiceMicroservice.portNumber = Integer.parseInt(args[ConfigurationArgumentIndices.INVOICE_SERVICE_PORT]);
        if( args.length >= 2 ) invoiceMicroservice.abacusPortNumber = Integer.parseInt(args[ConfigurationArgumentIndices.ABACUS_SERVICE_PORT]);
        if (args.length > ConfigurationArgumentIndices.ARRAY_SIZE) {
            System.out.println("Invalid number of arguments. Expected " + ConfigurationArgumentIndices.ARRAY_SIZE + " but received " + args.length);
            System.exit(1);
        }
    }

    /**
     * Initialize sample invoice data
     */
    private static void initializeSampleData() {
        invoices.clear();

        // Sample invoice 1
        Map<String, Object> invoice1 = new HashMap<>();
        invoice1.put("id", "INV-001");
        invoice1.put("customer", "Acme Corp");
        invoice1.put("amount", 1250.00);
        invoice1.put("date", "2023-01-15");
        invoice1.put("status", "PAID");

        // Sample invoice 2
        Map<String, Object> invoice2 = new HashMap<>();
        invoice2.put("id", "INV-002");
        invoice2.put("customer", "Globex Inc");
        invoice2.put("amount", 850.50);
        invoice2.put("date", "2023-02-20");
        invoice2.put("status", "PENDING");

        // Add to our in-memory storage
        invoices.add(invoice1);
        invoices.add(invoice2);
    }

    /**
     * Handler for invoice-related endpoints
     */
    static class InvoiceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            switch (method) {
                case "GET":
                    handleGetInvoices(exchange);
                    break;
                case "POST":
                    handleCreateInvoice(exchange);
                    break;
                default:
                    sendResponse(exchange, 405, "Method Not Allowed");
            }
        }

        /**
         * Handle GET requests for invoices
         */
        private void handleGetInvoices(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String response;

            if (query != null && query.startsWith("id=")) {
                // Get invoice by ID
                String id = query.substring(3);
                Map<String, Object> invoice = findInvoiceById(id);

                if (invoice != null) {
                    response = convertToJson(invoice);
                    sendResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 404, "{\"error\": \"Invoice not found\"}");
                }
            } else {
                // Get all invoices
                response = convertToJson(invoices);
                sendResponse(exchange, 200, response);
            }
        }

        /**
         * Handle POST requests to create a new invoice
         */
        private void handleCreateInvoice(HttpExchange exchange) throws IOException {
            if (!invoiceProcessedByAbacus(exchange)) {
                    sendResponse(exchange, 503, "{\"error\": \"Internal Server Error\"}");
            }

            // In a real application, we would parse the JSON from the request body
            // For simplicity, we'll just create a dummy invoice
            Map<String, Object> newInvoice = new HashMap<>();
            newInvoice.put("id", "INV-" + (invoices.size() + 1));
            newInvoice.put("customer", "New Customer");
            newInvoice.put("amount", 500.00);
            newInvoice.put("date", "2023-03-01");
            newInvoice.put("status", "NEW");

            invoices.add(newInvoice);

            sendResponse(exchange, 201, convertToJson(newInvoice));
        }

        /**
         *
         * @param exchange request from client of microservice
         * @return True if Abacus successfully processed request. False if otherwires.
         */
        private boolean invoiceProcessedByAbacus(HttpExchange exchange) {
            // prepare the request body
            String body;

            try (InputStream inputStream = exchange.getRequestBody()) {
                body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.err.println("Request to InvoiceService interrupted");
                e.printStackTrace();
                return false;
            }

            if (body.length() == 0) {
                System.err.println("Empty body received in invoice request. Aborting to call Abacus with a bad request.");
                return false;
            }
            System.out.println("Received request body: " + body);

            // Prepare the abacus request
            AbacusClient abacusClient = new AbacusClient("http://localhost:" + abacusPortNumber);
            System.out.println("AbacusClient connecting to service at " + abacusClient.getBasePath());
            // Deserialize JSON into InvoiceRequest
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // necessary to work with DateTime with Jackson.
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            try {
                InvoiceRequest invoiceRequest = objectMapper.readValue(body, InvoiceRequest.class);
                // map request to the upstream dependency, AbacusClient's invoice request
                System.out.println("InvoiceRequest deserialized: " + invoiceRequest);
                AbacusClient.InvoiceRequest abacusInvoiceRequest = new AbacusClient.InvoiceRequest();
                // copy data into abacusinvoiceRequest which only requires customer, amount and date to process the invoice.

                abacusInvoiceRequest.setCustomer(invoiceRequest.getCustomer());
                abacusInvoiceRequest.setAmount(invoiceRequest.getAmount());
                abacusInvoiceRequest.setDate(invoiceRequest.getDate());

                // Call the upstream API we depend on
                AbacusClient.ProcessResponse response = abacusClient.processInvoice(abacusInvoiceRequest);

                if (response.getStatus().equals("ACCEPTED")) {
                    System.out.println("Abacus processed invoice successfully");
                    return true;
                } else {
                    System.out.println("Abacus failed to process invoice");
                    return false;
                }

            } catch (Throwable e) {
                System.out.println("AbacusClient interrupted while processing invoice");
                e.printStackTrace();
            }
            return false;
        }

        /**
         * Find an invoice by ID
         */
        private Map<String, Object> findInvoiceById(String id) {
            return invoices.stream()
                    .filter(inv -> id.equals(inv.get("id")))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Handler for health check endpoint
     */
    static class HealthCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "{\"status\": \"UP\"}");
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    /**
     * Helper method to send HTTP response
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length());

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    /**
     * Simple JSON conversion (in a real app, use a proper JSON library)
     */
    private static String convertToJson(Object obj) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(convertToJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        } else if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":");

                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }
            }
            sb.append("}");
            return sb.toString();
        }
        return obj.toString();
    }
}