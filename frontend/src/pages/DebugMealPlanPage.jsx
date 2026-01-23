/**
 * STEP 5.5.2: Debug Meal Plan Page
 * 
 * This is a TEMPORARY READ-ONLY debug viewer.
 * NOT intended for production use.
 * 
 * Combines DayPlanView and WeekPlanView for visual inspection of AI meal plans.
 * Uses hardcoded test dates for simplicity.
 * 
 * Requirements Met:
 * - No styling required (uses inline styles)
 * - No authentication
 * - No state management libraries (React hooks only)
 * - No forms
 * - Read-only views only
 * - Uses plain fetch (via debugApi.js)
 * - Clarity > beauty
 */

import React from 'react';
import { DayPlanView } from '../components/DebugDayPlanView';
import { WeekPlanView } from '../components/DebugWeekPlanView';

export function DebugMealPlanPage() {
  // Hardcoded test dates for simplicity
  // In production, these would be date pickers or URL parameters
  const todayDate = '2024-01-25';
  const weekStartDate = '2024-01-22'; // Monday of the week

  return (
    <div style={{
      maxWidth: '1200px',
      margin: '0 auto',
      padding: '20px',
      fontFamily: 'system-ui, -apple-system, sans-serif',
      lineHeight: '1.6',
      backgroundColor: '#fff'
    }}>
      {/* Header */}
      <div style={{
        marginBottom: '30px',
        padding: '20px',
        backgroundColor: '#f0f0f0',
        borderRadius: '4px',
        borderLeft: '4px solid #ff6b6b'
      }}>
        <h1 style={{ margin: '0 0 10px 0', color: '#333' }}>
          🔍 Debug Meal Plan Viewer
        </h1>
        <p style={{ margin: '0 0 10px 0', color: '#666', fontSize: '0.95em' }}>
          <strong>⚠️ WARNING:</strong> This is a TEMPORARY debug interface for visually inspecting AI-generated meal plans.
          NOT intended for production use.
        </p>
        <p style={{ margin: '0', color: '#666', fontSize: '0.9em' }}>
          <strong>Backend:</strong> http://localhost:8080/api/debug/meal-plans
        </p>
      </div>

      {/* Instructions */}
      <div style={{
        marginBottom: '30px',
        padding: '15px',
        backgroundColor: '#e8f5e9',
        borderRadius: '4px',
        border: '1px solid #4caf50'
      }}>
        <h3 style={{ margin: '0 0 10px 0', color: '#2e7d32' }}>
          📝 How to Use
        </h3>
        <ul style={{ margin: '0', paddingLeft: '20px', color: '#555' }}>
          <li>Scroll down to see a single-day meal plan (date: {todayDate})</li>
          <li>Scroll further to see a 7-day weekly meal plan (week starting {weekStartDate})</li>
          <li>Each meal shows recipe details, calories, macros, ingredients, and preparation steps</li>
          <li>Click "View Full JSON" to inspect the raw API response data</li>
          <li>Check browser console for debug logs</li>
        </ul>
      </div>

      {/* Prerequisites Check */}
      <div style={{
        marginBottom: '30px',
        padding: '15px',
        backgroundColor: '#fff3e0',
        borderRadius: '4px',
        border: '1px solid #ff9800'
      }}>
        <h3 style={{ margin: '0 0 10px 0', color: '#e65100' }}>
          ✅ Prerequisites
        </h3>
        <p style={{ margin: '0', color: '#555', fontSize: '0.95em' }}>
          Before testing, ensure these steps are complete:
        </p>
        <ol style={{ margin: '10px 0 0 0', paddingLeft: '20px', color: '#555' }}>
          <li>User with id=1 exists in database</li>
          <li>User profile is configured with nutritional preferences (STEP 4.0)</li>
          <li>AI strategy has been generated and cached (STEP 4.1)</li>
          <li>Meal structure has been generated and cached (STEP 4.2)</li>
          <li>Backend is running on localhost:8080</li>
          <li>GROQ API is configured and working</li>
        </ol>
      </div>

      {/* Single Day View */}
      <div>
        <h2 style={{ marginTop: '40px', marginBottom: '0', color: '#333' }}>
          Single Day Meal Plan
        </h2>
        <DayPlanView date={todayDate} />
      </div>

      {/* Weekly View */}
      <div>
        <h2 style={{ marginTop: '40px', marginBottom: '0', color: '#333' }}>
          Weekly Meal Plan (7 Days)
        </h2>
        <WeekPlanView startDate={weekStartDate} />
      </div>

      {/* Footer */}
      <div style={{
        marginTop: '40px',
        padding: '20px',
        backgroundColor: '#f5f5f5',
        borderRadius: '4px',
        borderTop: '1px solid #ddd',
        fontSize: '0.9em',
        color: '#666'
      }}>
        <p style={{ margin: 0 }}>
          <strong>Debug Info:</strong> Open browser DevTools (F12) → Console tab to see debug logs
        </p>
        <p style={{ margin: '10px 0 0 0' }}>
          <strong>API Endpoint:</strong> GET {`http://localhost:8080/api/debug/meal-plans/day?date=YYYY-MM-DD`}
        </p>
        <p style={{ margin: '5px 0 0 0' }}>
          <strong>API Endpoint:</strong> GET {`http://localhost:8080/api/debug/meal-plans/week?startDate=YYYY-MM-DD`}
        </p>
        <p style={{ margin: '10px 0 0 0', fontStyle: 'italic', color: '#999' }}>
          This page is part of STEP 5.5.2 Debug Frontend implementation.
          It uses plain fetch and React hooks, no external state management.
        </p>
      </div>
    </div>
  );
}
