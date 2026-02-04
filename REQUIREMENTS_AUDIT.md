# Requirements Audit

Legend:
- ✅ Met (evidence found in repo)
- ⚠️ Partial / needs manual verification
- ❌ Not found

## Mandatory

1. README contains clear project overview, setup, and usage guide.
- ✅ `README.md`

2. Documentation includes:
- Overview of prompt engineering strategy
- AI model selection rationale
- Data model decisions
- Error handling methods used in the project
- ✅ Documented in `README.md` sections 5–8.

3. Nutritional planning supports at least 15 dietary preferences and 10 allergies/intolerances.
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/model/nutrition/NutritionalPreferencesConstants.java`
  - 15 dietary preferences, 10 allergies

4. Platform collects and utilizes user preferences (dietary, allergies/intolerances, disliked ingredients, cuisine prefs, calorie & macro targets, meal frequency & timing).
- ✅ Collection (UI + DTOs):
  - `frontend/src/components/NutritionalPreferences.jsx`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/dto/nutrition/NutritionalPreferencesDto.java`
- ✅ Utilization in meal generation:
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/service/DayPlanAssemblerService.java`

5. System reuses Project 1 info without duplicate input.
- ✅ Meal planner consumes Project 1 profile + meal frequency
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/service/impl/UserProfileServiceImpl.java`
  - UI still allows full input in `frontend/src/pages/Profile.jsx`

6. Input fields are pre-filled for info already provided; only confirmation should be asked.
- ✅ Prefilled and read-only by default with Confirm/Edit flow
  - `frontend/src/pages/Profile.jsx`
  - `frontend/src/components/NutritionalPreferences.jsx`

7. All dates/times follow ISO 8601.
- ✅ API uses ISO dates; UI displays ISO strings (YYYY-MM-DD / HH:mm) and timestamps in ISO
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java`
  - `frontend/src/pages/MealPlanPage.jsx`, `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - `frontend/src/components/NutritionalPreferences.jsx`

8. System generates complete daily & weekly meal plans based on user preferences.
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java`
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/service/DayPlanAssemblerService.java`
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/service/WeeklyMealPlanService.java`

9. Both daily and weekly meal plan durations supported.
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/entity/PlanDuration.java`

10. System allows customizing meal structures (meals/snacks per day, timing).
- ✅ `frontend/src/components/NutritionalPreferences.jsx`
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/service/nutrition/NutritionalPreferencesService.java`

11. Each meal has basic info (name, type, nutritional value).
- ✅ Name/type displayed with kcal per meal (planned/target fallback)
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/entity/Meal.java`
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`

12. Alternative meal options & replacements.
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java` (replace endpoints)
- ✅ `frontend/src/pages/ReplaceMealPage.jsx`

13. Reordering meals within a day or swapping between days (e.g. Monday dinner ↔ Tuesday dinner).
- ✅ Reorder within day implemented (requirement allows OR)
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java` (move)
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - Logic: meals are re-ordered by index within the same day; UI prevents moving beyond first/last.

14. Manual custom meals.
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java` (custom add/delete)
- ✅ `frontend/src/pages/MealPlanPage.jsx`, `frontend/src/pages/WeeklyMealPlanPage.jsx`

