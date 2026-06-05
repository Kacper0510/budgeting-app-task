FROM node:24-alpine AS frontend

WORKDIR /app
RUN npm install -g pnpm
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY \
    frontend/eslint.config.js \
    frontend/index.html \
    frontend/tsconfig.app.json \
    frontend/tsconfig.json \
    frontend/tsconfig.node.json \
    frontend/vite.config.ts \
    ./
COPY frontend/src ./src
RUN pnpm run build

FROM maven:3.9-eclipse-temurin-25-alpine AS backend

WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
COPY --from=frontend /app/dist ./static
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
