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
  const [mealType, setMealType] = useState('all');
  const [cuisine, setCuisine] = useState('all');
  const [tagFilters, setTagFilters] = useState([]);

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

  const filteredRecipes = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    const ingTerm = ingredientTerm.trim().toLowerCase();

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
      if (tagFilters.length > 0) {
        const tags = Array.isArray(r.dietary_tags) ? r.dietary_tags : [];
        const hasAll = tagFilters.every((t) => tags.includes(t));
        if (!hasAll) return false;
      }
      return true;
    });
  }, [recipes, searchTerm, ingredientTerm, mealType, cuisine, tagFilters]);

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
