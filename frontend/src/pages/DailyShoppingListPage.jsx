import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import '../styles/DailyShoppingList.css';
import { useUser } from '../contexts/UserContext';
import { getAccessToken } from '../lib/tokens';
import { api } from '../lib/api';

function formatDateISO(date) {
  return date.toLocaleDateString('en-CA');
}

function formatQuantity(value) {
  const num = typeof value === 'number' ? value : 0;
  const rounded = Math.round(num);
  if (Math.abs(num - rounded) < 0.01) return `${rounded}`;
  return num.toFixed(1);
}

export function DailyShoppingListPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { userId, setUserId } = useUser();

  const [shoppingList, setShoppingList] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [checkedItems, setCheckedItems] = useState({});
  const [itemEdits, setItemEdits] = useState({});

  const date = useMemo(() => {
    const params = new URLSearchParams(location.search);
    const queryDate = params.get('date');
    if (queryDate) return queryDate;
    return formatDateISO(new Date());
  }, [location.search]);

  const storageKey = useMemo(() => {
    if (!userId || !date) return null;
    return `dailyShoppingChecked:${userId}:${date}`;
  }, [userId, date]);

  useEffect(() => {
    if (!storageKey) return;
    try {
      const raw = localStorage.getItem(storageKey);
      const parsed = raw ? JSON.parse(raw) : null;
      if (parsed && typeof parsed === 'object') {
        setCheckedItems(parsed);
      } else {
        setCheckedItems({});
      }
    } catch {
      setCheckedItems({});
    }
  }, [storageKey]);

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

  useEffect(() => {
    const fillUserId = async () => {
      if (userId) return;
      const token = getAccessToken();
      if (!token) return;
      try {
        const data = await api.me(token);
        const resolvedId = data?.user?.id || data?.id;
        if (resolvedId) {
          setUserId(resolvedId);
        }
      } catch (err) {
        console.warn('Auto-resolve userId failed', err);
      }
    };
    fillUserId();
  }, [userId, setUserId]);

  const loadShoppingList = useCallback(async () => {
    if (!userId) {
      setError('No active user. Sign in to view shopping list.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const token = getAccessToken();
      const data = await api.getDailyShoppingList(token, userId, date);
      setShoppingList(data);
      console.log('[SHOPPING_LIST_PAGE] Loaded daily shopping list');
      console.log('[SHOPPING_LIST_PAGE] Items count:', data?.items?.length || 0);
    } catch (err) {
      console.warn('Daily shopping list load failed', err);
      setError('Failed to load shopping list');
    } finally {
      setLoading(false);
    }
  }, [userId, date]);

  useEffect(() => {
    loadShoppingList();
  }, [loadShoppingList]);

  const groupedItems = useMemo(() => {
    if (!shoppingList?.items) return [];
    const groups = new Map();
    shoppingList.items.forEach((item) => {
      const key = `${item.ingredient}-${item.unit}`;
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
    setCheckedItems((prev) => {
      const next = { ...prev, [key]: !prev[key] };
      if (storageKey) {
        try {
          localStorage.setItem(storageKey, JSON.stringify(next));
        } catch {
          // ignore
        }
      }
      return next;
    });
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
      <div className="daily-shopping-page">
        <div className="shopping-state">Loading shopping list...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="daily-shopping-page">
        <div className="shopping-state">
          <p>Failed to load shopping list</p>
          <button onClick={loadShoppingList} className="shopping-retry-btn">
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!groupedItems.length) {
    return (
      <div className="daily-shopping-page">
        <div className="shopping-header">
          <div>
            <h1>Daily Shopping List</h1>
            <p className="shopping-date-range">{shoppingList?.date}</p>
          </div>
          <button
            className="shopping-back-btn"
            onClick={() => navigate('/meals/today')}
          >
            Back to Today Meal Plan
          </button>
        </div>
        <div className="shopping-state">No ingredients found for this day</div>
      </div>
    );
  }

  return (
    <div className="daily-shopping-page">
      <div className="shopping-header">
        <div>
          <h1>Daily Shopping List</h1>
          <p className="shopping-date-range">{shoppingList?.date}</p>
        </div>
        <button
          className="shopping-back-btn"
          onClick={() => navigate('/meals/today')}
        >
          Back to Today Meal Plan
        </button>
      </div>

      <div className="shopping-list">
        {groupedItems.map((group) => (
          <div key={group.category} className="shopping-category">
            <h3 className="shopping-category-title">{group.category}</h3>
            {group.items.map((item) => {
              const key = `${item.ingredient}-${item.unit}`;
              const isChecked = !!checkedItems[key];
              return (
                <div key={key} className={`shopping-item ${isChecked ? 'checked' : ''}`}>
                  <label className="shopping-checkbox">
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={() => toggleChecked(key)}
                    />
                    <span></span>
                  </label>
                  <div className="shopping-item-text">
                    <span className="shopping-ingredient">{item.ingredient}</span>
                    <span className="shopping-quantity">
                      {formatQuantity(item.totalQuantity)} {item.unit}
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
