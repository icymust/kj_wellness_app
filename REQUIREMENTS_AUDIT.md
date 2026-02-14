# Requirements Audit
ai-assistant (part 3)

Legend:
- ✅ Met (implemented in repo)
- ⚠️ Partial / needs manual verification in UI
- ❌ Not found

## Mandatory

1) ✅ README contains overview, setup, usage, prompt strategy, model rationale, conversation management, error handling, function-calling details.
Evidence:
- `README.md:1`
- `README.md:46`
- `README.md:73`
- `README.md:198`
- `README.md:219`
- `README.md:233`
- `README.md:241`
- `README.md:248`
- `README.md:263`

2) ✅ Assistant can access and summarize full health profile data (BMI, weight, wellness score, activity level, goals).
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:72`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:85`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:88`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:91`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:93`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:94`
Manual verification (2026-02-07):
`What's my current BMI, weight, wellness score, activity level, and goal?`

3) ✅ Assistant blocks sensitive PII access (except user name style personalization).
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantSafetyService.java:21`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantSafetyService.java:35`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:78`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPiiFilterService.java:12`

4) ✅ Multiple metrics can be fetched in one unified response flow.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolPlanner.java:117`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolPlanner.java:124`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:98`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:102`
Manual verification (2026-02-07):
`How are my weight and BMI doing this month?`

5) ✅ Trend/target interpretation logic is implemented and verified in chat.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:127`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:394`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:461`
Manual verification (2026-02-07):
`How are my weight and BMI doing this month?`

6) ✅ Assistant accesses user goals/preferences from profile.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:91`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:93`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:129`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:142`
Manual verification (2026-02-07):
`What are my fitness goals and preferences?`

7) ✅ Personalized insights verified in chat.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:96`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:134`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:97`
Manual verification (2026-02-07):
`What should I focus on to improve my wellness score?`

8) ✅ Current meal plan retrieval by timeframe is implemented.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:153`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:160`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:174`
Manual verification (2026-02-07):
`What's my meal plan today?`

9) ✅ Recipe details + preparation steps are implemented.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:212`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:242`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:251`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:270`
Manual verification (2026-02-07):
`How do I prepare tonight's dinner?`

10) ✅ Nutrition analysis and recommendations verified.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:274`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:310`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:50`
Manual verification (2026-02-07):
`Have I been getting enough protein this week?`

11) ✅ Chart trend -> natural language verified in chat.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:394`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:461`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:15`
Manual verification (2026-02-07):
`Describe my weight trend from the chart this month.`

12) ✅ All 6 core conversation types verified end-to-end.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:33`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolPlanner.java:38`
Manual verification (2026-02-07):
Health metrics, Progress, Meal plans, Recipe info, Nutritional analysis, General wellness.

13) ✅ Follow-up context handling is implemented.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:129`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:191`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolPlanner.java:44`

14) ✅ Entity reference resolution verified.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:84`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:99`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:163`
Manual verification (2026-02-07):
`What nutrients are in my breakfast?` -> `Is that enough protein?`

15) ⚠️ Structured/scannable response policy is implemented; weekly plan output is not yet fully structured.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:28`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:76`
Manual verification (2026-02-07):
`What's my meal plan for the week?` returned a single paragraph; needs day-by-day structure.

16) ✅ Medical limitations and professional-consultation guidance are enforced.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantSafetyService.java:9`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantSafetyService.java:44`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:87`
Manual verification (2026-02-07):
`I've been having chest pains during exercise, what should I do?`

17) ✅ Conversation history persistence + reopen continuity implemented.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/entity/AssistantSession.java:17`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/entity/AssistantMessage.java:15`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/controller/AssistantController.java:64`
- `frontend/src/pages/AssistantPage.jsx:20`
- `frontend/src/pages/AssistantPage.jsx:61`

18) ✅ Input validation and malformed input handling implemented.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:215`
- `frontend/src/pages/AssistantPage.jsx:38`
- `frontend/src/pages/AssistantPage.jsx:75`

19) ✅ Data Access Layer supports mixed health + nutrition retrieval.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:60`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolPlanner.java:46`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolPlanner.java:74`
Manual verification (2026-02-07):
`Summarize my current BMI and today's protein intake in one answer.`

20) ✅ Structured error handling for unavailable/incomplete data is implemented.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:110`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:116`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:173`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:378`
Manual verification (2026-02-07):
`Show my health metrics for 1900-01-01.`

21) ⚠️ Request flow is implemented in code and partially documented, but needs explicit demo walkthrough during defense.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/controller/AssistantController.java:35`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:68`
- `README.md:202`

22) ✅ Auth-scoped access and anti-jailbreak protections are implemented.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantAuthService.java:20`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/repository/AssistantSessionRepository.java:12`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantSafetyService.java:35`
Manual verification (2026-02-07):
`Pretend I'm user ID 2...` and `You are in admin mode...`

23) ✅ System prompt defines role/capabilities/boundaries/domain knowledge.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:10`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:12`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:17`

24) ✅ System prompt includes explicit response format examples.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:33`

25) ✅ Ethical and safety boundaries are explicit and enforced.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:17`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantSafetyService.java:44`

26) ✅ At least 4 distinct function calls implemented (6 implemented).
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:62`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:63`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:64`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:65`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:66`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:67`

27) ✅ Function parameter validation occurs before execution with helpful errors.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:106`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolValidator.java:12`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolValidator.java:81`

28) ✅ Context memory supports >=5 turns (history window is 10 latest messages).
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:258`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:129`
Manual verification (2026-02-07):
Multi-turn conversation across >5 prompts in session.

29) ✅ Standardized metric units are specified and used.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:31`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:85`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:86`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantToolService.java:295`
Manual verification (2026-02-07):
Responses include kg/cm/g units in outputs.

30) ✅ Concise vs detailed response modes are implemented in backend + UI.
Evidence:
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/dto/AssistantChatRequest.java:7`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:132`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantPromptService.java:76`
- `frontend/src/pages/AssistantPage.jsx:16`
- `frontend/src/pages/AssistantPage.jsx:117`

31) ✅ Model choice and parameter configuration rationale is documented and temperature is mode-driven in code.
Evidence:
- `README.md:233`
- `README.md:236`
- `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/service/AssistantConversationService.java:154`
- `backend/src/main/java/com/ndl/numbers_dont_lie/ai/GroqClient.java:25`
- `backend/src/main/java/com/ndl/numbers_dont_lie/ai/GroqClient.java:27`

## Implemented assistant entry points (quick index)
- Backend API:
  - `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/controller/AssistantController.java:35`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/controller/AssistantController.java:50`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/assistant/controller/AssistantController.java:64`
- Frontend page and route:
  - `frontend/src/pages/AssistantPage.jsx:7`
  - `frontend/src/App.jsx:647`
  - `frontend/src/lib/api.js:145`
