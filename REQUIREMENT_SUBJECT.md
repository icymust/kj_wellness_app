ai-assistant
The Situation 👀
Your wellness platform has evolved into a comprehensive health ecosystem, featuring health analytics and nutrition planning. Now it's time to make these valuable features more accessible through a conversational AI assistant.
In this project, you'll design and implement an AI assistant that helps users interact with your platform using natural language. Your assistant will enable users to access health metrics, nutrition information, and receive personalized wellness guidance through simple conversations that feel intuitive and helpful.
Functional Requirements 📋
Users should be able to ask questions about their health data and receive clear, helpful responses that feel natural and personalized. The assistant should understand follow-up questions, remember context from earlier in the conversation, and provide information in an easily digestible format.
Integration with Existing Features
Health Profile:
Access current health metrics (weight, BMI, wellness score)
Retrieve health goals and preferences
Summarize progress data and activity history
Provide personalized health insights based on user's data
Nutrition Management:
Access current meal plans and nutritional intake
Retrieve recipe information and preparation steps
Provide nutritional analysis and recommendations
Answer questions about dietary goals and restrictions
Data Visualization:
Describe trends from health and nutrition charts
Highlight key insights from user's data
Compare current metrics to targets and historical data
Conversation Capabilities
Your AI assistant must support diverse conversation types and maintain conversational context. You must provide few-shot examples to the following conversation types:
Core Conversation Types (with example questions)
Health metrics queries
"What's my current BMI?
"How has my weight changed this month?"
Progress questions
"How close am I to my weight goal?"
"Am I making progress with my fitness level?"
Meal plan inquiries
"What's on my meal plan today?"
"What's for lunch tomorrow?"
Recipe information
"Tell me about my dinner recipe"
"How do I prepare tonight's meal?"
Nutritional analysis
"How many calories have I consumed today?"
"Am I meeting my protein target?"
General wellness questions
"How can I improve my sleep?"
"What stretches help with lower back pain?"
Multi-turn Conversation Management
Follow-up questions
"Can you tell me more about that?"
"Why is that important?"
Context-aware responses (remembering topics from previous messages)
Reference resolution (understanding "it", "that", etc. based on conversation history)
Response Requirements
Personalization:
Include user's name and preferences in responses
Reference specific goals and metrics relevant to the user
Structure and Formatting:
Present information in clear, scannable formats
Use appropriate lists, sections, and emphasis for readability
Maintain consistent response patterns for similar queries
Format numerical data and statistics appropriately
Safety and Boundaries:
Clearly indicate AI limitations regarding medical advice
Provide appropriate responses to sensitive health topics
Implement guardrails for questions outside the assistant's scope
Suggest professional consultation for medical concerns
Implementation Architecture
Your AI assistant implementation should follow this two-layer architecture that creates an efficient and user-friendly conversational experience:
Conversation Layer
The Conversation Layer handles direct user interactions and maintains the conversation flow.
Key Components:
Chat interface with message history
Input handling and validation
Response rendering and formatting
Session state tracking
Conversation history management
Context maintenance (recent messages, user preferences)
Response generation
Data Access Layer
The Data Access Layer connects to your platform's features and processes data for the conversation.
Key Components:
Health data access functions
Nutrition data access functions
User profile integration
Function calling implementation
Data formatting (for AI consumption)
Error handling for data retrieval
Example Request flow:
User sends message (Conversation Layer)
"What's my current BMI?"
Message is added to conversation context (Conversation Layer)
System appends message to conversation history array
AI determines if data is needed (Conversation Layer)
AI recognizes "BMI" as health metric requiring data retrieval
If needed, data is fetched from platform (Data Access Layer)
get_health_metrics("bmi", "current") function returns BMI=24.2
Response is generated using available data (Conversation Layer)
AI formats response including user's BMI value and classification
Response is added to context (Conversation Layer)
System stores that "BMI" was last discussed topic
Response is displayed to user (Conversation Layer)
"Your current BMI is 24.2, which falls in the normal range."
GenAI Techniques
Your implementation must showcase these essential AI assistant techniques:
System Prompt Design
The system prompt is your assistant's foundation - it defines capabilities, personality, and constraints. Your implementation should demonstrate:
Core Components of Effective System Prompts:
Clear definition of assistant's purpose and capabilities
Personality and tone guidelines
Domain-specific knowledge (health and nutrition terminology)
Ethical boundaries and limitations
Response formatting requirements
Implementation Requirements:
Create a structured system prompt that defines your assistant's role
Include examples of desired response formats for different query types
Define guardrails for handling out-of-scope or sensitive requests
Establish rules for personalization and context maintenance
Example system prompt:
------------------------------
You are a wellness assistant helping users with health analytics and nutrition planning.

