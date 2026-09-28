# Developer Twin

> Understand a developer through their digital footprint.

> 📋 Kế hoạch triển khai từng bước: xem [PLAN.md](./PLAN.md)

## 1. Project Overview

Developer Twin là một nền tảng giúp xây dựng một "bản sao số" (Digital Twin) của một Developer thông qua các dấu vết kỹ thuật mà họ để lại trên Internet.

Thay vì chỉ sinh ra một CV hoặc Portfolio, hệ thống sẽ cố gắng hiểu:

- Developer đang làm gì
- Developer giỏi lĩnh vực nào
- Developer đang học gì
- Developer thích công nghệ gì
- Developer đã từng xây dựng những hệ thống nào
- Những kết luận đó được chứng minh bằng dữ liệu nào

CV, Portfolio hay Career Report chỉ là các góc nhìn khác nhau của cùng một Developer Twin.

---

# 2. Vision

Xây dựng một nền tảng có khả năng phân tích nhiều nguồn dữ liệu kỹ thuật khác nhau để tạo nên một hồ sơ kỹ thuật hoàn chỉnh cho một Developer.

Trong tương lai, hệ thống không chỉ phục vụ Developer mà còn có thể phục vụ Recruiter, Team Lead, Hiring Manager hoặc AI Assistant.

---

# 3. Goals

- Tự động phân tích Github Repository
- Hiểu Tech Stack của Developer
- Xây dựng Skill Graph
- Xây dựng Timeline phát triển
- Phân tích Domain Knowledge
- Sinh Portfolio
- Sinh Resume
- Hỗ trợ Recruiter đánh giá ứng viên
- Hỗ trợ AI trả lời các câu hỏi về Developer

---

# 4. Non Goals

Dự án không hướng tới:

- Clone LinkedIn
- Clone Github
- Clone Resume Builder
- Clone ChatGPT

Developer Twin sẽ chỉ sử dụng các nền tảng đó như nguồn dữ liệu.

---

# 5. High Level Architecture

```
                    User

                     │

             Developer Twin API

                     │

        ┌────────────┴────────────┐

   Connector Layer          Twin Engine

        │                         │

        └────────────┬────────────┘

              Knowledge Graph

                     │

                AI Engine

                     │

                 UI Layer
```

---

# 6. Core Modules

## Connector Layer

Thu thập dữ liệu từ nhiều nền tảng khác nhau.

Ví dụ:

- Github
- Gitlab
- Dev.to
- Docker Hub
- Stack Overflow
- Medium
- ...

Mỗi nền tảng sẽ được xây dựng thành một Connector riêng.

---

## Repository Analyzer

Phân tích Repository.

Có thể đọc:

- README
- Source Code
- Dependency
- Dockerfile
- CI/CD
- Workflow
- Configuration
- Commit
- Release

Kết quả sẽ được chuẩn hóa trước khi đưa sang Twin Engine.

---

## Twin Engine

Đây là trái tim của toàn bộ hệ thống.

Twin Engine chịu trách nhiệm:

- Tổng hợp dữ liệu
- Chuẩn hóa dữ liệu
- Phân tích dữ liệu
- Sinh Developer Twin

Twin Engine không gọi API bên ngoài.

Twin Engine không giao tiếp trực tiếp với UI.

Twin Engine chỉ làm việc với dữ liệu đã được chuẩn hóa.

---

## Knowledge Graph

Lưu toàn bộ tri thức của Developer.

Ví dụ:

- Repository
- Skill
- Technology
- Domain
- Timeline
- Achievement
- Interest
- Evidence

Mọi chức năng phía trên đều sử dụng Knowledge Graph.

---

## Evidence Engine

Mọi kết luận đều phải có bằng chứng.

Ví dụ:

Developer biết Redis.

↓

Evidence

- Repository
- Commit
- README
- Dependency
- Configuration

AI không được phép đưa ra kết luận nếu không có đủ Evidence.

---

## AI Engine

AI không đọc toàn bộ Source Code.

AI chỉ sử dụng dữ liệu đã được phân tích.

Nhiệm vụ của AI:

- Viết mô tả
- Tổng hợp thông tin
- Sinh CV
- Sinh Portfolio
- Trả lời câu hỏi
- Career Suggestion

---

## Resume Generator

Sinh CV từ Developer Twin.

Có thể hỗ trợ:

- PDF
- HTML
- Markdown

---

## Portfolio Generator

Sinh Portfolio Website.

Không cần người dùng tự viết.

---

## Recruiter View

Một góc nhìn dành cho Recruiter.

Có thể xem:

- Skill
- Evidence
- Timeline
- Domain
- Project nổi bật

---

## Career Coach

Đưa ra các gợi ý:

- Thiếu kỹ năng gì
- Nên học gì tiếp
- Phù hợp vị trí nào
- Xu hướng phát triển

---

# 7. Connector Roadmap

## Phase 1

Github

---

## Phase 2

Gitlab

---

