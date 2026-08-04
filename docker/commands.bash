# Get all slots for user1 on Aug 4, 2026
curl "http://localhost:8080/v1/slots?userId=user1&start=2026-08-04T00:00:00&end=2026-08-04T23:59:59"

# Create a slot
curl -X POST http://localhost:8080/v1/slots \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user1",
    "startTime": "2026-08-05T10:00:00",
    "endTime": "2026-08-05T11:00:00"
  }'

# Update a slot
curl -X PUT http://localhost:8080/v1/slots/UUID \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2026-08-04T09:30:00",
    "endTime": "2026-08-04T10:30:00"
  }'

# Delete a slot
curl -X DELETE http://localhost:8080/v1/slots/UUID

# Get meetings for user1 on Aug 4, 2026
curl "http://localhost:8080/v1/meetings?userId=user1&start=2026-08-04T00:00:00&end=2026-08-04T23:59:59"

# Book a meeting (using one of the available slots)
curl -X POST http://localhost:8080/v1/meetings \
  -H "Content-Type: application/json" \
  -d '{
    "slotId": "slot-001",
    "title": "Quick Sync",
    "description": "Morning catchup with team"
  }'