15. Regenerate individual meals or entire plans while preserving preferences.
- ✅ individual meal replacement + day/week refresh
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java`

16. Shopping list can be generated from a specific meal or meal plan.
- ✅ Per-meal + day/week shopping list endpoints + UI links
  - `backend/src/main/java/com/ndl/numbers_dont_lie/shoppinglist/ShoppingListController.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/shoppinglist/service/ShoppingListService.java`
  - `frontend/src/pages/MealShoppingListPage.jsx`
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - Logic: ingredients are aggregated by normalized name + unit and summed.

17. Shopping list items categorized by food group (>=5 categories).
- ✅ Category inference (Dairy, Protein, Produce, Grains, Pantry, Beverages, Other) + UI grouping
  - `backend/src/main/java/com/ndl/numbers_dont_lie/shoppinglist/service/ShoppingListService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/shoppinglist/dto/ShoppingListItemDto.java`
  - `frontend/src/pages/DailyShoppingListPage.jsx`
  - `frontend/src/pages/WeeklyShoppingListPage.jsx`
  - `frontend/src/pages/MealShoppingListPage.jsx`
  - Logic: server assigns category by keyword mapping; UI groups items by category.

18. Shopping list allows quantity adjustments and item exclusions.
- ✅ Local adjustments + removal persisted in localStorage per list
  - `frontend/src/pages/DailyShoppingListPage.jsx`
  - `frontend/src/pages/WeeklyShoppingListPage.jsx`
  - `frontend/src/pages/MealShoppingListPage.jsx`
  - Logic: adjustments/removals stored under list-specific localStorage keys; reloaded on mount.

19. Sequential prompting with at least 3 distinct steps used for meal plan generation.
- ✅ Steps 4.1 → 4.2 → 4.3.1/4.3.2
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/AiStrategyService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeGenerationService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/service/DayPlanAssemblerService.java`
  - Flow detail (with line refs; full step‑by‑step):
    - STEP 4.1 Strategy analysis (inputs → output):
      - Input: normalized user profile (goal, preferences, allergies, meal frequency, etc.).
      - Prompt build: `buildStrategyPrompt(...)` (`AiStrategyService.java` ~112–137).
      - AI call + validation: `analyzeStrategy(...)` (`AiStrategyService.java` ~47–60).
      - Output: `AiStrategyResult` (targets, macros, constraints) cached for reuse (`cache.putStrategyResult(...)`, ~59–60).
    - STEP 4.2 Meal structure distribution (uses Step 4.1 output):
      - Requires cached strategy; if missing, throws with explicit message (`AiStrategyService.java` ~80–87).
      - Prompt build: `buildMealStructurePrompt(...)` (`AiStrategyService.java` ~192–240).
      - AI call + validation: `analyzeMealStructure(...)` (`AiStrategyService.java` ~78–103).
      - Output: `AiMealStructureResult` (meal slots + calorie targets) cached (`cache.putMealStructureResult(...)`, ~95–102).
    - STEP 4.3.1 Retrieval (RAG):
      - Similar recipes retrieved and attached to generation request as context.
      - Used when building the augmented prompt (`RecipeGenerationService.java` ~153–166).
    - STEP 4.3.2 Generation (uses Step 4.1 + 4.2 + 4.3.1):
      - Orchestrator `generate(...)` builds augmented prompt and calls AI with function contract (`RecipeGenerationService.java` ~88–119).
      - Prompt explicitly injects: Step 4.1 strategy + Step 4.2 meal slot + RAG context (`RecipeGenerationService.java` ~136–166).
      - Nutrition is not guessed; it is calculated via function calling and injected back into final JSON (`RecipeGenerationService.java` ~190–216).
    - Day plan assembly (consumes cached results):
      - Fetches cached strategy + structure (`DayPlanAssemblerService.java` ~174–184).
      - Expands meal slots + generates per‑slot meals, then assembles `DayPlan` (`DayPlanAssemblerService.java` ~229–237).

