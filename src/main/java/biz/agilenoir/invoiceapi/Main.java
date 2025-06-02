package biz.agilenoir.invoiceapi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple API Workshop Application
 * This application demonstrates a basic REST API using Java's built-in HttpServer.
 * It implements a simple invoice management system as described in the README.
 */
public class Main {
    // In-memory storage for invoices
    private static final List<Map<String, Object>> invoices = new ArrayList<>();
    private int portNumber;

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        if( args.length != 1 ) main.portNumber = 8090;
        else main.portNumber = Integer.parseInt(args[0]);

        // Initialize with some sample data
        initializeSampleData();

        // Create HTTP server on port
        HttpServer server = HttpServer.create(new InetSocketAddress(main.portNumber), 0);

        // Define API endpoints
        server.createContext("/api/invoices", new InvoiceHandler());
        server.createContext("/api/health", new HealthCheckHandler());

        // Set executor and start server
        server.setExecutor(null);
        server.start();

        System.out.println("API Server started on port " + main.portNumber );
        System.out.println("Available endpoints:");
        System.out.println("  GET  /api/health - Health check endpoint");
        System.out.println("  GET  /api/invoices - List all invoices");
        System.out.println("  GET  /api/invoices?id={id} - Get invoice by ID");
        System.out.println("  POST /api/invoices - Create a new invoice (send JSON in request body)");
    }

    /**
     * Initialize sample invoice data
     */
    private static void initializeSampleData() {
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