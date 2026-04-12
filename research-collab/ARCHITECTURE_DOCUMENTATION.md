# 🏛️ Research Collaboration Platform — Master Architectural Documentation

> **Version:** 1.0.0 | **Project:** PES Research Collaboration & Expert Recommendation System  
> **Authors:** Shivakumar et al. | **Stack:** Java 17, Spring Boot, JavaFX, Google Cloud BigQuery, ADK Cloud Run  
> **Purpose:** Complete technical reference for all teammates and contributors.

---

## 📋 Table of Contents

1. [System Overview](#1-system-overview)
2. [Full Directory Tree](#2-full-directory-tree)
3. [Expert Search Architecture — Three Modes](#3-expert-search-architecture--three-modes)
   - [Mode 1: Keyword Search](#mode-1-keyword-search)
   - [Mode 2: Semantic Search (BigQuery Embedding)](#mode-2-semantic-search-bigquery-embedding)
   - [Mode 3: Mentor Scout AI Agent](#mode-3-mentor-scout-ai-agent)
4. [Database & Dataset Setup](#4-database--dataset-setup)
5. [Google Cloud BigQuery — Vector Search Deep Dive](#5-google-cloud-bigquery--vector-search-deep-dive)
6. [Mentor Scout AI Agent — 2-Step ADK Protocol](#6-mentor-scout-ai-agent--2-step-adk-protocol)
7. [Design Patterns Used](#7-design-patterns-used)
8. [SOLID & Other Design Principles](#8-solid--other-design-principles)
9. [UI Architecture — Expert Card & Profile Dialog](#9-ui-architecture--expert-card--profile-dialog)
10. [Expert Profile Retrieval — Hybrid Persistence Mechanism](#10-expert-profile-retrieval--hybrid-persistence-mechanism)
11. [Authentication & Google Cloud Security](#11-authentication--google-cloud-security)
12. [Email Notification Workflow (Observer + n8n)](#12-email-notification-workflow-observer--n8n)
13. [Application Properties Configuration Reference](#13-application-properties-configuration-reference)
14. [Thread Safety & Concurrency Model](#14-thread-safety--concurrency-model)
15. [Error Handling & Observability](#15-error-handling--observability)

---

## 1. System Overview

The **PES Research Collaboration Platform** is an enterprise-grade JavaFX + Spring Boot desktop application that enables researchers to:

- 🔍 **Discover Experts** using three intelligent search strategies
- 🤝 **Request Collaboration** with internal researchers
- 📄 **Publish & Review** research papers
- 📨 **Receive Notifications** on new publications via email automation

The application uniquely integrates **three tiers of AI intelligence** into a single search bar:

| Mode | Technology | Intelligence Level |
|------|-----------|-------------------|
| Keyword Match | Java Stream filtering on local MySQL data | ⭐ Fast, deterministic |
| Semantic Match | Google Cloud BigQuery + Vertex AI Embedding Model | ⭐⭐⭐ Contextual AI understanding |
| Mentor Scout | Google ADK Cloud Run AI Agent (Natural Language) | ⭐⭐⭐⭐⭐ Full conversational AI |

---

## 2. Full Directory Tree

```
research-collab/
│
├── pom.xml                              ← Maven build (Spring Boot, JavaFX, BigQuery, org.json)
├── ARCHITECTURE_DOCUMENTATION.md       ← This document
├── import_staff_data_v2.py             ← Python: CSV → MySQL import script
│
└── src/
    └── main/
        ├── java/com/research/
        │   │
        │   ├── ResearchCollaborationApp.java       ← Spring Boot + JavaFX bootstrap
        │   │
        │   ├── config/
        │   │   └── (Spring Security, JPA configs)
        │   │
        │   ├── model/                            ← JPA Entity layer (14-column Expert, Users, Papers)
        │   │   ├── Expert.java                   ← Core entity: 14 columns + scoreAgainst() + aggregated text
        │   │   ├── User.java                     ← Base user class
        │   │   ├── Researcher.java               ← Inherits User
        │   │   ├── Admin.java                    ← Inherits User
        │   │   ├── Reviewer.java                 ← Inherits User
        │   │   ├── CollaborationRequest.java
        │   │   ├── ResearchPaper.java
        │   │   ├── ResearchProject.java
        │   │   ├── ProjectMessage.java
        │   │   ├── ProjectUpdate.java
        │   │   ├── PublicOpinion.java
        │   │   ├── Visitor.java
        │   │   ├── Collaborator.java
        │   │   └── UserSubclasses.java
        │   │
        │   ├── repository/                       ← Spring Data JPA repositories
        │   │
        │   ├── service/                          ← Business logic layer
        │   │   ├── AuthService.java              ← Login, register, session management
        │   │   ├── ExpertService.java            ← Load & cache all Expert entities from MySQL
        │   │   ├── ExpertDataSeeder.java         ← Populates Expert table from CSV on startup
        │   │   ├── MentorScoutClient.java        ← ★ ADK 2-step session protocol client
        │   │   ├── RecommendationService.java    ← Delegates to strategy via context
        │   │   ├── CollaborationService.java     ← Handles collab requests
        │   │   └── PaperService.java             ← Research paper CRUD
        │   │
        │   ├── pattern/                          ← Design pattern implementations
        │   │   ├── BehavioralPatterns.java       ← Strategy impls + Observer + TF-IDF engine
        │   │   ├── RecommendationContext.java    ← Strategy Context (swaps strategies at runtime)
        │   │   ├── RecommendationFacade.java     ← Facade: single entry point for UI
        │   │   ├── ResearchUpdateObserver.java   ← Observer interface
        │   │   ├── PaperPublicationSubject.java  ← Subject (publisher)
        │   │   ├── StructuralPatterns.java       ← Decorator pattern for paper filters
        │   │   ├── CreationalPatterns.java       ← Factory + Singleton patterns
        │   │   ├── UserFactory.java              ← Factory: creates correct User subtype
        │   │   ├── DatabaseConnectionManager.java ← Singleton DB connection manager
        │   │   ├── DomainFilterDecorator.java    ← Decorator: filters papers by domain
        │   │   ├── PublishedOnlyDecorator.java   ← Decorator: filters unpublished papers
        │   │   ├── BasicPaperSearch.java         ← Component for Decorator chain
        │   │   └── PaperSearchComponent.java     ← Paper search interface
        │   │
        │   └── view/                             ← JavaFX UI layer
        │       ├── auth/                         ← Login / Register screens
        │       ├── dashboard/                    ← Main navigation hub
        │       ├── expert/                       ← ★ Expert search (all 3 modes)
        │       │   ├── ExpertSearchPane.java     ← Main search pane with 3-mode renderer
        │       │   ├── ExpertSearchView.java     ← Container view that hosts the pane
        │       │   ├── ExpertCardUI.java         ← List card: image, name, dept, email, phone
        │       │   ├── ExpertProfileDialogBuilder.java ← Builder: full 14-column profile popup
        │       │   └── ResearcherCardUI.java     ← Researcher collaboration card
        │       ├── admin/                        ← Admin management views
        │       ├── collab/                       ← Collaboration request inbox/outbox
        │       ├── research/                     ← My Research Projects view
        │       ├── paper/                        ← Paper search & publish
        │       └── reviewer/                     ← Reviewer dashboard
        │
        └── resources/
            ├── application.properties            ← All config (DB, GCP, Email, n8n)
            └── (CSS, assets)
```

---

## 3. Expert Search Architecture — Three Modes

The Expert Search panel (`ExpertSearchPane.java`) exposes **three radio-button modes**. All modes share the same search bar and results container. Each mode is wired differently under the hood.

### Mode 1: Keyword Search

**Strategy Class:** `KeywordMatchingStrategy` in `BehavioralPatterns.java`

**How it works:**

```
User types query → Java Streams filter local MySQL Expert list
 ┌──────────────────────────────────────────┐
 │  experts.stream()                        │
 │    .filter(e → e.isActive())             │
 │    .filter(e → e.scoreAgainst(query) > 0)│
 │    .sorted(by score DESC)                │
 │    .limit(topN)                          │
 └──────────────────────────────────────────┘
```

- `Expert.scoreAgainst(query)` scans the expert's aggregated profile text (name + department + teaching + research areas) against tokenized query words.
- **100% local execution** — no network calls, sub-millisecond response.
- Source of truth: MySQL `pes_staff_data` database loaded into memory by `ExpertService`.

---

### Mode 2: Semantic Search (BigQuery Embedding)

**Strategy Class:** `BigQuerySemanticStrategy` in `BehavioralPatterns.java`

**Cloud Resources used:**

| Resource | Value |
|----------|-------|
| GCP Project | `developer-491706` |
| BigQuery Dataset | `pes_staff_dataset` |
| BigQuery Table | `pes_staff_info` |
| Embedding Model | `developer-491706.pes_staff_dataset.embedding_model` |

**SQL query sent to BigQuery:**

```sql
SELECT base.name, base.department, base.research
FROM VECTOR_SEARCH(
    TABLE `developer-491706.pes_staff_dataset.pes_staff_info`,
    'embedding',
    (SELECT text_embedding AS embedding FROM ML.GENERATE_TEXT_EMBEDDING(
        MODEL `developer-491706.pes_staff_dataset.embedding_model`,
        (SELECT @val AS content)
    )),
    top_k => 10,
    distance_type => 'COSINE'
)
```

**Step-by-step execution flow:**

```
1. User types "cyber security"
2. Java sends query to BigQuery via google-cloud-bigquery SDK
3. BigQuery's ML.GENERATE_TEXT_EMBEDDING converts "cyber security" → high-dimensional float vector
4. VECTOR_SEARCH computes Cosine Similarity of the query vector against all pre-stored expert vectors
5. Returns top_k=10 CLOSEST expert names by semantic meaning
6. Java maps returned names → full Expert JPA objects (from local MySQL)
7. Results rendered as ExpertCardUI cards
```

> **Why Cosine Similarity?** It measures the angular similarity between two vectors, not their magnitude. This makes it ideal for comparing text embeddings regardless of document length.

---

### Mode 3: Mentor Scout AI Agent

**Client Class:** `MentorScoutClient.java`  
**Agent Deployment:** Google Cloud Run (ADK — Agent Development Kit)  
**Agent Endpoint:** `https://mentor-scout-482781773486.us-central1.run.app`

This is the most powerful mode. The user can type **natural language queries** like:
- `"Get me the image of Surabhi Narayana"`
- `"Deep search on Sindhu Pai"`
- `"Who are the best experts in cyber security?"`

The entire response — images, links, formatted text, hyperlinks — is dynamically rendered in the JavaFX response panel.

> **See Section 6** for the complete ADK protocol and session management documentation.

---

## 4. Database & Dataset Setup

### MySQL Database (`pes_staff_data`)

The application uses a local **MySQL database** populated from the PES staff CSV dataset.

**Setup Steps:**

```bash
# 1. Create database
CREATE DATABASE pes_staff_data;

# 2. Run the Python import script to load CSV data
python import_staff_data_v2.py
```

The Python script (`import_staff_data_v2.py`) reads the staff CSV file and bulk-inserts all rows into MySQL via SQLAlchemy. The `Expert.java` entity maps to the resulting table via Spring JPA.

**Expert Table Columns (14 total):**

| Column | Description |
|--------|-------------|
| `id` | Auto-generated primary key |
| `name` | Full staff name |
| `designation` | Job title (Professor, Assistant Professor, etc.) |
| `department` | Academic department |
| `campus` | Campus location (RR or EC) |
| `email` | Institutional email address |
| `phone` | Contact number |
| `profileUrl` | URL to staff photo on `staff.pes.edu` |
| `about` | Bio / responsibilities |
| `education` | Degrees and institutions |
| `teaching` | Teaching subjects/courses |
| `researchAreas` | Active research interests |
| `publicationsJournals` | Journal publication list |
| `publicationsConferences` | Conference papers list |

**`application.properties` — MySQL configuration:**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pes_staff_data
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
```

---

### BigQuery Dataset (`pes_staff_dataset`)

A **separate cloud dataset** in Google BigQuery containing pre-computed AI embedding vectors for each expert.

**Table:** `pes_staff_info`  
All rows in this table have an `embedding` column — a pre-computed float vector of the expert's research profile text, generated by the `embedding_model`.

**`application.properties` — GCP configuration:**

```properties
google.cloud.project.id=developer-491706
google.cloud.bigquery.dataset=pes_staff_dataset
google.cloud.bigquery.table=pes_staff_info
google.cloud.bigquery.model=embedding_model
```

> **Key insight:** MySQL stores all 14 columns (identity, contact, publications). BigQuery stores only the **embedding vectors** for semantic search. The two databases work together: BigQuery finds semantically relevant names → Java hydrates full profiles from MySQL.

---

## 5. Google Cloud BigQuery — Vector Search Deep Dive

### Authentication — Application Default Credentials (ADC)

BigQuery requires Google Cloud authentication. On a fresh machine:

```bash
# Step 1: Login
gcloud auth application-default login

# Step 2: Set billing project
gcloud auth application-default set-quota-project developer-491706
```

This creates a credentials file at:
```
%APPDATA%\gcloud\application_default_credentials.json
```

The Java code explicitly loads these credentials:

```java
com.google.auth.Credentials credentials = 
    com.google.auth.oauth2.GoogleCredentials.getApplicationDefault();
```

If credentials are missing, the app throws a clear error:
```
Critical Google Cloud Auth Error. Ensure you have run 
'gcloud auth application-default login'
```

### Error Recovery

| Error | Cause | Fix |
|-------|-------|-----|
| `NullPointerException: getUniverseDomain()` | ADC credentials not set | Run `gcloud auth application-default login` |
| `HTTP 403 Forbidden` | Wrong/expired project quota | Run `gcloud auth application-default set-quota-project developer-491706` |
| `HTTP 429 Quota Exceeded` | Too many BigQuery requests | Wait, or retry with backoff |

---

## 6. Mentor Scout AI Agent — 2-Step ADK Protocol

### What is ADK?

ADK (Agent Development Kit) is Google's framework for deploying AI agents on Cloud Run. The Mentor Scout agent is deployed at a public Cloud Run URL and understands natural language queries about PES staff.

### The 2-Step Session Protocol

The ADK API requires a **pre-registered session** before any query can be executed. This is unlike stateless REST APIs.

```
┌─────────────────────────────────────────────────────────────┐
│                    MentorScoutClient.java                    │
│                                                             │
│  App Starts                                                 │
│       │                                                     │
│       └─ First callAgent() invocation                       │
│             │                                               │
│             ├─ cachedSessionId == null?                     │
│             │       YES →                                   │
│             │  STEP 1: POST /apps/mentor_scout/             │
│             │               users/java_client/sessions       │
│             │       ↓ Server returns { "id": "abc-xyz" }    │
│             │  cachedSessionId = "abc-xyz"  ✅ Cached       │
│             │                                               │
│             └─ STEP 2: POST /run                            │
│                   Body: { session_id: "abc-xyz",            │
│                           app_name: "mentor_scout",         │
│                           user_id: "java_client",           │
│                           new_message: { role: "user",      │
│                             parts: [{ text: "<query>" }] }} │
│                   ↓ Returns ADK event array                 │
│                   extractAgentText() parses events → string │
│                                                             │
│  Subsequent queries:                                        │
│       └─ cachedSessionId != null → SKIP Step 1, reuse ID   │
│                                                             │
│  On HTTP 500 from /run:                                     │
│       └─ cachedSessionId = null ← Reset for fresh session   │
└─────────────────────────────────────────────────────────────┘
```

### Session Lifecycle

| Event | Behavior |
|-------|----------|
| App launch | No session exists yet |
| First query | Session registered via POST `/sessions` |
| Subsequent queries in same session | Session ID reused — no extra network round-trip |
| App closed | Session ID goes out of scope (garbage collected) — effectively invalidated |
| HTTP 500 response | Session ID reset to `null` — next query creates a fresh session |

### ADK Response Parsing

The agent returns a JSON **array of events**, not a simple string. The extraction logic:

```java
JSONArray events = new JSONArray(rawJson);
for each event:
    if event.has("content") && content.has("parts"):
        for each part:
            if part.has("text") → append to result
```

### Mentor Scout Response Renderer — `renderScoutResponse()`

The renderer in `ExpertSearchPane.java` handles all response formats:

| Response Format | Rendering |
|----------------|-----------|
| `<img src="...">` | Downloads and displays `ImageView` with drop shadow |
| `[Label](https://url)` | Rendered as clickable `Hyperlink` with ↗ symbol |
| `### Heading` / `#### Heading` | Styled colored `TextField` (orange/green/blue) |
| `- bullet item` | Prefixed with `•` symbol |
| `**bold text**` | Stripped of `**`, rendered bold |
| Plain text | Selectable `TextField` (transparent, no border) |
| Entire response | **📋 Copy All** button — copies raw response to clipboard |

> **All rendered text is selectable and copyable** using `TextField(editable=false)` with transparent CSS styling. Ctrl+A and Ctrl+C work on every text element.

### Browser Hyperlink Opening (Windows)

Standard `Desktop.getDesktop().browse()` fails inside JavaFX apps. The fix:

```java
new ProcessBuilder("cmd", "/c", "start", url.replace("&", "^&")).start();
```

This fires `cmd /c start <url>` natively, which correctly opens the system default browser on Windows.

---

## 7. Design Patterns Used

### Summary Table

| Pattern | Category | File | Purpose |
|---------|----------|------|---------|
| Strategy | Behavioral | `BehavioralPatterns.java`, `RecommendationContext.java` | Swap search algorithm at runtime |
| Builder | Creational | `ExpertProfileDialogBuilder.java` | Construct complex 14-column profile dialog |
| Observer | Behavioral | `BehavioralPatterns.java`, `ResearchUpdateObserver.java` | Email notifications on paper publish |
| Factory | Creational | `UserFactory.java` | Create correct User subtype (Admin/Researcher/Reviewer) |
| Singleton | Creational | `DatabaseConnectionManager.java` | One shared DB connection instance |
| Decorator | Structural | `DomainFilterDecorator.java`, `PublishedOnlyDecorator.java` | Layered paper search filtering |
| Facade | Structural | `RecommendationFacade.java` | Single entry point for UI to access all recommendation logic |

---

### 1. Strategy Pattern

**Files:** `BehavioralPatterns.java` + `RecommendationContext.java`

```
«interface»
RecommendationStrategy
    + recommend(query, experts, topN): List<Expert>
    + strategyName(): String
           ▲
           │ implements
    ┌──────┴──────────────┬──────────────────────┐
    │                     │                      │
KeywordMatchingStrategy  BigQuerySemanticStrategy  SemanticTFIDFStrategy
(local, fast)            (cloud AI, cosine)        (TF-IDF scoring)

           ↑ managed by
    RecommendationContext
    + setStrategyByMode(mode)
    + execute(query, experts, topN)
```

The `RecommendationContext` is a Spring `@Component` that holds the currently active strategy. The UI radio buttons call `setStrategyByMode("ai")` or `setStrategyByMode("keyword")` to swap strategies at runtime without any code change — **pure Open/Closed Principle**.

---

### 2. Builder Pattern

**File:** `ExpertProfileDialogBuilder.java`

```java
new ExpertProfileDialogBuilder(expert)
    .build()          // Constructs: header, image, scrollable content
    .showAndWait();   // Renders the complete 14-column dialog
```

Breaks the UI construction into clean phases:
- `buildHeader()` → Profile image + name + email + phone
- `buildScrollableContext()` → About, Education, Teaching, Research, Publications

Isolates all dialog construction logic from `ExpertCardUI` (SRP compliance).

---

### 3. Observer Pattern

**Files:** `ResearchUpdateObserver.java`, `PaperPublicationSubject.java`, `BehavioralPatterns.java`

```
PaperPublicationSubject (Subject)
    + attach(observer)
    + detach(observer)
    + notifyObservers(paper, keyword)
           │
           │ notifies
           ↓
EmailNotificationObserver (Concrete Observer)
    + onNewPaper(paper, keyword)
           │
           │ calls
           ↓
N8nWebhookCaller
    + triggerEmailNotification(email, paper, keyword)
           │
           │ HTTP POST (async Thread)
           ↓
n8n Webhook → Gmail SMTP → Researcher's Inbox
```

---

### 4. Factory Pattern

**File:** `UserFactory.java`  
Creates the correct `User` subtype (`Admin`, `Researcher`, `Reviewer`) based on a role string. Decouples object creation from the service layer.

---

### 5. Singleton Pattern

**File:** `DatabaseConnectionManager.java`  
Ensures exactly one shared connection instance is created throughout the application lifecycle, preventing resource waste and connection pool exhaustion.

---

### 6. Decorator Pattern

**Files:** `DomainFilterDecorator.java`, `PublishedOnlyDecorator.java`, `BasicPaperSearch.java`

```
PaperSearchComponent (interface)
       ↑
BasicPaperSearch (base)
       ↑ wraps
DomainFilterDecorator
       ↑ wraps
PublishedOnlyDecorator
```

Paper search results can be wrapped with zero or more filters at runtime without altering the original classes — **Open/Closed Principle in action**.

---

### 7. Facade Pattern

**File:** `RecommendationFacade.java`  
The UI delegates to a single facade method rather than calling `RecommendationContext` + `ExpertService` + filtering logic separately. The facade orchestrates all internal subsystems transparently.

---

## 8. SOLID & Other Design Principles

| Principle | Full Name | Applied Where |
|-----------|-----------|---------------|
| **S** | Single Responsibility | `ExpertCardUI` only renders card UI. `ExpertProfileDialogBuilder` only builds dialog. `MentorScoutClient` only handles agent HTTP. |
| **O** | Open/Closed | `RecommendationStrategy` interface is closed for modification. Adding a new search mode (e.g., a future GraphSearch) only requires a new implementing class — no changes to existing code. |
| **L** | Liskov Substitution | `Admin`, `Researcher`, `Reviewer` all extend `User`. Any service method accepting `User` works correctly with all subtypes. |
| **I** | Interface Segregation | `ResearchUpdateObserver` has only one method `onNewPaper()`. `RecommendationStrategy` has minimal required methods. No fat interfaces. |
| **D** | Dependency Inversion | `RecommendationContext` depends on `RecommendationStrategy` (abstraction), not on `KeywordMatchingStrategy` (concrete). UI depends on `RecommendationFacade`, not on BigQuery SDK directly. |

### Additional Principles

| Principle | Applied Where |
|-----------|---------------|
| **Fail-Fast Validation** | `ExpertCardUI` and `ExpertProfileDialogBuilder` intercept `null`, `"nan"`, `"not listed"` strings at entity injection — the UI never crashes on bad data. |
| **Progressive Disclosure** | Cards show minimal info (name, dept, email, phone). Full 14-column data only in expanded dialog — reduces cognitive overload. |
| **Defensive Coding** | `BigQuerySemanticStrategy` explicitly checks for null credentials before SDK call, giving a clear developer error message instead of a cryptic NullPointerException. |
| **Separation of Concerns** | View layer (`view/`) knows nothing about BigQuery SDK. Service layer (`service/`) knows nothing about JavaFX nodes. Strict layered architecture. |
| **DRY (Don't Repeat Yourself)** | `selectableTextField()` helper method defined once in each UI class and reused for all field types. |

---

## 9. UI Architecture — Expert Card & Profile Dialog

### ExpertCardUI — List Card (Minimal View)

```
┌─────────────────────────────────────────────────────────────────────┐
│  [Profile Image]   Name (selectable)                   [📄 Details] │
│  (120×150px)       Teaching — Computer Science Dept                  │
│                    Campus: Ring Road Campus                          │
│                    Email: name@pes.edu      (selectable, blue)      │
│                    Phone: +91-XXXXXXXXXX    (selectable)            │
└─────────────────────────────────────────────────────────────────────┘
```

All text fields are `TextField(editable=false)` with transparent CSS, enabling:
- **Mouse drag to select text**
- **Ctrl+A** to select all
- **Ctrl+C** to copy

### ExpertProfileDialogBuilder — Full Profile Dialog (14 columns)

```
┌─────────────────────────────────────────────────────────────────────┐
│  [Avatar 140×180]  Name (26pt, selectable)                         │
│                    Designation — Department (selectable)            │
│                    Campus (selectable)                              │
│                    Email: ...   Phone: ...  (both selectable)       │
├─────────────────────────────────────────────────────────────────────┤
│  ▶ About & Responsibilities   [TextArea, editable=false, copyable] │
│  ▶ Education                  [TextArea, editable=false, copyable] │
│  ▶ Teaching Details           [TextArea, editable=false, copyable] │
│  ▶ Research Interests         [TextArea, editable=false, copyable] │
│  ▶ Publications (Journals)    [TextArea, editable=false, copyable] │
│  ▶ Publications (Conferences) [TextArea, editable=false, copyable] │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 10. Expert Profile Retrieval — Hybrid Persistence Mechanism

This answers: *"BigQuery only returns names — how do we get full profile details?"*

```
User clicks "View Details"
        │
        ▼
ExpertCardUI holds reference to full Expert JPA object (from MySQL)
        │
        ▼
ExpertProfileDialogBuilder(expert)
        │
        ▼
Reads all 14 columns directly from the in-memory Expert object
        │
        ▼
No additional database call needed — profile already loaded in RAM
```

**Two-database architecture:**

```
MySQL (local)              BigQuery (cloud)
─────────────              ───────────────
14 columns of data    ←→   Only: name + embedding vector
Full profile details        Used for: semantic search ranking
Loaded once at startup      Called per: user query
Stays in JPA memory         Network call on each search
```

---

## 11. Authentication & Google Cloud Security

### Local App Auth (Spring Security)

| Component | Detail |
|-----------|--------|
| Framework | Spring Security (auto-configured) |
| Default Admin | Created at startup: `admin@research.com` |
| Password Storage | BCrypt-hashed in MySQL |
| Session | In-memory session per app lifetime |

### Google Cloud Auth (BigQuery)

```
%APPDATA%\gcloud\application_default_credentials.json
           ↓ loaded by
GoogleCredentials.getApplicationDefault()
           ↓ used by
BigQueryOptions.newBuilder().setCredentials(credentials).build()
```

**Required one-time setup:**
```bash
gcloud auth application-default login
gcloud auth application-default set-quota-project developer-491706
```

---

## 12. Email Notification Workflow (Observer + n8n)

When a researcher publishes a new paper, the notification pipeline fires:

```
PaperPublicationSubject.notifyObservers(paper, keyword)
           │
           ▼
EmailNotificationObserver.onNewPaper(paper, keyword)
  (checks if observer follows that keyword)
           │
           ▼
N8nWebhookCaller.triggerEmailNotification(email, paper, keyword)
  (builds JSON payload, fires async HTTP POST)
           │
           ▼
n8n Webhook (ngrok URL in application.properties)
           │
           ▼
Gmail SMTP → Researcher's Email Inbox
```

**Configuration:**
```properties
# Gmail SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=shivakumarullagaddi855@gmail.com
spring.mail.password=<Google App Password>

# n8n Automation
n8n.webhook.url=https://<ngrok-url>/webhook-test/research-update
```

---

## 13. Application Properties Configuration Reference

Complete annotated reference for `src/main/resources/application.properties`:

```properties
# ─── SERVER ───────────────────────────────────────────────────
server.port=8080

# ─── MYSQL DATABASE ───────────────────────────────────────────
# Database: pes_staff_data (populated by import_staff_data_v2.py)
spring.datasource.url=jdbc:mysql://localhost:3306/pes_staff_data
    ?createDatabaseIfNotExist=true
    &useSSL=false
    &serverTimezone=Asia/Kolkata
    &allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ─── JPA / HIBERNATE ──────────────────────────────────────────
spring.jpa.hibernate.ddl-auto=update   # auto-creates/updates tables
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# ─── EMAIL (GMAIL SMTP) ───────────────────────────────────────
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=shivakumarullagaddi855@gmail.com
spring.mail.password=<16-char Google App Password>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# ─── N8N AUTOMATION WEBHOOK ───────────────────────────────────
n8n.webhook.url=https://<ngrok-url>/webhook-test/research-update

# ─── GOOGLE CLOUD + BIGQUERY ──────────────────────────────────
google.cloud.project.id=developer-491706
google.cloud.bigquery.dataset=pes_staff_dataset
google.cloud.bigquery.table=pes_staff_info
google.cloud.bigquery.model=embedding_model

# ─── LOGGING ──────────────────────────────────────────────────
logging.level.com.research=INFO
logging.level.org.springframework.security=WARN
```

---

## 14. Thread Safety & Concurrency Model

The JavaFX Application Thread (UI thread) must never block. All heavy operations run off-thread:

```
User clicks "Find Experts"
        │
        ▼
javafx.concurrent.Task<List<Expert>> (background thread)
        │  runs in background
        ├─ Keyword mode: local filtering (fast, ~1ms)
        ├─ Semantic mode: BigQuery HTTP call (~2-5s)
        └─ Mentor Scout mode: ADK HTTP call (~5-15s)
        │
        ▼ task.setOnSucceeded()
back on JavaFX Application Thread
        │
        ▼
UI rendered: ExpertCardUI cards or renderScoutResponse()
```

**Key guarantees:**
- UI always stays at 60 FPS — never freezes during network calls
- BigQuery SDK calls are inside `Task.call()` — isolated from UI thread
- Mentor Scout HTTP calls use `java.net.http.HttpClient` synchronously inside the Task background thread
- All UI updates (adding cards to VBox) are done in `Platform.runLater()` or `setOnSucceeded()`

---

## 15. Error Handling & Observability

### Log Prefix Convention

Every component writes prefixed logs for clean filtering:

| Prefix | Component |
|--------|-----------|
| `[BigQuery]` | Semantic search strategy |
| `[MentorScout]` | ADK client (session, /run calls) |
| `[MentorScout ERROR]` | ADK error with full body dump |
| `[ScoutRenderer]` | UI renderer (image detection, link opens) |
| `[ExpertSearchPane]` | Search orchestration, results count |
| `[AuthService]` | Login/register events |
| `[DashboardView]` | Navigation events |
| `[DataSeeder]` | CSV/DB seeding on startup |

### Key Error Scenarios & Handling

| Scenario | Detection | Recovery |
|----------|-----------|----------|
| BigQuery credentials missing | Catches `NullPointerException` with `getUniverseDomain` in message | Throws descriptive error pointing to `gcloud auth` command |
| Mentor Scout HTTP 500 | Status code check | Logs full response body, resets `cachedSessionId` to `null` |
| Mentor Scout HTTP 404 | Caught — was caused by locally-generated session IDs | Fixed by implementing 2-Step ADK Protocol |
| Image URL load failure | `try/catch` around `new Image(url)` | Silently shows `👤` placeholder emoji |
| Browser open failure | `try/catch` around `ProcessBuilder` | Falls back to `Desktop.getDesktop().browse()` |
| Expert name not in MySQL | `Optional.orElse(null)` check during BQ mapping | Logs warning, skips the row |
| Null/NaN field values | Ternary guards in `ExpertCardUI` and `ExpertProfileDialogBuilder` | Shows "Not listed" gracefully |

---

*Document last updated: April 2026. This is the complete architectural record of all search, AI, and UI decisions made during the development of the PES Research Collaboration Platform.*