20. RAG used to generate recipes and nutritional values.
- ✅ RAG retrieval + augmented prompts for recipes; nutrition values computed from ingredient DB via function calling
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeRetrievalService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeGenerationService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/FunctionCallingOrchestrator.java`
  - Logic: embed query → retrieve top‑K recipes/ingredients → augment prompt → generate recipe; nutrition via function calling.
  - Full paths (for quick reference):
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Recipe.java`
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Ingredient.java`
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/ai/embedding/RecipeEmbeddingService.java`
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/ai/AiConfig.java`
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/ai/vector/InMemoryVectorStore.java`
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeRetrievalService.java`
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeGenerationService.java`
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/FunctionCallingOrchestrator.java`
  - RAG pipeline cheat‑sheet (database → embedding → retrieval → augmentation → generation):
    - Database layer (source of truth):
      - Recipes + ingredients stored in DB tables with embedding columns.
      - Recipe entity embedding field: `Recipe.java` ~94–96 (`embedding` real[]).
      - Ingredient entity embedding field: `Ingredient.java` ~55–56 (`embedding` real[]).
    - Embedding generation:
      - `RecipeEmbeddingService.generateEmbedding(...)` builds a weighted text representation (title 3x, cuisine 2x, tags, summary) and embeds it. (`RecipeEmbeddingService.java` ~34–103)
      - `embedAndSave(...)` persists embedding to DB. (`RecipeEmbeddingService.java` ~44–51)
      - Embedding engine is configured in `AiConfig.java` (SimpleTfIdfEmbedding). (`AiConfig.java` ~50–57)
    - Vector store + similarity search:
      - In‑memory cosine similarity vector store: `InMemoryVectorStore.java` ~10–84
      - `VectorStore.search(...)` returns Top‑N recipe IDs by similarity. (`InMemoryVectorStore.java` ~36–55)
      - Vector store bean configured in `AiConfig.java` ~55–57.
    - Retrieval (query → vectors → recipes):
      - `RecipeRetrievalService.retrieve(...)` builds query text, embeds it, searches vector store, and fetches recipes. (`RecipeRetrievalService.java` ~59–95)
      - `buildQueryText(...)` weights fields (cuisine, dietary restrictions, meal type, macro focus). (`RecipeRetrievalService.java` ~101–138)
    - Augmentation (inject retrieved context into prompt):
      - `RecipeGenerationService.buildAugmentedPrompt(...)` adds retrieved recipe summaries as context for the AI. (`RecipeGenerationService.java` ~153–166)
    - Generation (AI uses augmented prompt + function calling):
      - `RecipeGenerationService.generate(...)` builds augmented prompt and calls AI with function schema. (`RecipeGenerationService.java` ~88–119)
      - Function calling computes nutrition from ingredient DB; AI is forced to embed those values. (`RecipeGenerationService.java` ~190–216)
    - Why RAG is better than generation from scratch:
      - Retrieved recipes provide realistic ingredient combinations and cuisine consistency.
      - Reduces hallucination risk and improves diversity.
      - Nutrition is computed from DB via function calling, not guessed.

21. Recipe & ingredient DBs have at least 500 entries each.
- ✅ Counts from data files:
  - `backend/src/main/resources/data/recipes.json` (500)
  - `backend/src/main/resources/data/ingredients.json` (1342)

22. RAG includes embeddings + relevance search.
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/ai/vector/InMemoryVectorStore.java`
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/ai/embedding/RecipeEmbeddingService.java`

23. Vector similarity used for retrieval.
- ✅ recipe + ingredient vector similarity search
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/vector/InMemoryVectorStore.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeRetrievalService.java`
  - Defense notes (explainable flow):
    - Embeddings exist on recipe/ingredient entities (DB column `embedding` real[]):
      - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Recipe.java` (~94–96)
      - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Ingredient.java` (~55–56)
    - Query → embedding → similarity search:
      - Query text built and embedded in `RecipeRetrievalService.retrieve(...)` (~59–65).
      - Vector similarity search executed in `InMemoryVectorStore.search(...)` (~36–55).
      - Top‑N recipe IDs are mapped back to DB records in `RecipeRetrievalService` (~70–90).
    - Why this satisfies “vector similarity”:
      - `InMemoryVectorStore` uses cosine similarity / dot product on normalized vectors.
      - Retrieval is relevance‑sorted by similarity score.

24. Response generated from augmented prompt.
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeGenerationService.java`
  - Augmented prompt is built in `buildAugmentedPrompt(...)` with prior-step context + retrieval results.
  - Location: `RecipeGenerationService.java` ~131–221.

25. Clearly defined functions for nutrition; all nutrition via function calling.
- ✅ Nutrition is calculated via function-calling contract and database-backed calculator
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/NutritionCalculator.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/DatabaseNutritionCalculator.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/FunctionCallingOrchestrator.java`
  - Logic: AI calls nutrition function with structured ingredients; server computes totals from DB.
  - Exact usage (function calling path):
    - Function schema defined: `FunctionCallingOrchestrator.defineCalculateNutritionFunction()` (`FunctionCallingOrchestrator.java` ~60–95).
    - AI call uses function schema: `RecipeGenerationService.generate(...)` (`RecipeGenerationService.java` ~99–110).
    - Function call execution + DB calc: `FunctionCallingOrchestrator.executeCalculateNutrition(...)` (`FunctionCallingOrchestrator.java` ~120–170).
    - Final nutrition embedded only after function result: `RecipeGenerationService.handleFunctionCall(...)` (`RecipeGenerationService.java` ~233–256).

26. Function calling error handling (parsing, missing params, invalid values, execution errors, timeout, rate limits, connectivity).
- ✅ Error handling covers validation + HTTP failures + timeouts/connectivity
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/FunctionCallingOrchestrator.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/GroqClient.java`
  - Logic: schema validation + guarded parsing + explicit exception mapping to user-safe messages.
  - Full paths:
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/FunctionCallingOrchestrator.java`
    - `/Users/martinmust/Coding/kood_johvi/github_ndl/backend/src/main/java/com/ndl/numbers_dont_lie/ai/GroqClient.java`
  - Defense notes (where each error type is handled):
    - Parsing errors (invalid JSON):
      - `GroqClient.callForJson(...)` JSON parse with explicit exception mapping. (`GroqClient.java` ~120–125)
      - `GroqClient.callWithFunctionResult(...)` parse after function call. (`GroqClient.java` ~200–205)
    - Missing parameters / invalid values:
      - Function call argument validation in `executeCalculateNutrition(...)` (missing `arguments`, `ingredients`, `servings`, invalid values). (`FunctionCallingOrchestrator.java` ~134–179)
      - Additional input validation via `CalculateNutritionRequest.Input.validate()` (`FunctionCallingOrchestrator.java` ~185–187)
    - Execution errors:
      - Wrapped calculation errors are surfaced as `RuntimeException` with message. (`FunctionCallingOrchestrator.java` ~231–233)
    - Timeout errors:
      - `GroqClient` catches `HttpTimeoutException` and maps to `AiClientException`. (`GroqClient.java` ~126–129, ~206–209)
    - Rate limits:
      - HTTP 429 handled in `handleHttpErrors(...)`. (`GroqClient.java` ~223–225)
    - Connectivity issues:
      - `ConnectException` mapped to `AiClientException`. (`GroqClient.java` ~129–132, ~209–212)

