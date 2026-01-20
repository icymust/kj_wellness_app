#!/bin/bash

# Recipe Data Verification Script
# Checks if recipes and ingredients were loaded successfully

set -e

echo "================================="
echo "Recipe Data Verification"
echo "================================="
echo ""

# Check PostgreSQL connection
if ! docker ps | grep -q postgres; then
    echo "❌ PostgreSQL container not running"
    echo "Start with: docker-compose up -d db"
    exit 1
fi

echo "✓ PostgreSQL container is running"
echo ""

# Check ingredients count
echo "Checking ingredients..."
INGREDIENTS_COUNT=$(docker exec postgres psql -U postgres -d ndl -t -c "SELECT COUNT(*) FROM ingredients;")
INGREDIENTS_COUNT=$(echo $INGREDIENTS_COUNT | tr -d ' ')

if [ "$INGREDIENTS_COUNT" -eq "0" ]; then
    echo "⚠️  No ingredients found in database"
    echo "Run: ./run-data-loader.sh"
else
    echo "✓ Found $INGREDIENTS_COUNT ingredients"
fi

echo ""

# Check recipes count
echo "Checking recipes..."
RECIPES_COUNT=$(docker exec postgres psql -U postgres -d ndl -t -c "SELECT COUNT(*) FROM recipes;")
RECIPES_COUNT=$(echo $RECIPES_COUNT | tr -d ' ')

if [ "$RECIPES_COUNT" -eq "0" ]; then
    echo "⚠️  No recipes found in database"
    echo "Run: ./run-data-loader.sh"
else
    echo "✓ Found $RECIPES_COUNT recipes"
fi

echo ""

# Check recipe_ingredients junctions
if [ "$RECIPES_COUNT" -gt "0" ]; then
    echo "Checking recipe-ingredient relationships..."
    JUNCTIONS_COUNT=$(docker exec postgres psql -U postgres -d ndl -t -c "SELECT COUNT(*) FROM recipe_ingredients;")
    JUNCTIONS_COUNT=$(echo $JUNCTIONS_COUNT | tr -d ' ')
    echo "✓ Found $JUNCTIONS_COUNT recipe-ingredient relationships"
    echo ""
fi

# Sample data preview
if [ "$RECIPES_COUNT" -gt "0" ]; then
    echo "Sample recipes (first 5):"
    docker exec postgres psql -U postgres -d ndl -c "SELECT id, title, cuisine, meal FROM recipes LIMIT 5;"
    echo ""
fi

if [ "$INGREDIENTS_COUNT" -gt "0" ]; then
    echo "Sample ingredients (first 5):"
    docker exec postgres psql -U postgres -d ndl -c "SELECT id, label, calories, protein FROM ingredients LIMIT 5;"
    echo ""
fi

echo "================================="
echo "Verification complete!"
echo "================================="
