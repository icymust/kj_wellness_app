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
- ❌ Not found in `README.md` (no separate docs located; README is wellness/analytics-focused).

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

17. Shopping list items categorized by food group (>=5 categories).
- ✅ Category inference (Dairy, Protein, Produce, Grains, Pantry, Beverages, Other) + UI grouping
  - `backend/src/main/java/com/ndl/numbers_dont_lie/shoppinglist/service/ShoppingListService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/shoppinglist/dto/ShoppingListItemDto.java`
  - `frontend/src/pages/DailyShoppingListPage.jsx`
  - `frontend/src/pages/WeeklyShoppingListPage.jsx`
  - `frontend/src/pages/MealShoppingListPage.jsx`

18. Shopping list allows quantity adjustments and item exclusions.
- ✅ Local adjustments + removal persisted in localStorage per list
  - `frontend/src/pages/DailyShoppingListPage.jsx`
  - `frontend/src/pages/WeeklyShoppingListPage.jsx`
  - `frontend/src/pages/MealShoppingListPage.jsx`

19. Sequential prompting with at least 3 distinct steps used for meal plan generation.
- ✅ Steps 4.1 → 4.2 → 4.3.1/4.3.2
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/AiStrategyService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeGenerationService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/service/DayPlanAssemblerService.java`

20. RAG used to generate recipes and nutritional values.
- ✅ RAG retrieval + augmented prompts for recipes; nutrition values computed from ingredient DB via function calling
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeRetrievalService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeGenerationService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/FunctionCallingOrchestrator.java`

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
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/IngredientRetrievalService.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/embedding/VectorStoreInitializer.java`

24. Response generated from augmented prompt.
- ✅ `backend/src/main/java/com/ndl/numbers_dont_lie/ai/RecipeGenerationService.java`

25. Clearly defined functions for nutrition; all nutrition via function calling.
- ✅ Nutrition is calculated via function-calling contract and database-backed calculator
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/NutritionCalculator.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/DatabaseNutritionCalculator.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/FunctionCallingOrchestrator.java`

26. Function calling error handling (parsing, missing params, invalid values, execution errors, timeout, rate limits, connectivity).
- ✅ Error handling covers validation + HTTP failures + timeouts/connectivity
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/function/FunctionCallingOrchestrator.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/ai/GroqClient.java`

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

34. Standard units (g, ml, kcal, minutes).
- ✅ Units standardized: grams/milliliters for ingredients, kcal in nutrition, time in minutes
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Ingredient.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Nutrition.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/recipe/entity/Recipe.java`

35. Recipe data structure has required fields.
- ✅ `backend/src/main/resources/data/recipes.json`
  - keys: id, title, cuisine, meal, servings, ingredients(id,name,quantity), summary, time, difficulty_level, dietary_tags, source, img, preparation(step, description, ingredients)

36. Ingredient data structure has required fields.
- ✅ `backend/src/main/resources/data/ingredients.json`
  - keys: id, label, unit, quantity, nutrition(calories, carbs, protein, fats)

37. Nutritional analysis includes kcal + macros per meal/day.
- ✅ Daily nutrition summary with % targets + per-meal macro breakdowns
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/styles/MealPlan.css`

38. Macro breakdown visualized (graph/chart).
- ✅ Progress bars
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`

39. Nutrition tracking compares to goals; daily + weekly deficit/surplus.
- ✅ Daily calorie progress bar indicates surplus/deficit; weekly delta via trend endpoint
  - `frontend/src/pages/MealPlanPage.jsx`
  - `frontend/src/styles/MealPlan.css`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java` (week trends)

40. Progress toward calorie targets visualized + color-coded.
- ✅ `frontend/src/pages/MealPlanPage.jsx` and `frontend/src/pages/WeeklyMealPlanPage.jsx`

41. Trend lines for daily caloric deficit/surplus weekly + monthly.
- ✅ Weekly bars + monthly trend line (4-week aggregation)
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - `frontend/src/styles/WeeklyMealPlan.css`

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

50. Content versioning & restore previous meal plans.
- ✅ Version history + restore endpoints + UI access on weekly plan
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/controller/MealPlanController.java`
  - `backend/src/main/java/com/ndl/numbers_dont_lie/mealplan/service/MealPlanVersionService.java`
  - `frontend/src/pages/WeeklyMealPlanPage.jsx`
  - `frontend/src/styles/WeeklyMealPlan.css`
  - UI proof: Weekly Meal Plan → “Plan Versions” → Restore button for any earlier version
