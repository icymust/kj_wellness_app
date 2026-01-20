#!/usr/bin/env python3
"""
Process Food.com RAW_recipes.csv to generate JSON files for Spring Boot application.
Selects 500 high-quality diverse recipes and extracts 500+ unique ingredients.

Usage: python scripts/process_recipes.py /path/to/RAW_recipes.csv
"""

import sys
import json
import ast
import re
from collections import defaultdict, Counter
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import pandas as pd
import numpy as np


class RecipeProcessor:
    # Daily Value reference amounts for nutrient conversion
    DV_PROTEIN = 50  # grams
    DV_CARBS = 275   # grams
    DV_FAT = 78      # grams

    # Cuisine keywords mapping
    CUISINE_KEYWORDS = {
        'italian': ['italian', 'pasta', 'risotto', 'pizza', 'pesto'],
        'mexican': ['mexican', 'taco', 'enchilada', 'salsa', 'churro'],
        'asian': ['asian', 'chinese', 'japanese', 'thai', 'vietnamese', 'korean', 'stir-fry'],
        'mediterranean': ['mediterranean', 'greek', 'middle eastern', 'lebanese', 'moroccan'],
        'american': ['american', 'burger', 'bbq', 'fried', 'southern'],
        'indian': ['indian', 'curry', 'tikka', 'dal', 'tandoori'],
        'middle eastern': ['middle eastern', 'lebanese', 'persian', 'falafel', 'hummus'],
        'french': ['french', 'crepe', 'coq au vin', 'beef bourguignon'],
    }

    # Meal type keywords
    MEAL_KEYWORDS = {
        'breakfast': ['breakfast', 'brunch', 'pancake', 'waffle', 'omelet', 'smoothie'],
        'lunch': ['lunch', 'sandwich', 'salad', 'wrap', 'bowl'],
        'dinner': ['dinner', 'supper'],
        'snack': ['snack', 'appetizer', 'dip', 'chip'],
    }

    # Dietary tags to extract
    DIETARY_KEYWORDS = ['vegetarian', 'vegan', 'gluten-free', 'low-carb', 'keto', 'paleo',
                        'dairy-free', 'nut-free', 'egg-free', 'soy-free', 'low-sugar', 'low-fat']

    # Ingredient category defaults (kcal, protein, carbs, fats per 100g)
    INGREDIENT_DEFAULTS = {
        'vegetable': (40, 1.0, 9.0, 0.2),
        'fruit': (50, 0.5, 12.0, 0.3),
        'meat': (150, 25.0, 0.0, 5.0),
        'fish': (110, 20.0, 0.0, 2.0),
        'poultry': (165, 31.0, 0.0, 3.6),
        'grain': (350, 12.0, 70.0, 2.0),
        'dairy': (100, 7.0, 5.0, 5.0),
        'oil': (800, 0.0, 0.0, 90.0),
    }

    def __init__(self, csv_path: str):
        self.csv_path = csv_path
        self.df = None
        self.selected_recipes = []
        self.ingredients_dict = {}

    def load_csv(self):
        """Load and validate CSV file."""
        print(f"📂 Loading CSV from {self.csv_path}...")
        try:
            self.df = pd.read_csv(self.csv_path)
            print(f"✅ Loaded {len(self.df)} total recipes")
            print(f"   Columns: {list(self.df.columns)}")
            return True
        except Exception as e:
            print(f"❌ Error loading CSV: {e}")
            return False

    def parse_list_column(self, value) -> list:
        """Safely parse stringified Python list."""
        if pd.isna(value) or value == '':
            return []
        if isinstance(value, list):
            return value
        try:
            parsed = ast.literal_eval(value)
            return parsed if isinstance(parsed, list) else []
        except (ValueError, SyntaxError):
            return []

    def extract_cuisine(self, tags: List[str]) -> str:
        """Extract cuisine type from tags."""
        tags_lower = [tag.lower() for tag in tags]
        for cuisine, keywords in self.CUISINE_KEYWORDS.items():
            for keyword in keywords:
                if any(keyword in tag for tag in tags_lower):
                    return cuisine.title()
        return "Other"

    def extract_meal(self, tags: List[str]) -> str:
        """Extract meal type from tags."""
        tags_lower = [tag.lower() for tag in tags]
        for meal, keywords in self.MEAL_KEYWORDS.items():
            for keyword in keywords:
                if any(keyword in tag for tag in tags_lower):
                    return meal
        return "dinner"  # default

    def extract_dietary_tags(self, tags: List[str]) -> List[str]:
        """Extract dietary preference tags."""
        tags_lower = [tag.lower() for tag in tags]
        dietary = []
        for keyword in self.DIETARY_KEYWORDS:
            if any(keyword in tag for tag in tags_lower):
                dietary.append(keyword)
        return dietary

    def convert_nutrition(self, nutrition: List[float]) -> Dict[str, float]:
        """Convert nutrition from % Daily Value to grams."""
        if len(nutrition) < 7:
            return {'calories': 0, 'protein': 0, 'carbs': 0, 'fats': 0}

        try:
            calories = float(nutrition[0]) if nutrition[0] else 0
            protein_pct = float(nutrition[4]) if nutrition[4] else 0
            carbs_pct = float(nutrition[6]) if nutrition[6] else 0
            fat_pct = float(nutrition[1]) if nutrition[1] else 0

            protein = max(0, (protein_pct / 100) * self.DV_PROTEIN)
            carbs = max(0, (carbs_pct / 100) * self.DV_CARBS)
            fats = max(0, (fat_pct / 100) * self.DV_FAT)

            return {
                'calories': round(calories, 1),
                'protein': round(protein, 1),
                'carbs': round(carbs, 1),
                'fats': round(fats, 1)
            }
        except (TypeError, IndexError, ValueError):
            return {'calories': 0, 'protein': 0, 'carbs': 0, 'fats': 0}

    def clean_ingredient_name(self, name: str) -> str:
        """Clean and normalize ingredient name."""
        if not name:
            return ""
        # Remove quantity prefixes (e.g., "1 cup chicken" -> "chicken")
        name = re.sub(r'^[\d\s.\/]+', '', name)
        # Convert to lowercase
        name = name.lower().strip()
        # Remove special characters but keep spaces
        name = re.sub(r'[^a-z0-9\s]', '', name)
        # Remove extra spaces
        name = re.sub(r'\s+', ' ', name).strip()
        return name

    def filter_recipes(self) -> pd.DataFrame:
        """Filter recipes by quality criteria."""
        print("\n🔍 Filtering recipes by quality criteria...")
        
        # Parse columns
        self.df['ingredients_list'] = self.df['ingredients'].apply(self.parse_list_column)
        self.df['steps_list'] = self.df['steps'].apply(self.parse_list_column)
        self.df['tags_list'] = self.df['tags'].apply(self.parse_list_column)
        self.df['nutrition_list'] = self.df['nutrition'].apply(self.parse_list_column)

        # Apply filters
        filtered = self.df[
            (self.df['ingredients_list'].apply(len) >= 5) &
            (self.df['ingredients_list'].apply(len) <= 20) &
            (self.df['minutes'] >= 15) &
            (self.df['minutes'] <= 120) &
            (self.df['steps_list'].apply(len) >= 5) &
            (self.df['description'].notna()) &
            (self.df['description'] != '')
        ].copy()

        print(f"✅ After quality filters: {len(filtered)} recipes")
        return filtered

    def select_diverse_recipes(self, filtered_df: pd.DataFrame, target: int = 500) -> pd.DataFrame:
        """Select diverse recipes across cuisines, meals, and difficulty levels."""
        print(f"\n🎯 Selecting {target} diverse recipes...")

        filtered_df['cuisine'] = filtered_df['tags_list'].apply(self.extract_cuisine)
        filtered_df['meal'] = filtered_df['tags_list'].apply(self.extract_meal)
        filtered_df['difficulty'] = filtered_df['steps_list'].apply(
            lambda x: 'easy' if len(x) <= 8 else ('medium' if len(x) <= 15 else 'hard')
        )

        # Target distribution
        cuisine_targets = {
            'Italian': 60, 'Mexican': 60, 'Asian': 60, 'Mediterranean': 60,
            'American': 60, 'Indian': 50, 'Middle Eastern': 50, 'French': 50, 'Other': 50
        }
        meal_targets = {'breakfast': 100, 'lunch': 200, 'dinner': 150, 'snack': 50}
        difficulty_targets = {'easy': 250, 'medium': 175, 'hard': 75}

        selected = []
        cuisine_counts = Counter()
        meal_counts = Counter()
        difficulty_counts = Counter()

        # First pass: prioritize recipes to hit targets
        for _, row in filtered_df.sample(frac=1, random_state=42).iterrows():
            if len(selected) >= target:
                break

            cuisine = row['cuisine']
            meal = row['meal']
            difficulty = row['difficulty']

            # Check if we still need this combo
            can_add = (
                cuisine_counts[cuisine] < cuisine_targets.get(cuisine, 0) and
                meal_counts[meal] < meal_targets.get(meal, 0) and
                difficulty_counts[difficulty] < difficulty_targets.get(difficulty, 0)
            )

            if can_add:
                selected.append(row)
                cuisine_counts[cuisine] += 1
                meal_counts[meal] += 1
                difficulty_counts[difficulty] += 1

        # Second pass: fill remaining slots with any quality recipes
        remaining_needed = target - len(selected)
        if remaining_needed > 0:
            print(f"\n   First pass: {len(selected)} recipes")
            print(f"   Second pass: filling {remaining_needed} more recipes...")
            
            # Get recipes not yet selected
            selected_ids = {row['id'] for row in selected}
            remaining_recipes = filtered_df[~filtered_df['id'].isin(selected_ids)]
            
            # Add random quality recipes until we hit target
            for _, row in remaining_recipes.sample(n=min(remaining_needed, len(remaining_recipes)), random_state=42).iterrows():
                selected.append(row)
                cuisine_counts[row['cuisine']] += 1
                meal_counts[row['meal']] += 1
                difficulty_counts[row['difficulty']] += 1

        selected_df = pd.DataFrame(selected)
        print(f"✅ Selected {len(selected_df)} recipes")

        # Print distribution
        print("\n📊 Distribution:")
        print(f"  Cuisines: {dict(cuisine_counts)}")
        print(f"  Meals: {dict(meal_counts)}")
        print(f"  Difficulty: {dict(difficulty_counts)}")

        return selected_df

    def extract_all_ingredients(self, recipes_df: pd.DataFrame) -> Dict[str, Dict]:
        """Extract and aggregate unique ingredients from selected recipes."""
        print("\n🥘 Extracting unique ingredients...")

        ingredient_data = defaultdict(lambda: {
            'count': 0,
            'calories': 0,
            'protein': 0,
            'carbs': 0,
            'fats': 0,
            'quantities': [],
            'category': None
        })

        for _, row in recipes_df.iterrows():
            ingredients = row.get('ingredients_list', [])
            nutrition = self.convert_nutrition(row.get('nutrition_list', []))

            # Per-ingredient nutrition (divide by number of ingredients)
            ing_count = len(ingredients) if ingredients else 1
            ing_calories = nutrition['calories'] / ing_count if ing_count > 0 else 0
            ing_protein = nutrition['protein'] / ing_count if ing_count > 0 else 0
            ing_carbs = nutrition['carbs'] / ing_count if ing_count > 0 else 0
            ing_fats = nutrition['fats'] / ing_count if ing_count > 0 else 0

            for ing_name in ingredients:
                cleaned_name = self.clean_ingredient_name(ing_name)
                if cleaned_name:
                    ingredient_data[cleaned_name]['count'] += 1
                    ingredient_data[cleaned_name]['calories'] += ing_calories
                    ingredient_data[cleaned_name]['protein'] += ing_protein
                    ingredient_data[cleaned_name]['carbs'] += ing_carbs
                    ingredient_data[cleaned_name]['fats'] += ing_fats

        # Average nutrition and assign categories
        final_ingredients = {}
        for ing_name, data in ingredient_data.items():
            avg_count = data['count']
            final_ingredients[ing_name] = {
                'label': ing_name,
                'unit': 'gram',
                'quantityPer100': 100.0,
                'calories': round(data['calories'] / avg_count, 1) if avg_count > 0 else 0,
                'protein': round(data['protein'] / avg_count, 1) if avg_count > 0 else 0,
                'carbs': round(data['carbs'] / avg_count, 1) if avg_count > 0 else 0,
                'fats': round(data['fats'] / avg_count, 1) if avg_count > 0 else 0,
                'count': avg_count
            }

        # Apply default nutrition where data is missing
        calculated_count = 0
        default_count = 0
        for ing_name, ing_data in final_ingredients.items():
            if ing_data['calories'] > 0:
                calculated_count += 1
            else:
                # Assign default based on keywords
                category = self._categorize_ingredient(ing_name)
                defaults = self.INGREDIENT_DEFAULTS.get(category, self.INGREDIENT_DEFAULTS['vegetable'])
                ing_data['calories'] = defaults[0]
                ing_data['protein'] = defaults[1]
                ing_data['carbs'] = defaults[2]
                ing_data['fats'] = defaults[3]
                default_count += 1

        print(f"✅ Found {len(final_ingredients)} unique ingredients")
        print(f"   With calculated nutrition: {calculated_count}")
        print(f"   With default nutrition: {default_count}")

        self.ingredients_dict = final_ingredients
        return final_ingredients

    def _categorize_ingredient(self, name: str) -> str:
        """Categorize ingredient by keywords for default nutrition."""
        name_lower = name.lower()

        vegetables = ['carrot', 'lettuce', 'spinach', 'broccoli', 'tomato', 'onion', 'garlic',
                      'pepper', 'celery', 'cucumber', 'pea', 'bean', 'squash']
        fruits = ['apple', 'banana', 'orange', 'berry', 'grape', 'lemon', 'lime', 'pineapple']
        meats = ['beef', 'pork', 'lamb', 'ham', 'bacon', 'sausage', 'veal']
        fish = ['salmon', 'tuna', 'cod', 'shrimp', 'lobster', 'crab', 'fish']
        poultry = ['chicken', 'turkey', 'duck']
        grains = ['rice', 'pasta', 'bread', 'flour', 'wheat', 'oat', 'cereal', 'corn']
        dairy = ['milk', 'cheese', 'yogurt', 'butter', 'cream', 'egg']
        oils = ['oil', 'butter', 'ghee']

        for keyword in vegetables:
            if keyword in name_lower:
                return 'vegetable'
        for keyword in fruits:
            if keyword in name_lower:
                return 'fruit'
        for keyword in meats:
            if keyword in name_lower:
                return 'meat'
        for keyword in fish:
            if keyword in name_lower:
                return 'fish'
        for keyword in poultry:
            if keyword in name_lower:
                return 'poultry'
        for keyword in grains:
            if keyword in name_lower:
                return 'grain'
        for keyword in dairy:
            if keyword in name_lower:
                return 'dairy'
        for keyword in oils:
            if keyword in name_lower:
                return 'oil'

        return 'vegetable'  # default

    def build_recipe_json(self, recipes_df: pd.DataFrame) -> List[Dict]:
        """Build recipe JSON objects."""
        recipes = []

        for _, row in recipes_df.iterrows():
            steps = row.get('steps_list', [])
            ingredients = row.get('ingredients_list', [])

            recipe = {
                'title': str(row['name']).strip(),
                'cuisine': row['cuisine'],
                'meal': row['meal'],
                'servings': 4,  # default
                'summary': str(row.get('description', '')).strip()[:500],
                'timeMinutes': int(row['minutes']),
                'difficultyLevel': row['difficulty'],
                'source': 'food-com',
                'imageUrl': '/images/default-recipe.jpg',
                'dietaryTags': self.extract_dietary_tags(row['tags_list']),
                'ingredientNames': [self.clean_ingredient_name(ing) for ing in ingredients],
                'steps': steps
            }
            recipes.append(recipe)

        return recipes

    def save_json(self, recipes: List[Dict], ingredients: Dict):
        """Save recipes and ingredients to JSON files."""
        output_dir = Path(self.csv_path).parent.parent / 'backend' / 'src' / 'main' / 'resources' / 'data'
        output_dir.mkdir(parents=True, exist_ok=True)

        # Save recipes
        recipes_path = output_dir / 'recipes.json'
        with open(recipes_path, 'w', encoding='utf-8') as f:
            json.dump(recipes, f, indent=2, ensure_ascii=False)
        print(f"\n✅ Saved {len(recipes)} recipes to {recipes_path}")

        # Save ingredients (remove metadata)
        ingredients_list = []
        for ing in ingredients.values():
            ing_clean = {k: v for k, v in ing.items() if k != 'count'}
            ingredients_list.append(ing_clean)

        ingredients_path = output_dir / 'ingredients.json'
        with open(ingredients_path, 'w', encoding='utf-8') as f:
            json.dump(ingredients_list, f, indent=2, ensure_ascii=False)
        print(f"✅ Saved {len(ingredients_list)} ingredients to {ingredients_path}")

    def print_summary_lists(self, recipes: List[Dict], ingredients: Dict):
        """Print detailed lists of all recipes and ingredients."""
        print("\n" + "=" * 60)
        print("📋 SELECTED RECIPES LIST")
        print("=" * 60)
        
        # Group by cuisine
        by_cuisine = defaultdict(list)
        for recipe in recipes:
            by_cuisine[recipe['cuisine']].append(recipe['title'])
        
        for cuisine in sorted(by_cuisine.keys()):
            print(f"\n{cuisine} ({len(by_cuisine[cuisine])} recipes):")
            for i, title in enumerate(sorted(by_cuisine[cuisine]), 1):
                print(f"  {i}. {title}")
        
        print("\n" + "=" * 60)
        print("🥘 INGREDIENTS LIST")
        print("=" * 60)
        print(f"Total: {len(ingredients)} unique ingredients\n")
        
        # Sort ingredients alphabetically
        sorted_ingredients = sorted(ingredients.items(), key=lambda x: x[0])
        
        for i, (name, data) in enumerate(sorted_ingredients, 1):
            calories = data.get('calories', 0)
            protein = data.get('protein', 0)
            carbs = data.get('carbs', 0)
            fats = data.get('fats', 0)
            print(f"{i:4d}. {name:30s} | {calories:6.1f} kcal | P: {protein:4.1f}g | C: {carbs:4.1f}g | F: {fats:4.1f}g")
        
        print("\n" + "=" * 60)

    def process(self):
        """Main processing pipeline."""
        print("=" * 60)
        print("🍳 Food.com Recipe Processor")
        print("=" * 60)

        if not self.load_csv():
            return False

        # Filter and select
        filtered_df = self.filter_recipes()
        selected_df = self.select_diverse_recipes(filtered_df, target=500)

        # Extract ingredients
        self.extract_all_ingredients(selected_df)

        # Build JSON
        recipes_json = self.build_recipe_json(selected_df)

        # Save outputs
        self.save_json(recipes_json, self.ingredients_dict)

        # Print summary lists
        self.print_summary_lists(recipes_json, self.ingredients_dict)

        print("=" * 60)
        print("✅ Processing complete!")
        print("=" * 60)
        return True


def main():
    if len(sys.argv) < 2:
        print("Usage: python scripts/process_recipes.py /path/to/RAW_recipes.csv")
        sys.exit(1)

    csv_path = sys.argv[1]
    if not Path(csv_path).exists():
        print(f"❌ File not found: {csv_path}")
        sys.exit(1)

    processor = RecipeProcessor(csv_path)
    success = processor.process()
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
