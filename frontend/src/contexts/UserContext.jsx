/**
 * UserContext
 * 
 * Provides shared user state across the app.
 * Temporarily tracks current userId until authentication is wired.
 * 
 * Usage:
 *   const { userId, setUserId } = useUser();
 */

import React, { createContext, useContext, useState, useEffect } from 'react';

const UserContext = createContext(null);

export function UserProvider({ children }) {
  // Initialize from localStorage if present; otherwise stay null until login populates
  const [userId, setUserId] = useState(() => {
    const stored = localStorage.getItem('currentUserId');
    if (!stored) return null;
    const parsed = parseInt(stored, 10);
    return Number.isFinite(parsed) ? parsed : null;
  });

  // Persist to localStorage whenever userId changes; remove when cleared
  useEffect(() => {
    if (userId == null) {
      localStorage.removeItem('currentUserId');
    } else {
      localStorage.setItem('currentUserId', String(userId));
    }
  }, [userId]);

  return (
    <UserContext.Provider value={{ userId, setUserId }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUser() {
  // eslint-disable-next-line no-unused-vars
  const context = useContext(UserContext);
  if (!context) {
    throw new Error('useUser must be used within UserProvider');
  }
  return context;
}
