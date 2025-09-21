#!/bin/bash

echo "🛑 Stopping RAG System..."

# Stop Spring Boot application
echo "Stopping API server..."
pkill -f "bootRun" || true

# Stop Docker services
echo "Stopping Docker services..."
docker-compose down

echo "✅ RAG System stopped"