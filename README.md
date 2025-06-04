As with all test automation, there are several levels of testing that can be done in API testing.
Here is a lab which illustrates the "Why" of certain levels of testing, along with examples to 
illustrate "How."  

This particular workshop will use the following tools:
* Java for the implementation language
* RestAssured for testing the API
* OpenAPI for specifying the API


# Getting Started
This repository contains a simple API application that demonstrates the concepts described below.

## Project Structure
- `src/main/resources/openapi.yaml` - OpenAPI specification for the API
- `src/main/java/biz/agilenoir/invoiceapi/Main.java` - The main application that implements a simple invoice API
- `src/test/java/biz/agilenoir/invoiceapi/contract/InvoiceTest.java` - A test that demonstrates how to test the 
contract from the API producer's perspective.
- `src/test/java/biz/agilenoir/invoiceapi/apischemavalidation/InvoiceSchemaTest.java` - A test that checks that the 
schema is correctly represented by the controller.

## Running the Application
To run the application:
```
javac src/main/java/biz/agilenoir/invoiceapi/Main.java
java -cp src/main/java biz.agilenoir.invoiceapi.Main
```

The server will start on port 8090 (default, or pass a port number as an argument) with the following endpoints:
- `GET /api/health` - Health check endpoint
- `GET /api/invoices` - List all invoices
- `GET /api/invoices?id={id}` - Get invoice by ID
- `POST /api/invoices` - Create a new invoice

## Running the Tests
To run the tests:
```
maven test
```

The test client will start the server, run tests against all endpoints, and verify the responses.

# Why
API tests exist to ensure the quality of a product. With distributed systems built by 
different teams, there are two actors involved: consumers of an API and producers of an API.
Quality in this environment becomes a team sport where both consumers and producers have a role.

# How
Let's start by sharing a story. Team Razzmatazz produces a product that is used by customers.
They have been asked by the business to add in invoice presentation to their customer facing application. 
The invoice data is produced by a different product and team called Ledgerlicious.

Razzmatazz and Ledgerlicious meet and discuss business need and realize that if Ledgerlicious can produce an API, Excitement
can use that API to show the invoice information in their front end application. To do this without 
being wasteful of time and effort, they realize they need to work together in some fashion.

### They discover this way of working:
* Members from Ledgerlicious and Razzmatazz collaborate to define what they need with an OpenAPI spec.
* Razzmatazz creates simple **consumer** contract tests
* Ledgerlicious will simple **producer** contract tests 
* Ledgerlicious will create functional tests for their service
* Ledgerlicious will use WireMock to allow most tests to run independently of Abacus

### More details about this way of working
- Razzmatazz creates consumer tests with RestAssured that will confirm that the endpoint that Ledgerlicious creates 
follows the API contract (schema in the OpenAI Spec) that they depend on. They do this out of repsonsibility to the 
users of their product. For now, Razzmatazz implements and checks in the tests (test driven development at the 
API layer). Because the tests fail in CI (the API is yet to be developed), they are marked with pending/ignore for now.
For the meantime, Razzmatazz isn’t blocked from continuing to work, so they build out the view layer using client 
objects—built from OpenAPI spec—that will be loaded with data once the api is produced. Sometimes they discover a
problem with the API spec and go back and talk to Ledgerlicious about changes in the spec.
- Ledgerlicious creates producer tests with RestAssured that will confirm the API functionality works at the contract level.
They will test the entire contract they offer for all consumers, not just Razzmatazz. 
The tests fail for the meantime as the API has yet to be built so these tests will be marked as pending/ignored so 
their CI system doesn’t declare the build as broken.
- Day by day, Ledgerlicious will implement the API by performing TDD: create unit tests and acceptance tests. These
tests will be run into their CI environment.
- Ledgerlicious has a dependency on data provided by team Abacus. Abacus is a slow legacy system which would greatly impact 
the speed of Ledgerlicious’s producer tests, so they mock this upstream dependency with WireMock.

Over time, iteration by iteration (sprint by sprint), API functionality is built out by Ledgerlicious, so as a result, 
the tests produced by Ledgerlicious and Razzmatazz are switched from pending/ignored to actually testing 
against a real endpoint released by Ledgerlicious. Occasionally a producer test passes but then still fails a 
consumer test, in which case, Razzmatazz and Ledgerlicious have conversations on why that has happened and 
make adjustments.
