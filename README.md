# Doodle Scheduler - Meeting Scheduling Platform

A high-performance meeting scheduling platform built with Spring Boot and Java 21, following Domain-Driven Design principles.

## Key Features

1. **Time Slot Management**: Create, update, delete availability slots
2. **Meeting Scheduling**: Convert slots to meetings with participants
3. **Calendar Availability**: Query free/busy slots in time ranges
4. **Concurrent Booking**: Handle concurrent booking requests safely

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21 (if running locally)

### Run with Docker
```bash
docker-compose up --build