## Phase 3

Dev.to

Docker Hub

---

## Phase 4

Stack Overflow

Medium

---

## Phase 5

Các Connector khác

---

# 8. Roadmap

## V1

Đọc Github Profile

Đọc Repository

Phân tích Tech Stack

Sinh Twin đầu tiên

---

## V2

Skill Graph

Evidence

Timeline

---

## V3

Portfolio

Resume

Recruiter View

---

## V4

AI Career Coach

AI Interview

Chat With Twin

---

## V5

Nhiều Connector hơn

Knowledge Graph hoàn chỉnh

---

# 9. Future Ideas

- Developer Ranking
- Team Twin
- Company Twin
- OSS Contribution Report
- Project Health Report
- Architecture Detection
- Interview Simulator
- Learning Recommendation
- API cho bên thứ ba

---

# 10. Documents

Sau này mỗi module sẽ có một tài liệu riêng.

```
docs/

connector.md

github-connector.md

repository-analyzer.md

knowledge-graph.md

twin-engine.md

evidence-engine.md

resume-generator.md

portfolio-generator.md

career-coach.md

api.md

database.md

deployment.md
```

README chỉ đóng vai trò mô tả tổng quan.

---

# 11. Development Philosophy

- Connector phải độc lập.
- Twin Engine không phụ thuộc Connector.
- AI chỉ làm lớp diễn giải.
- Mọi kết luận đều có Evidence.
- Mọi dữ liệu đều được chuẩn hóa trước khi xử lý.
- Hệ thống có thể mở rộng bằng cách thêm Connector mới mà không cần sửa Twin Engine.

---

# 12. Final Vision

Developer Twin không chỉ là một công cụ sinh CV.

Đây là một nền tảng có khả năng hiểu một Developer thông qua các dấu vết kỹ thuật mà họ tạo ra trong quá trình làm việc.

Mục tiêu cuối cùng là xây dựng một hệ thống có thể trả lời câu hỏi:

> "AI có thể hiểu một Developer đến mức nào nếu chỉ dựa trên những gì họ đã tạo ra?"

---

# 13. Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4, Maven |
| Database | PostgreSQL 16 (Docker), Flyway migration |
| GitHub API | Spring `RestClient` (REST API v3) |
| AI | Google Gemini (GenAI Java SDK) |
| Web UI | React, Vite, TypeScript, Tailwind CSS |
| Mobile (tương lai) | Flutter, dùng chung REST API |

Kiến trúc: **monolith module hoá** — mỗi module trong mục 6 là một package độc lập, giao tiếp qua interface và DTO chuẩn hoá. Không tách microservice ở giai đoạn này.

---

# 14. Project Structure

```
Developer_Twin/
├── Readme.md                  # Tài liệu này (vision + tổng quan)
├── PLAN.md                    # Kế hoạch triển khai từng bước
├── docker-compose.yml         # Postgres cho local dev
├── .env.example               # Mẫu biến môi trường (copy thành .env)
├── docs/                      # Tài liệu chi tiết từng module (viết dần)
│
├── DevTwin_Backend/demo/      # Spring Boot backend
│   └── src/main/
│       ├── java/com/devtwin/
│       │   ├── api/           # REST controllers
│       │   ├── connector/     # Interface Connector + github/ (Connector Layer)
│       │   ├── analyzer/      # Repository Analyzer: raw → normalized
│       │   ├── twinengine/    # Twin Engine: normalized → Developer Twin
│       │   ├── knowledge/     # Knowledge Graph: entities + repositories
│       │   ├── evidence/      # Evidence Engine
│       │   ├── ai/            # AI Engine: Gemini client + prompts
│       │   └── common/        # DTO chuẩn hoá, exception, config
│       └── resources/
│           ├── application.yml
│           └── db/migration/  # Flyway (V1__init.sql, ...)
│
└── frontend/                  # React + Vite
    └── src/
        ├── api/               # Hàm gọi REST API của backend
        ├── components/        # Component tái sử dụng
        ├── pages/             # Home, Twin view
        └── types/             # TypeScript types cho Twin JSON
```

Luồng dữ liệu giữa các package (một chiều, đúng Development Philosophy):

```
connector (raw data) → analyzer (normalized) → twinengine (twin) → ai (diễn giải)
                                    ↕
                          knowledge + evidence (lưu trữ)
```

---

# 15. Getting Started (Local)

```bash
# 1. Bật Postgres
docker compose up -d

# 2. Biến môi trường
#    Copy .env.example thành .env, điền GITHUB_TOKEN (và GEMINI_API_KEY sau này)

# 3. Backend
cd DevTwin_Backend/demo
./mvnw spring-boot:run     # chạy tại http://localhost:8080

# 4. Frontend (terminal khác)
cd frontend
npm install
npm run dev                # chạy tại http://localhost:5173, proxy /api sang backend
```

Các bước chi tiết để xây dựng từ đầu: xem [PLAN.md](./PLAN.md).
