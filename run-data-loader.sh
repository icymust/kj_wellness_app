#!/bin/bash

# Recipe Data Loader Execution Script
# This script runs the RecipeDataLoader to ingest JSON data into PostgreSQL

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"

echo "================================="
echo "Recipe Data Loader"
echo "================================="
echo ""

# Check if PostgreSQL is running
echo "Checking PostgreSQL connection..."
if ! docker ps | grep -q postgres; then
    echo "⚠️  PostgreSQL container not running. Starting docker-compose..."
    docker-compose up -d db
    echo "Waiting for PostgreSQL to be ready..."
    sleep 10
fi

# Verify JSON files exist
echo ""
echo "Verifying JSON files..."
if [ ! -f "$BACKEND_DIR/src/main/resources/data/ingredients.json" ]; then
    echo "❌ ingredients.json not found!"
    exit 1
fi

if [ ! -f "$BACKEND_DIR/src/main/resources/data/recipes.json" ]; then
    echo "❌ recipes.json not found!"
    exit 1
fi

echo "✓ ingredients.json found ($(wc -l < "$BACKEND_DIR/src/main/resources/data/ingredients.json") lines)"
echo "✓ recipes.json found ($(wc -l < "$BACKEND_DIR/src/main/resources/data/recipes.json") lines)"

# Run the data loader
echo ""
echo "Starting data loader..."
echo "================================="
echo ""

cd "$BACKEND_DIR"
mvn spring-boot:run -Dspring-boot.run.profiles=data-loader

echo ""
echo "================================="
echo "Data loader finished!"
echo "================================="
