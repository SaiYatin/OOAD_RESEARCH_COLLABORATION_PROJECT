# Research Collaboration & Expert Recommendation System

**Course:** UE23CS352B – Object Oriented Analysis & Design  
**Team:** 4 Members | **Language:** Java 17 | **Framework:** Spring Boot 3.2 + JavaFX 21  
**Database:** MySQL | **Automation:** n8n workflows

---

## Project Overview

A platform for research scholars to:
- Search research papers by keyword, domain, status
- Get AI-powered expert recommendations (PES University professors)
- Send and manage collaboration requests
- Follow research keywords and receive email notifications via n8n

---

## Architecture: MVC Pattern (Spring-enforced)

```
┌─────────────────────────────────────────────┐
│             JavaFX Views (V)                │
│  LoginView · DashboardView · PaperSearch   │
│  ExpertSearch · CollaborationView           │
└────────────────────┬────────────────────────┘
                     │ calls
┌────────────────────▼────────────────────────┐
│           Spring Services (C+M)             │
│  AuthService · ExpertService                │
│  CollaborationService · RecommendationService│
└────────────────────┬────────────────────────┘
                     │ uses
┌────────────────────▼────────────────────────┐
│     JPA Repositories + MySQL (M)            │
│  UserRepository · ExpertRepository          │
│  PaperRepository · ProjectRepository        │
└─────────────────────────────────────────────┘
```

---

## Design Patterns Used

### Creational Patterns

| Pattern | Where Applied | How |
|---------|--------------|-----|
| **Singleton** | `DatabaseConnectionManager` | Single instance of DB connection handle via double-checked locking |
| **Factory Method** | `UserFactory` | Creates correct User subclass (Researcher/Admin/Reviewer etc.) based on role string — controller doesn't know class hierarchy |

### Structural Patterns

| Pattern | Where Applied | How |
|---------|--------------|-----|
| **Facade** | `RecommendationFacade` | Hides keyword extraction → expert scoring → ranking pipeline behind a single `recommend(query)` call |
| **Decorator** | `PaperSearchDecorator` chain | `BasicPaperSearch` wrapped by `PublishedOnlyDecorator` and `DomainFilterDecorator` — filters composable at runtime |

### Behavioral Patterns

| Pattern | Where Applied | How |
|---------|--------------|-----|
| **Observer** | `PaperPublicationSubject` + `EmailNotificationObserver` | When paper is published, all subscribers following matched keywords get notified via n8n webhook |
| **Strategy** | `RecommendationContext` + 3 strategies | UI lets user switch between KeywordMatching / AI-Powered (n8n Gemini) / Hybrid at runtime |

---

## Design Principles (SOLID)

| Principle | Where Applied |
|-----------|--------------|
| **SRP** | Each service handles exactly one domain (AuthService=auth, ExpertService=profiles, etc.) |
| **OCP** | New recommendation strategies added without changing existing code — just add new Strategy impl |
| **LSP** | All User subclasses (Researcher, Admin, Reviewer etc.) substitutable for User base class |
| **DIP** | Services depend on Repository interfaces, not concrete classes |

---

## Team Split

| Member | Primary Use Case | Secondary |
|--------|-----------------|-----------|
| Member 1 | User Auth (Register/Login/Roles) | Paper Search UI + Decorator filters |
| Member 2 | Expert Profile Management | CSV import from n8n scraper |
| Member 3 | Collaboration Requests (send/accept/reject) | Keyword Follow + Observer notifications |
| Member 4 | Expert Recommendation Engine | Strategy selection + n8n AI integration |

---

## n8n Workflows

### Workflow 1: University Research Agent
Scrapes PES staff page → extracts professor data → saves to Google Sheets  
**File:** `University_Research_Agent.json`

### Workflow 2: Search Agent
Chat trigger → reads from Google Sheets → AI Agent (Gemini) answers expert queries  
**File:** `Search_Agent.json`

### Workflow 3: Email Notification (NEW)
Webhook trigger from Java app → builds HTML email → sends via Gmail SMTP  
**File:** `Email_Notification_Workflow.json`  
**Trigger URL:** `https://<your-ngrok-domain>/webhook/research-update`

---

## Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- n8n running (Docker + ngrok per n8n-setup-main)

### 1. Database Setup
```sql
CREATE DATABASE research_collab;
CREATE USER 'research_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON research_collab.* TO 'research_user'@'localhost';
```

### 2. Configure application.properties
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/research_collab
spring.datasource.username=research_user
spring.datasource.password=your_password
spring.mail.username=your_gmail@gmail.com
spring.mail.password=your_gmail_app_password
n8n.webhook.url=https://your-ngrok-domain/webhook/research-update
```

### 3. Build and Run
```bash
mvn clean install
mvn javafx:run
```

### 4. Import n8n Workflows
1. Open n8n at `http://localhost:5678`
2. Go to **Workflows → Import**
3. Import all 3 JSON files
4. Set up credentials (Google Sheets OAuth, Gmail SMTP)
5. Activate Workflow 3 (Email Notification)

### 5. Expert Data Seeding
On first run, the app auto-imports `src/main/resources/data/experts.csv`  
(the PES professor data scraped by n8n Workflow 1) into the MySQL `experts` table.

---

## Project Structure

```
research-collaboration/
├── pom.xml
├── Email_Notification_Workflow.json      ← n8n Workflow 3
├── src/main/java/com/research/
│   ├── ResearchCollaborationApp.java     ← Entry point
│   ├── config/
│   │   ├── SecurityConfig.java           ← Spring Security (BCrypt)
│   │   └── AppConfig.java                ← CSV seeding on startup
│   ├── model/
│   │   ├── User.java                     ← Abstract base (Class Diagram)
│   │   ├── UserSubclasses.java           ← Admin, Reviewer, Visitor, Collaborator
│   │   ├── Researcher.java               ← Researcher subclass
│   │   ├── Expert.java                   ← PES professor entity
│   │   ├── ResearchPaper.java            ← Paper entity
│   │   └── ResearchProject.java          ← Project + CollaborationRequest
│   ├── pattern/
│   │   ├── CreationalPatterns.java       ← Singleton + Factory
│   │   ├── StructuralPatterns.java       ← Facade + Decorator
│   │   └── BehavioralPatterns.java       ← Observer + Strategy
│   ├── repository/
│   │   └── Repositories.java             ← All JPA repos
│   ├── service/
│   │   ├── AuthService.java              ← Member 1
│   │   ├── ExpertService.java            ← Member 2
│   │   ├── CollaborationService.java     ← Member 3
│   │   └── RecommendationService.java    ← Member 4
│   └── view/
│       ├── auth/LoginView.java           ← Login + Register screen
│       ├── dashboard/DashboardView.java  ← Navigation shell
│       ├── paper/PaperSearchView.java    ← Paper search + Decorator UI
│       ├── expert/ExpertSearchView.java  ← Expert recommendation UI
│       └── collab/CollaborationView.java ← Requests + keyword follow
└── src/main/resources/
    ├── application.properties
    └── data/experts.csv                  ← n8n scraped PES professors
```
