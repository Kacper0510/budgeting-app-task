# Budgeting App

A simple budgeting application built with Java Spring Boot for the backend and React with TypeScript for the frontend. The application allows users to track their income and expenses, categorize them, and view summaries of their financial data. Created as a SoftNet recruitment task.

## Tech Stack

- **Backend**: Java 25, Maven, Spring Boot 4, Spring Data JPA, OpenAPI
- **Database**: PostgreSQL 18 in production, H2 in-memory for testing
- **Frontend**: React, TypeScript, Vite, TailwindCSS

## Building and Running

### Using Docker

1. Install [Docker](https://www.docker.com/get-started) if you haven't already.
2. Clone the repository and navigate to the project directory:
   ```shell
   git clone https://github.com/Kacper0510/budgeting-app-task.git
   cd budgeting-app-task
   ```
3. Build and run the application using Docker Compose:
   ```shell
   docker compose up --build
   ```
4. Go to `http://localhost:8080` in your web browser to access the application.

### Running Locally

1. Ensure you have all the dependencies installed - take a look at `shell.nix` if unsure.
2. Start the PostgreSQL database as a container or install it locally and create a database named `budgeting` with username and password both set to `budgeting`.
3. Navigate to the backend directory and run the Spring Boot application:
   ```shell
   cd backend
   mvn spring-boot:run
   ```
4. In another terminal, navigate to the frontend directory and start the React application:
   ```shell
   cd frontend
   pnpm install
   pnpm run dev
   ```
5. Go to `http://localhost:5173` in your web browser to access the frontend, which will communicate with the backend running on `http://localhost:8080`.

### Additional Notes

- The OpenAPI documentation for the backend is available at `http://localhost:8080/swagger-ui/index.html` when the application is running.
- You may access the PostgreSQL database directly on `localhost:5432` with the credentials provided above for development and testing purposes.
- To run the tests for the backend (and also generate coverage report), navigate to the backend directory and execute:
  ```shell
  mvn test
  ```
