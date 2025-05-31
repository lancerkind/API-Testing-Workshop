As with all test automation, there are several levels of testing that can be done in API testing.
Here is a lab which illustrates the "Why" of certain levels of testing, along with examples to 
illustrate "How."

This particular workshop will use the following tools:
* Java for the implementation language 
* SpringBoot for implenting the API with microservices
* Docker containers as infrastructure for the microservices
* RestAssured for testing the API
* WireMock for reducing upstream dependencies
* OpenAPI for specifying the API

# Why
API tests exist to ensure the quality of a product. With distributed systems built by 
different teams, there are two actors involved: consumers of an API and producers of an API.
Quality in this environment becomes a team sport where both consumers and producers have a role.

# How
Let's start by sharing a story. Team Razzmatazz produces a product that is used by customers.
They have been asked by the business to add in invoice presentation to their customer facing application. 
The invoice data is produced by a different product and team called Ledgerlicious.

Razzmatazz and Ledgerlicious meet and discuss business need and realize that if Ledgerlicious can produce an API, Excitment
can use that API to show the invoice information in their front end application. To do this without 
being wasteful of time and effort, they realize they need to work together in some fashion.

### They discover this way of working:
* Members from Ledgerlicious and Razzmatazz collaborate to define what they need with an OpenAPI schema.
* Razzmatazz creates simple **consumer** contract tests
* Ledgerlicious will simple **producer** contract tests 
* Ledgerlicious will create functional tests for their service
* Ledgerlicious will use WireMock to allow most tests to run independently from Abacus