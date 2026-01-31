import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/RecipesPage.css';
import { getAccessToken } from '../lib/tokens';
import { api } from '../lib/api';

export function RecipesPage() {
  const navigate = useNavigate();
  const [recipes, setRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [ingredientTerm, setIngredientTerm] = useState('');
  const [allergyTerm, setAllergyTerm] = useState('');
  const [mealType, setMealType] = useState('all');
  const [cuisine, setCuisine] = useState('all');
  const [tagFilters, setTagFilters] = useState([]);
  const [caloriesMin, setCaloriesMin] = useState('');
  const [caloriesMax, setCaloriesMax] = useState('');
  const [proteinMin, setProteinMin] = useState('');
  const [proteinMax, setProteinMax] = useState('');
  const [carbsMin, setCarbsMin] = useState('');
  const [carbsMax, setCarbsMax] = useState('');
  const [fatsMin, setFatsMin] = useState('');
  const [fatsMax, setFatsMax] = useState('');
  const [timeMin, setTimeMin] = useState('');
  const [timeMax, setTimeMax] = useState('');

  useEffect(() => {
    const loadRecipes = async () => {
      try {
        setLoading(true);
        setError(null);
        const token = getAccessToken();
        const data = await api.getRecipes(token, null);
        setRecipes(Array.isArray(data) ? data : []);
      } catch (err) {
        setError('Failed to load recipes');
      } finally {
        setLoading(false);
      }
    };
    loadRecipes();
  }, []);

  const availableCuisines = useMemo(() => {
    const set = new Set();
    recipes.forEach((r) => {
      if (r.cuisine) set.add(r.cuisine);
    });
    return Array.from(set).sort((a, b) => a.localeCompare(b));
  }, [recipes]);

  const availableTags = useMemo(() => {
    const set = new Set();
    recipes.forEach((r) => {
      if (Array.isArray(r.dietary_tags)) {
        r.dietary_tags.forEach((t) => t && set.add(t));
      }
    });
    return Array.from(set).sort((a, b) => a.localeCompare(b));
  }, [recipes]);

  const recipeNutritionMap = useMemo(() => {
    const map = new Map();
    recipes.forEach((recipe) => {
      const ingredients = Array.isArray(recipe.ingredients) ? recipe.ingredients : [];
      let calories = 0;
      let protein = 0;
      let carbs = 0;
      let fats = 0;

      ingredients.forEach((ing) => {
        const nutrition = ing && ing.nutrition ? ing.nutrition : null;
        const quantity = typeof ing.quantity === 'number' ? ing.quantity : null;
        if (!nutrition || !quantity || quantity <= 0) return;
        const factor = quantity / 100;
        calories += (nutrition.calories || 0) * factor;
        protein += (nutrition.protein || 0) * factor;
        carbs += (nutrition.carbs || 0) * factor;
        fats += (nutrition.fats || 0) * factor;
      });

      map.set(recipe.id, { calories, protein, carbs, fats });
    });
    return map;
  }, [recipes]);

  const filteredRecipes = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    const ingTerm = ingredientTerm.trim().toLowerCase();
    const allergyTerms = allergyTerm
      .split(/[;,]/)
      .map((t) => t.trim().toLowerCase())
      .filter(Boolean);

    const toNumber = (value) => {
      if (value === '' || value === null || value === undefined) return null;
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : null;
    };

    const ranges = {
      caloriesMin: toNumber(caloriesMin),
      caloriesMax: toNumber(caloriesMax),
      proteinMin: toNumber(proteinMin),
      proteinMax: toNumber(proteinMax),
      carbsMin: toNumber(carbsMin),
      carbsMax: toNumber(carbsMax),
      fatsMin: toNumber(fatsMin),
      fatsMax: toNumber(fatsMax),
      timeMin: toNumber(timeMin),
      timeMax: toNumber(timeMax),
    };

    return recipes.filter((r) => {
      if (mealType !== 'all' && (r.meal || '').toLowerCase() !== mealType) {
        return false;
      }
      if (cuisine !== 'all' && r.cuisine !== cuisine) {
        return false;
      }
      if (term) {
        const text = `${r.title || ''} ${r.summary || ''}`.toLowerCase();
        if (!text.includes(term)) return false;
      }
      if (ingTerm) {
        const ingredients = Array.isArray(r.ingredients) ? r.ingredients : [];
        const hasIngredient = ingredients.some((i) =>
          (i.label || '').toLowerCase().includes(ingTerm)
        );
        if (!hasIngredient) return false;
      }
      if (allergyTerms.length > 0) {
        const ingredients = Array.isArray(r.ingredients) ? r.ingredients : [];
        const hasAllergy = ingredients.some((i) =>
          allergyTerms.some((termItem) =>
            (i.label || '').toLowerCase().includes(termItem)
          )
        );
        if (hasAllergy) return false;
      }
      if (tagFilters.length > 0) {
        const tags = Array.isArray(r.dietary_tags) ? r.dietary_tags : [];
        const hasAll = tagFilters.every((t) => tags.includes(t));
        if (!hasAll) return false;
      }

      const nutrition = recipeNutritionMap.get(r.id) || {
        calories: 0,
        protein: 0,
        carbs: 0,
        fats: 0,
      };

      if (ranges.caloriesMin !== null && nutrition.calories < ranges.caloriesMin) {
        return false;
      }
      if (ranges.caloriesMax !== null && nutrition.calories > ranges.caloriesMax) {
        return false;
      }
      if (ranges.proteinMin !== null && nutrition.protein < ranges.proteinMin) {
        return false;
      }
      if (ranges.proteinMax !== null && nutrition.protein > ranges.proteinMax) {
        return false;
      }
      if (ranges.carbsMin !== null && nutrition.carbs < ranges.carbsMin) {
        return false;
      }
      if (ranges.carbsMax !== null && nutrition.carbs > ranges.carbsMax) {
        return false;
      }
      if (ranges.fatsMin !== null && nutrition.fats < ranges.fatsMin) {
        return false;
      }
      if (ranges.fatsMax !== null && nutrition.fats > ranges.fatsMax) {
        return false;
      }

      if (ranges.timeMin !== null) {
        const time = typeof r.time === 'number' ? r.time : null;
        if (time === null || time < ranges.timeMin) return false;
      }
      if (ranges.timeMax !== null) {
        const time = typeof r.time === 'number' ? r.time : null;
        if (time !== null && time > ranges.timeMax) return false;
      }

      return true;
    });
  }, [
    recipes,
    searchTerm,
    ingredientTerm,
    allergyTerm,
    mealType,
    cuisine,
    tagFilters,
    caloriesMin,
    caloriesMax,
    proteinMin,
    proteinMax,
    carbsMin,
    carbsMax,
    fatsMin,
    fatsMax,
    timeMin,
    timeMax,
    recipeNutritionMap,
  ]);

  const toggleTag = (tag) => {
    setTagFilters((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    );
  };

  if (loading) {
    return (
      <div className="recipes-page">
        <div className="recipes-state">Loading recipes...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="recipes-page">
        <div className="recipes-state">
          <p>{error}</p>
          <button onClick={() => window.location.reload()} className="recipes-retry-btn">
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="recipes-page">
      <div className="recipes-header">
        <div>
          <h1>Recipes</h1>
          <p className="recipes-subtitle">Browse all recipes in the database</p>
        </div>
        <button className="recipes-back-btn" onClick={() => navigate(-1)}>
          Back
        </button>
      </div>

      <div className="recipes-filters">
        <div className="filter-group">
          <label>Search</label>
          <input
            type="text"
            placeholder="Search by name or description..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Ingredient</label>
          <input
            type="text"
            placeholder="Filter by ingredient..."
            value={ingredientTerm}
            onChange={(e) => setIngredientTerm(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Allergies (exclude)</label>
          <input
            type="text"
            placeholder="e.g. peanut, shellfish"
            value={allergyTerm}
            onChange={(e) => setAllergyTerm(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Meal Type</label>
          <select value={mealType} onChange={(e) => setMealType(e.target.value)}>
            <option value="all">All</option>
            <option value="breakfast">Breakfast</option>
            <option value="lunch">Lunch</option>
            <option value="dinner">Dinner</option>
            <option value="snack">Snack</option>
          </select>
        </div>
        <div className="filter-group">
          <label>Cuisine</label>
          <select value={cuisine} onChange={(e) => setCuisine(e.target.value)}>
            <option value="all">All</option>
            {availableCuisines.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
        <div className="filter-group">
          <label>Calories (min)</label>
          <input
            type="number"
            placeholder="Min kcal"
            value={caloriesMin}
            onChange={(e) => setCaloriesMin(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Calories (max)</label>
          <input
            type="number"
            placeholder="Max kcal"
            value={caloriesMax}
            onChange={(e) => setCaloriesMax(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Protein (min)</label>
          <input
            type="number"
            placeholder="Min g"
            value={proteinMin}
            onChange={(e) => setProteinMin(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Protein (max)</label>
          <input
            type="number"
            placeholder="Max g"
            value={proteinMax}
            onChange={(e) => setProteinMax(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Carbs (min)</label>
          <input
            type="number"
            placeholder="Min g"
            value={carbsMin}
            onChange={(e) => setCarbsMin(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Carbs (max)</label>
          <input
            type="number"
            placeholder="Max g"
            value={carbsMax}
            onChange={(e) => setCarbsMax(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Fats (min)</label>
          <input
            type="number"
            placeholder="Min g"
            value={fatsMin}
            onChange={(e) => setFatsMin(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Fats (max)</label>
          <input
            type="number"
            placeholder="Max g"
            value={fatsMax}
            onChange={(e) => setFatsMax(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Prep Time (min)</label>
          <input
            type="number"
            placeholder="Min minutes"
            value={timeMin}
            onChange={(e) => setTimeMin(e.target.value)}
          />
        </div>
        <div className="filter-group">
          <label>Prep Time (max)</label>
          <input
            type="number"
            placeholder="Max minutes"
            value={timeMax}
            onChange={(e) => setTimeMax(e.target.value)}
          />
        </div>
      </div>

      {availableTags.length > 0 && (
        <div className="recipes-tags">
          <span className="tags-label">Dietary tags:</span>
          <div className="tags-list">
            {availableTags.map((tag) => (
              <button
                key={tag}
                type="button"
                className={`tag-chip ${tagFilters.includes(tag) ? 'active' : ''}`}
                onClick={() => toggleTag(tag)}
              >
                {tag}
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="recipes-count">
        {filteredRecipes.length} recipe{filteredRecipes.length !== 1 ? 's' : ''}
      </div>

      <div className="recipes-list">
        {filteredRecipes.length === 0 ? (
          <div className="recipes-state">No recipes found</div>
        ) : (
          filteredRecipes.map((recipe) => (
            <div key={recipe.id} className="recipe-card">
              <div className="recipe-card-header">
                <div>
                  <h3>{recipe.title}</h3>
                  {recipe.summary && <p className="recipe-summary">{recipe.summary}</p>}
                  <div className="recipe-meta">
                    {recipe.meal && <span>🍽 {String(recipe.meal).toLowerCase()}</span>}
                    {recipe.cuisine && <span>🌍 {recipe.cuisine}</span>}
                    {recipe.time && <span>⏱ {recipe.time} min</span>}
                  </div>
                </div>
                <button
                  className="recipe-view-btn"
                  onClick={() => navigate(`/recipes/${recipe.stable_id || recipe.id}`)}
                >
                  View
                </button>
              </div>

              <div className="recipe-section">
                <h4>Ingredients</h4>
                <ul>
                  {(recipe.ingredients || []).map((ing, idx) => (
                    <li key={`${ing.label}-${idx}`}>
                      {ing.label} — {ing.quantity ? ing.quantity.toFixed(1) : '-'} {ing.unit || 'unit'}
                    </li>
                  ))}
                </ul>
              </div>

              <div className="recipe-section">
                <h4>Preparation</h4>
                <ol>
                  {(recipe.preparation || []).map((step, idx) => (
                    <li key={`${recipe.id}-step-${idx}`}>{step}</li>
                  ))}
                </ol>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
