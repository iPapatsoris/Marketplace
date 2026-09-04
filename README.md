# Marketplace

## Overview

A marketplace backend where users can browse products and create reservations / orders. 
The project focuses on maintaining consistency across concurrent operations and asynchronous processing, 
rather than on building a feature-rich application.

## Backend concepts

- Transaction boundaries
- Outbox pattern
- Retries
- Optimistic locking
- Concurrency handling
- Layered architecture
- Domain modeling
- REST API design

## Technologies
- Java
- Spring Boot
- JPA / Hibernate
- Mapstruct
- PostgreSQL
- JUnit / Mockito / MockMVC

## Testing

The project includes a testing suite consisting of:

- Integration tests covering key user scenarios, transactional workflows, outbox processing, and areas with high concurrency risk.
- Integration tests targeting specific native queries of interest.
- Unit tests for services and controllers with mocking.
