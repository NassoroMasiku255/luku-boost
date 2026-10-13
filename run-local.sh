#!/bin/bash

# Default values if not set
PORT=${SERVER_PORT:-9001}
WHATSAPP_TOKEN=${WHATSAPP_ACCESS_TOKEN:-fake-token}

echo "Starting Luku Boost Backend on port $PORT..."

./mvnw spring-boot:run -Dspring-boot.run.arguments=" \
  --server.port=$PORT \
  --spring.datasource.url=jdbc:h2:mem:lukuboost;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
  --spring.jpa.hibernate.ddl-auto=create \
  --whatsapp.api-url=https://graph.facebook.com/v25.0/1317524134784685/messages \
  --whatsapp.access-token=$WHATSAPP_TOKEN \
  --whatsapp.otp-template-name=lukuboost_auth \
  --whatsapp.otp-template-language=en \
  --whatsapp.default-country-code=255"
