import React, { useEffect, useMemo, useState } from 'react';
import { getAccessToken } from '../lib/tokens';
import { api } from '../lib/api';

const SESSION_STORAGE_KEY = 'assistantSessionId';

export default function AssistantPage() {
  const token = useMemo(() => getAccessToken(), []);
  const [sessionId, setSessionId] = useState(() => {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    const parsed = raw ? parseInt(raw, 10) : null;
    return Number.isFinite(parsed) ? parsed : null;
  });
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [responseMode, setResponseMode] = useState('concise');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadHistory = async () => {
      if (!token || !sessionId) return;
      try {
        const data = await api.assistantHistory(token, sessionId);
        const rows = Array.isArray(data) ? data : [];
        setMessages(rows.map((item) => ({ role: item.role, content: item.content })));
      } catch {
        localStorage.removeItem(SESSION_STORAGE_KEY);
        setSessionId(null);
        setMessages([]);
      }
    };
    loadHistory();
  }, [token, sessionId]);

  const sendMessage = async () => {
    const text = input.trim();
    if (!text) {
      setError('Please enter a message.');
      return;
    }
    if (!token) {
      setError('Sign in to use the assistant.');
      return;
    }

    setLoading(true);
    setError(null);
    setInput('');

    const userMessage = { role: 'user', content: text };
    setMessages((prev) => [...prev, userMessage]);

    try {
      const data = await api.assistantChat(token, {
        sessionId,
        message: text,
        responseMode,
      });

      if (data?.sessionId && data.sessionId !== sessionId) {
        setSessionId(data.sessionId);
        localStorage.setItem(SESSION_STORAGE_KEY, String(data.sessionId));
      }

      const assistantText = data?.assistantMessage || 'No response from assistant.';
      setMessages((prev) => [...prev, { role: 'assistant', content: assistantText }]);

      if (Array.isArray(data?.warnings) && data.warnings.length > 0) {
        setMessages((prev) => [
          ...prev,
          { role: 'assistant', content: `Warnings: ${data.warnings.join(' | ')}` },
        ]);
      }
    } catch (err) {
      setError(err?.data?.error || err?.message || 'Assistant request failed.');
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: 'Assistant is temporarily unavailable. Please try again.' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const startNewConversation = async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const created = await api.assistantCreateSession(token, { title: 'New conversation' });
      const id = created?.sessionId;
      if (id) {
        setSessionId(id);
        localStorage.setItem(SESSION_STORAGE_KEY, String(id));
      } else {
        setSessionId(null);
        localStorage.removeItem(SESSION_STORAGE_KEY);
      }
      setMessages([]);
    } catch (err) {
      setError(err?.data?.error || err?.message || 'Failed to create session.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section style={{ border: '1px solid #ddd', borderRadius: 12, padding: 16, maxWidth: 920 }}>
      <h2 style={{ marginTop: 0 }}>AI Assistant</h2>
      <p style={{ marginTop: 0, color: '#555' }}>
        Ask about your health metrics, goals, meal plans, recipes, nutrition, and trends.
      </p>

      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 12 }}>
        <label htmlFor="response-mode">Response mode:</label>
        <select
          id="response-mode"
          value={responseMode}
          onChange={(e) => setResponseMode(e.target.value)}
          disabled={loading}
        >
          <option value="concise">Concise</option>
          <option value="detailed">Detailed</option>
        </select>
        <button onClick={startNewConversation} disabled={loading}>
          New Conversation
        </button>
      </div>

      <div
        style={{
          border: '1px solid #e5e5e5',
          borderRadius: 10,
          minHeight: 320,
          maxHeight: 520,
          overflowY: 'auto',
          padding: 12,
          background: '#fafafa',
          marginBottom: 12,
        }}
      >
        {messages.length === 0 ? (
          <p style={{ color: '#777' }}>No messages yet. Start with: "What\'s my current BMI?"</p>
        ) : (
          messages.map((msg, idx) => (
            <div
              key={`${msg.role}-${idx}`}
              style={{
                marginBottom: 10,
                padding: 10,
                borderRadius: 8,
                background: msg.role === 'user' ? '#e8f1ff' : '#ffffff',
                border: '1px solid #ddd',
              }}
            >
              <b>{msg.role === 'user' ? 'You' : 'Assistant'}:</b>
              <div style={{ whiteSpace: 'pre-wrap', marginTop: 4 }}>{msg.content}</div>
            </div>
          ))
        )}
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask a question about your health or nutrition"
          style={{ flex: 1, padding: 10 }}
          disabled={loading}
          onKeyDown={(e) => {
            if (e.key === 'Enter') sendMessage();
          }}
        />
        <button onClick={sendMessage} disabled={loading}>
          {loading ? 'Sending...' : 'Send'}
        </button>
      </div>

      {error && <p style={{ color: '#b00020', marginTop: 10 }}>{error}</p>}
      <p style={{ fontSize: 12, color: '#666', marginTop: 10 }}>
        Medical note: this assistant does not provide diagnosis. For urgent symptoms, seek professional care.
      </p>
    </section>
  );
}
