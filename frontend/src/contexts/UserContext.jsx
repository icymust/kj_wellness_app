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
  // Initialize from localStorage, fallback to 2
  const [userId, setUserId] = useState(() => {
    const stored = localStorage.getItem('currentUserId');
    return stored ? parseInt(stored, 10) : 2;
  });

  // Persist to localStorage whenever userId changes
  useEffect(() => {
    localStorage.setItem('currentUserId', String(userId));
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
