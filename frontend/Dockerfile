FROM node:20-alpine
WORKDIR /app

# Установка зависимостей
COPY package.json package-lock.json* ./
RUN npm ci

# Копируем исходники
COPY . .

# Vite dev-сервер на 0.0.0.0, чтобы пробрасывался порт
EXPOSE 8080
ENV VITE_API_BASE=http://localhost:5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "8080"]
