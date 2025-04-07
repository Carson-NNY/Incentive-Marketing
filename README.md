---

# Incentive Marketing Platform

A Spring Boot-based microservice application designed to manage and operate promotional activities, including raffles, credit systems, award logic, and strategy-based decision making.

## 🌟 Overview

This system provides a scalable backend infrastructure to support marketing events such as lucky draws, behavior-based rebates, and credit incentives. It uses a modular design to enable the integration of rules, strategy trees, and decision chains for dynamic reward distribution.

---

## 📐 Project Structure & Design

### 1. **Modules**
- **`big-market-app`**: Core application logic and entry point.
- **`big-market-domain`**: Contains domain-level logic like rule trees, raffle strategies, and logic chains.
- **`big-market-api`**: Defines API contracts via DTOs and interfaces for services.

### 2. **Key Concepts**
- **Decision Tree Engine** (`DecisionTreeEngine.java`): Evaluates reward rules using a tree of logic nodes.
- **Logic Chains** (`ILogicChain.java`, `DefaultLogicChain.java`): Apply a chain of responsibility to evaluate eligibility.
- **Strategy Patterns**: Different strategies for handling raffles and user participation.
- **Rule Nodes & Weights**: Customize probability and restrictions using rule nodes and weight configurations.

---

## 🔁 Workflow

1. **User Initiates Participation**
   - Frontend calls the raffle API with user and activity details.

2. **Activity & Strategy Validation**
   - The service checks quota, time window, and user eligibility using logic chains.

3. **Decision Engine Execution**
   - Rule tree evaluates all defined conditions using nodes (e.g., blacklist, luck-based).

4. **Strategy Execution**
   - A raffle strategy is selected and executed, using weighted probability or deterministic logic.

5. **Award Assignment**
   - Based on the strategy outcome, awards are assigned and recorded.

6. **Credit or Rebate Updates**
   - Credits or rebates are updated in the user account via DAO interactions.

---

## 🧪 Testing

Tests are available under:
```
big-market-app/src/test/java/cn/bugstack/test/
```
Covering:
- API Controller tests
- DAO layer integration
- Domain logic (Strategy, Logic Trees, etc.)

---

## ⚙️ Setup & Build

### Prerequisites
- Java 11+
- Maven
- Redis (for caching)
- MySQL (for persistence)

### Build and Run
```bash
cd big-market-app
sh build.sh
java -jar target/big-market-app.jar
```

### Docker (Optional)
```bash
docker build -t incentive-marketing-app .
docker run -p 8080:8080 incentive-marketing-app
```

---

## 📁 Configuration

Environment-specific YAML configs are in:
```
big-market-app/src/main/resources/
```
Edit `application-dev.yml` to configure your database, Redis, and thread pool settings.

---

## 🏗️ Technologies Used

- Spring Boot
- MyBatis
- Redis
- Docker
- Maven
- JUnit


---