27. Recipes searchable by name, ingredients, or cuisine.
- ✅ `frontend/src/pages/RecipesPage.jsx`

28. Recipe filters include dietary restrictions, allergies, ingredients, calorie & macro, prep time.
- ✅ Filters added for dietary tags, allergy exclusions, ingredient text, calorie/macro ranges, prep time
  - `frontend/src/pages/RecipesPage.jsx`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/controller/RecipeController.java`

29. Recipe details show ingredients, steps, nutritional info from ingredients; nutrition visualized.
- ✅ Nutrition summary + chart added based on ingredient data
  - `frontend/src/pages/RecipePage.jsx`
  - `frontend/src/styles/RecipePage.css`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/controller/RecipeController.java`

30. AI generates variety of custom recipes based on preferences.
- ✅ Prompt enforces distinct/unique recipe output with nutrition + steps
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiRecipeMvpService.java`

31. AI ingredient substitution.
- ✅ Backend + UI
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiIngredientSubstitutionService.java`
  - `frontend/src/pages/RecipePage.jsx`

32. Substitution based on availability + preferences.
- ✅ Availability input supported in substitute flow
  - `frontend/src/pages/RecipePage.jsx`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/controller/AiIngredientSubstitutionController.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiIngredientSubstitutionService.java`

33. Portion adjustment (serving size) + auto recalculation using function calling.
- ✅ Serving size adjustment with ingredient scaling and nutrition recalculation
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/controller/RecipeController.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/dto/RecipeServingsRequest.java`
  - `frontend/src/pages/RecipePage.jsx`
  - `frontend/src/styles/RecipePage.css`
  - Logic: ingredient quantities scale by serving multiplier; nutrition recalculated via function calling.

34. Standard units (g, ml, kcal, minutes).
- ✅ Units standardized: grams/milliliters for ingredients, kcal in nutrition, time in minutes
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Ingredient.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Nutrition.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Recipe.java`
  - Time in minutes (exact field):
    - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Recipe.java` ~65–67 (`time_minutes` / `timeMinutes`)

35. Recipe data structure has required fields.
- ✅ `backend/src/main/resources/data/recipes.json`
  - keys: id, title, cuisine, meal, servings, ingredients(id,name,quantity), summary, time, difficulty_level, dietary_tags, source, img, preparation(step, description, ingredients)
  - Entity mapping (where fields live in code):
    - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Recipe.java` (id, title, cuisine, meal, servings, summary, time, difficulty_level, dietary_tags, source, img)
    - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/RecipeIngredient.java` (ingredient id/name/quantity/unit)
    - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/PreparationStep.java` (step, description, ingredients)

36. Ingredient data structure has required fields.
- ✅ `backend/src/main/resources/data/ingredients.json`
  - keys: id, label, unit, quantity, nutrition(calories, carbs, protein, fats)
  - Entity mapping:
    - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Ingredient.java`
    - Nutrition fields live in embedded `Nutrition`:
      - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Ingredient.java` ~44–46 (`@Embedded private Nutrition nutrition`)
      - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Nutrition.java` ~16–20 (`calories`, `protein`, `carbs`, `fats`)

