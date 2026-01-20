#!/usr/bin/env python3
"""
Transform ingredients.json and recipes.json to match required data structures.
- Ingredient: Add id, move nutrition to nested object
- Recipe: Replace ingredientNames with ingredients array, rename fields, add preparation objects
"""

import json
import hashlib
from pathlib import Path
from typing import Dict, List, Any

def generate_stable_id(label: str, prefix: str = "ing") -> str:
    """Generate stable ID from ingredient label using hash."""
    hash_obj = hashlib.md5(label.lower().encode())
    hash_hex = hash_obj.hexdigest()[:8]
    return f"{prefix}{hash_hex}"

def transform_ingredients(input_path: str, output_path: str):
    """Transform ingredients.json to new structure."""
    print("📖 Loading ingredients...")
    with open(input_path, 'r', encoding='utf-8') as f:
        ingredients = json.load(f)
    
    transformed = []
    ingredient_map = {}  # For recipe transformation
    
    print(f"🔄 Transforming {len(ingredients)} ingredients...")
    for ing in ingredients:
        if not ing or not ing.get('label'):
            continue
            
        label = ing['label']
        stable_id = generate_stable_id(label, "ing")
        
        transformed_ing = {
            "id": stable_id,
            "label": label,
            "unit": ing.get('unit', 'gram'),
            "quantity": int(ing.get('quantityPer100', 100)),
            "nutrition": {
                "calories": ing.get('calories', 0),
                "carbs": ing.get('carbs', 0),
                "protein": ing.get('protein', 0),
                "fats": ing.get('fats', 0)
            }
        }
        
        transformed.append(transformed_ing)
        ingredient_map[label] = {
            "id": stable_id,
            "label": label
        }
    
    print(f"✅ Transformed {len(transformed)} ingredients")
    print(f"📝 Writing to {output_path}...")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(transformed, f, indent=2, ensure_ascii=False)
    
    return ingredient_map

def transform_recipes(input_path: str, output_path: str, ingredient_map: Dict[str, Dict]):
    """Transform recipes.json to new structure."""
    print("\n📖 Loading recipes...")
    with open(input_path, 'r', encoding='utf-8') as f:
        recipes = json.load(f)
    
    transformed = []
    recipe_counter = 0
    
    print(f"🔄 Transforming {len(recipes)} recipes...")
    for recipe in recipes:
        if not recipe or not recipe.get('title'):
            continue
        
        recipe_counter += 1
        stable_id = f"r{recipe_counter:05d}"
        
        # Transform ingredients
        ingredients = []
        if recipe.get('ingredientNames'):
            for ing_name in recipe['ingredientNames']:
                if ing_name in ingredient_map:
                    ing_data = ingredient_map[ing_name]
                    ingredients.append({
                        "id": ing_data['id'],
                        "name": ing_data['label'],
                        "quantity": 100  # Default quantity
                    })
        
        # Transform preparation steps
        preparation = []
        if recipe.get('steps'):
            for idx, step_text in enumerate(recipe['steps'], 1):
                prep_step = {
                    "step": f"Step {idx}",
                    "description": step_text,
                    "ingredients": [ing['id'] for ing in ingredients]
                }
                preparation.append(prep_step)
        
        transformed_recipe = {
            "id": stable_id,
            "title": recipe.get('title', ''),
            "cuisine": recipe.get('cuisine', 'Other'),
            "meal": recipe.get('meal', 'dinner'),
            "servings": recipe.get('servings', 4),
            "ingredients": ingredients,
            "summary": recipe.get('summary', ''),
            "time": recipe.get('timeMinutes', 30),
            "difficulty_level": recipe.get('difficultyLevel', 'medium'),
            "dietary_tags": recipe.get('dietaryTags', []),
            "source": recipe.get('source', 'food-com'),
            "img": recipe.get('imageUrl', '/images/default-recipe.jpg'),
            "preparation": preparation
        }
        
        transformed.append(transformed_recipe)
    
    print(f"✅ Transformed {len(transformed)} recipes")
    print(f"📝 Writing to {output_path}...")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(transformed, f, indent=2, ensure_ascii=False)

def main():
    base_dir = Path(__file__).parent / 'backend' / 'src' / 'main' / 'resources' / 'data'
    
    ingredients_file = base_dir / 'ingredients.json'
    recipes_file = base_dir / 'recipes.json'
    
    if not ingredients_file.exists() or not recipes_file.exists():
        print(f"❌ JSON files not found in {base_dir}")
        return False
    
    print("=" * 70)
    print("🍳 JSON Data Structure Transformer")
    print("=" * 70)
    
    # Transform ingredients
    ingredient_map = transform_ingredients(str(ingredients_file), str(ingredients_file))
    
    # Transform recipes
    transform_recipes(str(recipes_file), str(recipes_file), ingredient_map)
    
    print("\n" + "=" * 70)
    print("✅ Transformation complete!")
    print("=" * 70)
    return True

if __name__ == '__main__':
    main()
