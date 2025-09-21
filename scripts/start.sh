#!/bin/bash

echo "🚀 Starting RAG System..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Start Docker services
echo "📦 Starting Docker services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check Elasticsearch
echo "🔍 Checking Elasticsearch..."
until curl -s http://localhost:9200 > /dev/null; do
    echo "Waiting for Elasticsearch..."
    sleep 5
done
echo "✅ Elasticsearch is ready"

# Check Ollama
echo "🤖 Checking Ollama..."
until curl -s http://localhost:11434/api/tags > /dev/null; do
    echo "Waiting for Ollama..."
    sleep 5
done
echo "✅ Ollama is ready"

# Pull models if not exists
echo "📥 Downloading models..."
docker exec rag-ollama ollama pull nomic-embed-text || true
docker exec rag-ollama ollama pull llama3.2 || true

# Build and start application
echo "🔨 Building application..."
./gradlew clean build

echo "🎯 Starting API server..."
./gradlew :modules:rag-api:bootRun &

echo "✨ RAG System is ready!"
echo "📚 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "📊 Kibana: http://localhost:5601"