37. Nutritional analysis includes kcal + macros per meal/day.
- ✅ Daily nutrition summary with % targets + per-meal macro breakdowns
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/styles/MealPlan.css`
  - Примечание: «макросы» = белки/углеводы/жиры (в граммах) и их % от дневной цели. Визуально показываются прогресс-барами в блоке Daily Nutrition Summary.

38. Macro breakdown visualized (graph/chart).
- ✅ Progress bars
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - Визуализация макросов: полосы (progress bars) для Protein/Carbs/Fats с процентами.

39. Nutrition tracking compares to goals; daily + weekly deficit/surplus.
- ✅ Daily calorie progress bar indicates surplus/deficit; weekly delta via trend endpoint
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/styles/MealPlan.css`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java` (week trends)
  - Примечание (как показать): на странице Today Meal Plan в Daily Nutrition Summary видно сравнение с целью (progress‑bar и %), а на Weekly Meal Plan в Weekly Calorie Balance показывается ежедневный дефицит/профицит за неделю.

40. Progress toward calorie targets visualized + color-coded.
- ✅ `frontend/src/pages/MealPlanPage.jsx` and `frontend/src/pages/WeeklyMealPlanPage.jsx`

41. Trend lines for daily caloric deficit/surplus weekly + monthly.
- ✅ Weekly bars + monthly trend line (4-week aggregation)
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - `frontend/src/styles/WeeklyMealPlan.css`
  - Logic: weekly trend uses `/meal-plans/week/trends`; monthly view aggregates 4 consecutive weeks.

42. AI nutrition summaries (achievements, concerns, macro balance).
- ✅ Daily + weekly AI summaries implemented and displayed
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiNutritionSummaryService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiWeeklyNutritionInsightsService.java`
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`

43. AI improvement suggestions (food recommendations, timing, portions, alternatives, plan optimizations).
- ✅ Prompt enforces all required suggestion types + UI display
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiNutritionSuggestionsService.java`
  - `frontend/src/pages/MealPlanPage.jsx`

44. Nutrition analysis integrated (dashboard + progress charts + grouped insights).
- ✅ Daily tracking added to Dashboard; Progress Charts page added with historical trends and AI insights
  - `frontend/src/pages/Dashboard.jsx`
  - `frontend/src/styles/Dashboard.css`
  - `frontend/src/pages/ProgressChartsPage.jsx`
  - `frontend/src/styles/ProgressCharts.css`
  - `frontend/src/App.jsx`
  - Logic: daily metrics render on Dashboard; historical charts + AI insights grouped in Progress Charts.

45. Meal planner uses relevant user data (BMI/weight goals/activity) and updates wellness score.
- ✅ Dashboard shows personalization inputs + estimated targets + wellness score
  - `frontend/src/pages/Dashboard.jsx`

46. Model choice rationale + few-shot examples + parameter explanation (temperature/top‑p).
- ✅ Documented model choices, parameter settings, and few-shot strategy
  - `README.md`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/GroqClient.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiNutritionSummaryService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiNutritionSuggestionsService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/service/AiRecipeMvpService.java`

47. API errors handled gracefully with user-friendly messages.
- ✅ AI blocks show friendly errors on failure
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - `frontend/src/pages/ProgressChartsPage.jsx`
  - UI proof: “AI service unavailable. Please try again later.”

48. Rate limits, timeouts, malformed responses handled with meaningful user feedback.
- ✅ Rate-limit and AI unavailability messages shown; backend provides explicit error reasons
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/GroqClient.java`
  - UI proof: “AI rate limit reached. Try again later.”
  - Backend proof: rate-limit/timeout/connectivity exceptions surfaced with reason

49. Recovery mechanism for failed AI requests (caching, retry, alternative models).
- ✅ Recovery fallback via AI session cache + safe UI fallbacks when AI fails
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/cache/AiSessionCache.java`
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - Behavior: repeat AI requests can return cached summary; if not available, UI displays fallback text and does not block page
  - Logic: server caches last successful AI responses per user/date; UI degrades gracefully on failures.

50. Content versioning & restore previous meal plans.
- ✅ Version history + restore endpoints + UI access on weekly plan
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/service/MealPlanVersionService.java`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - `frontend/src/styles/WeeklyMealPlan.css`
  - UI proof: Weekly Meal Plan → “Plan Versions” → Restore button for any earlier version
  - Logic: each weekly regeneration creates a new version snapshot; switching versions changes active version id without creating a new one.

## Test Runs (Automated)

Backend
- `mvn test` (run in `backend/`) → FAILED
  - Failure: H2 does not support `real[]` column for embeddings during schema init.
  - Error: syntax error on `embedding real[]` and FK creation fails because table creation aborted.
  - Location: ingredient/recipe embedding fields in DB schema (H2 incompatibility).

Frontend
- `npm run lint` → FAILED
  - Errors: unused vars (`err`), undefined `dailyNutrition`, fast-refresh export rule in `UserContext.jsx`.
  - Warnings: missing hook deps in several files.
- `npm run build` → PASSED
  - Warnings: large chunk size and mixed dynamic/static import for `src/lib/api.js`.