## Your capabilities include:
- Answering questions about health metrics
- Providing information about meal plans
- Offering general wellness guidance

## Tone and personality:
- Friendly and encouraging
- Clear and straightforward
- Empathetic but not overly casual

## Response formatting:
- Short, focused paragraphs
- Bullet points for lists
- Bold for key metrics
...etc
Feature Access (Function Calling)
Function calling enables your assistant to securely access user data and platform features. Your implementation must demonstrate:
Function Definition and Implementation:
Define functions for accessing health metrics, nutrition data, and other platform features
Implement proper parameter validation and error handling
Create functions with appropriate scopes and granularity
Integration Requirements:
Implement at least 4 functions covering health metrics, nutrition data, and general platform features
Design functions with clear parameters and return types
Create error handling for when data is unavailable or incomplete
Implement security checks to verify data access permissions
Example function definition:
{
  "name": "get_health_metrics",
  "description": "Retrieves user's current health metrics",
  "parameters": {
    "type": "object",
    "properties": {
      "metric_type": {
        "type": "string",
        "enum": ["bmi", "weight", "wellness_score", "all"],
        "description": "The specific metric to retrieve"
      },
      "time_period": {
        "type": "string",
        "enum": ["current", "weekly", "monthly"],
        "description": "Time period for the metrics"
      }
    },
    "required": ["metric_type"]
  }
}
Conversation Memory and Context Management
Effective assistants maintain context across conversation turns. Your implementation must demonstrate:
Conversation History Management:
Design a system to store and retrieve conversation history
Implement context windowing to manage token limitations
Create mechanisms to identify and track important information
Implementation Requirements:
Support multi-turn conversations with at least 5 turns of context
Create a system to extract and track key user information
Demonstrate appropriate reference resolution (e.g., pronouns, previous topics)
Response formatting and Enhancement
Your assistant should provide consistent, well-structured responses. Your implementation must demonstrate:
Output Structuring Techniques:
Design templates for common response types
Implement data formatting for numerical information
All outputs must use the same standardized units as in previous projects
Implementation Requirements:
Define response structures for at least 4 query categories
Implement formatting systems for different data types
Create natural language descriptions of visual data
Support both concise and detailed response modes
Important Considerations ❗
Model Selection and Configuration: Choose appropriate models based on their capabilities for your assistant implementation. Balance response quality against performance needs to ensure efficient operation. Configure parameters such as temperature and top-p to achieve optimal responses that match your application's requirements.
Token Management: Design prompts and functions to minimize token usage throughout your implementation. Create efficient memory management systems for conversation history to reduce token consumption. Monitor and optimize token usage for cost efficiency, especially during long conversations or complex interactions.
Error Handling: Implement graceful fallbacks when services fail to maintain conversation flow. Provide clear user feedback when system limitations prevent fulfilling requests. Design recovery strategies for conversation derailment to get interactions back on track. Create robust validation for AI outputs to ensure response quality and accuracy.
Conversation Boundaries: Clearly communicate what the assistant can and cannot do to set appropriate user expectations. Implement appropriate redirects when users make out-of-scope requests. Create educational responses for boundary-crossing questions that explain limitations while providing alternative solutions. Design escalation paths for complex or sensitive topics that require additional expertise.
Finetuning the LLMs: Don't lose yourself in endless fine-tuning. While researching and implementing additional features is exciting, remember the core functionality of your AI-assistant: focus on direct data access and simple follow-up questions. Save J.A.R.V.I.S. for another day.
Useful links 🔗
System prompt engineering best practices
Token optimization strategies
Building AI Assistants With OpenAI's Assistant API
HIPAA Compliance Guide for Healthcare Chatbots
The Ethical Considerations of AI-Powered Chatbots in Healthcare
Docker