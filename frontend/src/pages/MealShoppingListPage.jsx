import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import '../styles/MealShoppingList.css';
import { api } from '../lib/api';
import { getAccessToken } from '../lib/tokens';

export function MealShoppingListPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mealId = searchParams.get('mealId');

  const [shoppingList, setShoppingList] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [checkedItems, setCheckedItems] = useState({});
  const [itemEdits, setItemEdits] = useState({});

  const storageKey = useMemo(() => {
    if (!mealId) return null;
    return `mealShoppingChecked:${mealId}`;
  }, [mealId]);

  useEffect(() => {
    if (!storageKey) return;
    const saved = localStorage.getItem(storageKey);
    if (saved) {
      try {
        setCheckedItems(JSON.parse(saved));
      } catch {
        setCheckedItems({});
      }
    }
  }, [storageKey]);

  useEffect(() => {
    if (!storageKey) return;
    localStorage.setItem(storageKey, JSON.stringify(checkedItems));
  }, [checkedItems, storageKey]);

  const editsKey = useMemo(() => {
    if (!storageKey) return null;
    return `${storageKey}:edits`;
  }, [storageKey]);

  useEffect(() => {
    if (!editsKey) return;
    try {
      const raw = localStorage.getItem(editsKey);
      const parsed = raw ? JSON.parse(raw) : null;
      if (parsed && typeof parsed === 'object') {
        setItemEdits(parsed);
      } else {
        setItemEdits({});
      }
    } catch {
      setItemEdits({});
    }
  }, [editsKey]);

  const loadShoppingList = useCallback(async () => {
    if (!mealId) {
      setError('Meal not specified.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const token = getAccessToken();
      const data = await api.getMealShoppingList(token, mealId);
      setShoppingList(data);
      console.log('[SHOPPING_LIST_PAGE] Loaded meal shopping list');
      console.log('[SHOPPING_LIST_PAGE] Items count:', data?.items?.length || 0);
    } catch (err) {
      console.warn('Meal shopping list load failed', err);
      setError('Failed to load shopping list');
    } finally {
      setLoading(false);
    }
  }, [mealId]);

  useEffect(() => {
    loadShoppingList();
  }, [loadShoppingList]);

  const groupedItems = useMemo(() => {
    if (!shoppingList?.items) return [];
    const groups = new Map();
    shoppingList.items.forEach((item) => {
      const key = `${item.ingredient}|${item.unit}`;
      const edit = itemEdits[key];
      if (edit?.removed) return;
      const quantity = edit?.quantity ?? item.totalQuantity;
      const adjusted = { ...item, totalQuantity: quantity };
      const category = item.category || 'Other';
      if (!groups.has(category)) groups.set(category, []);
      groups.get(category).push(adjusted);
    });
    return Array.from(groups.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([category, items]) => ({
        category,
        items: items.sort((a, b) => (a.ingredient || '').localeCompare(b.ingredient || '')),
      }));
  }, [shoppingList, itemEdits]);

  const toggleChecked = (key) => {
    setCheckedItems((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  const updateItemQuantity = (key, baseQty, nextValue) => {
    const safeValue = Number.isFinite(nextValue) ? Math.max(0, nextValue) : baseQty;
    setItemEdits((prev) => {
      const next = { ...prev, [key]: { ...(prev[key] || {}), quantity: safeValue } };
      if (editsKey) {
        try {
          localStorage.setItem(editsKey, JSON.stringify(next));
        } catch {
          // ignore
        }
      }
      return next;
    });
  };

  const removeItem = (key) => {
    setItemEdits((prev) => {
      const next = { ...prev, [key]: { ...(prev[key] || {}), removed: true } };
      if (editsKey) {
        try {
          localStorage.setItem(editsKey, JSON.stringify(next));
        } catch {
          // ignore
        }
      }
      return next;
    });
  };

  if (loading) {
    return (
      <div className="meal-shopping-page">
        <div className="shopping-state">Loading shopping list...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="meal-shopping-page">
        <div className="shopping-state">
          <p>{error}</p>
          <button onClick={loadShoppingList} className="shopping-retry-btn">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!shoppingList || groupedItems.length === 0) {
    return (
      <div className="meal-shopping-page">
        <div className="shopping-header">
          <div>
            <h1>Meal Shopping List</h1>
            <p className="shopping-date-range">
              {shoppingList?.mealType ? shoppingList.mealType : 'Meal'}
              {shoppingList?.mealName ? ` — ${shoppingList.mealName}` : ''}
            </p>
          </div>
          <button className="shopping-back-btn" onClick={() => navigate(-1)}>
            Back
          </button>
        </div>
        <div className="shopping-state">No ingredients found for this meal</div>
      </div>
    );
  }

  return (
    <div className="meal-shopping-page">
      <div className="shopping-header">
        <div>
          <h1>Meal Shopping List</h1>
          <p className="shopping-date-range">
            {shoppingList?.mealType ? shoppingList.mealType : 'Meal'}
            {shoppingList?.mealName ? ` — ${shoppingList.mealName}` : ''}
          </p>
        </div>
        <button className="shopping-back-btn" onClick={() => navigate(-1)}>
          Back
        </button>
      </div>

      <div className="shopping-list">
        {groupedItems.map((group) => (
          <div key={group.category} className="shopping-category">
            <h3 className="shopping-category-title">{group.category}</h3>
            {group.items.map((item) => {
              const key = `${item.ingredient}|${item.unit}`;
              const isChecked = !!checkedItems[key];
              return (
                <div key={key} className={`shopping-item ${isChecked ? 'checked' : ''}`}>
                  <label className="shopping-checkbox">
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={() => toggleChecked(key)}
                    />
                  </label>
                  <div className="shopping-item-text">
                    <span className="shopping-ingredient">{item.ingredient}</span>
                    <span className="shopping-quantity">
                      {Number(item.totalQuantity).toFixed(1)} {item.unit}
                    </span>
                  </div>
                  <div className="shopping-item-actions">
                    <input
                      type="number"
                      min="0"
                      step="0.1"
                      value={Number(item.totalQuantity)}
                      onChange={(e) => updateItemQuantity(key, item.totalQuantity, parseFloat(e.target.value))}
                    />
                    <button type="button" onClick={() => removeItem(key)}>
                      Remove
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
