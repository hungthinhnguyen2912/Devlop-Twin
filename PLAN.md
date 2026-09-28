# Developer Twin — Kế hoạch triển khai V1

> File này là "bản đồ" để làm dự án từng bước một. Làm xong bước nào, tick `[x]` bước đó.
> Quy tắc: **mỗi lần chỉ làm 1 checkbox, xong thì commit ngay.** Không nhảy cóc.

---

## Cách làm việc (đọc 1 lần trước khi bắt đầu)

1. **Mỗi checkbox = 1 commit.** Commit message ngắn gọn, ví dụ: `feat(connector): fetch github profile`.
2. **Đừng cầu toàn.** V1 chạy được quan trọng hơn code đẹp. Refactor là việc của vòng sau.
3. **Mỗi milestone có "Definition of Done"** — đạt được nó là xong, không làm thêm.
4. **Bí ở đâu quá 30 phút** → ghi lại câu hỏi, hỏi AI hoặc tìm docs, đừng ngồi mò vô hạn.
5. Thứ tự milestone là bắt buộc: M0 → M1 → M2 → M3 → M4. Mỗi milestone xây trên milestone trước.

---

## Milestone 0 — Dựng khung dự án (ước tính: 1 buổi)

Mục tiêu: có skeleton chạy được trên máy local, chưa có logic gì.

### Backend

- [X] Tạo project Spring Boot bằng [Spring Initializr](https://start.spring.io):
  - Maven, Java 21, Spring Boot 4.x
  - Dependencies: `Spring Web`, `Spring Data JPA`, `PostgreSQL Driver`, `Flyway Migration`, `Validation`, `Lombok`
  - Group: `com.devtwin` — Artifact: `backend`
  - Giải nén vào folder `backend/`
- [x] Tạo các package rỗng theo kiến trúc module (xem cây thư mục trong Readme):
  `api`, `connector`, `connector.github`, `analyzer`, `twinengine`, `knowledge`, `evidence`, `ai`, `common`
- [x] Chạy `docker compose up -d` để bật Postgres (file `docker-compose.yml` đã có sẵn ở root)
- [ ] Cấu hình `application.yml` kết nối Postgres (db: `devtwin`, user/pass: `devtwin`)
- [ ] Tạo migration Flyway đầu tiên `V1__init.sql` — bảng `raw_data`, copy DDL từ [docs/database.md](./docs/database.md)
- [ ] Viết 1 endpoint `GET /api/health` trả về `{"status": "ok"}`
- [ ] **Kiểm tra**: `./gradlew bootRun` chạy không lỗi, mở `http://localhost:8080/api/health` thấy kết quả

### Frontend

- [ ] Tạo project: `npm create vite@latest frontend -- --template react-ts`
- [ ] Cài Tailwind CSS (theo docs Tailwind + Vite)
- [ ] Cấu hình proxy trong `vite.config.ts`: `/api` → `http://localhost:8080`
- [ ] Trang chủ gọi `GET /api/health` và hiển thị kết quả
- [ ] **Kiểm tra**: `npm run dev`, mở `http://localhost:5173` thấy status "ok" từ backend

### Chung

- [ ] `git init` (nếu chưa), commit đầu tiên
- [ ] Copy `.env.example` thành `.env`, điền GitHub token (tạo tại GitHub → Settings → Developer settings → Personal access tokens, scope `public_repo`)

**Definition of Done**: Frontend hiển thị được dữ liệu lấy từ backend, backend kết nối được Postgres.

---

## Milestone 1 — GitHub Connector (ước tính: 1–2 tuần buổi tối)

Mục tiêu: nhập 1 GitHub username → toàn bộ dữ liệu thô được lưu vào Postgres.

Nguyên tắc từ Readme: Connector chỉ **thu thập**, không phân tích. Kết quả là raw data.

- [ ] Định nghĩa interface `Connector` trong package `connector`:
  - `String platform()` — ví dụ trả về `"github"`
  - `RawFetchResult fetch(String username)`
- [ ] Viết entity/repository cho bảng `raw_data` (đã tạo ở M0 — cấu trúc xem [docs/database.md](./docs/database.md))
- [ ] Viết `GithubApiClient` dùng Spring `RestClient`, đọc token từ biến môi trường `GITHUB_TOKEN`
- [ ] Fetch **profile**: `GET /users/{username}` → lưu vào `raw_data`
- [ ] Fetch **danh sách repo**: `GET /users/{username}/repos` (nhớ phân trang, `per_page=100`) → lưu từng repo
- [ ] Fetch **languages** cho mỗi repo: `GET /repos/{owner}/{repo}/languages`
- [ ] Fetch **README** cho mỗi repo: `GET /repos/{owner}/{repo}/readme` (decode base64)
- [ ] Fetch **dependency files** nếu có: thử lấy `package.json`, `pom.xml`, `build.gradle`, `requirements.txt`, `go.mod`, `Dockerfile` qua Contents API
- [ ] Fetch **commit activity**: `GET /repos/{owner}/{repo}/stats/commit_activity` (chú ý: API này có thể trả 202, cần retry)
- [ ] Xử lý rate limit: đọc header `X-RateLimit-Remaining`, nếu gần hết thì dừng và báo lỗi rõ ràng
- [ ] Endpoint `POST /api/ingest/{username}` chạy toàn bộ quá trình fetch (dùng `@Async` hoặc chạy đồng bộ cũng được ở V1)
- [ ] Endpoint `GET /api/ingest/{username}/status` xem đã fetch được những gì
- [ ] Cache: nếu dữ liệu đã fetch trong vòng 24h thì không fetch lại (tránh tốn rate limit)

**Definition of Done**: Gọi `POST /api/ingest/{your-username}` → mở Postgres thấy đủ profile, repos, languages, README, dependency files trong bảng `raw_data`.

---

## Milestone 2 — Repository Analyzer (ước tính: 1–2 tuần)

Mục tiêu: biến raw data thành dữ liệu chuẩn hoá. **Đây là phần thuần logic, không gọi API ngoài, không AI** — viết unit test được và nên viết.

- [ ] Định nghĩa các DTO chuẩn hoá (Java record) trong package `analyzer`:
  - `NormalizedRepo`: tên, mô tả, ngôn ngữ chính, tech list, thời gian tạo/commit cuối, số sao, có phải fork không
  - `DetectedTech`: tên tech (ví dụ `spring-boot`, `redis`), loại (language/framework/database/tool), nguồn phát hiện
- [ ] Viết `TechDetector` rule-based, phát hiện tech từ:
  - [ ] Languages API (Java, Python, ...)
  - [ ] `pom.xml` / `build.gradle` → parse dependencies (ví dụ thấy `spring-boot-starter-data-redis` → biết Spring Boot + Redis)
  - [ ] `package.json` → dependencies (react, express, ...)
  - [ ] `requirements.txt`, `go.mod`
  - [ ] `Dockerfile` → base image (ví dụ `FROM postgres` → Postgres)
  - [ ] `docker-compose.yml` → services
- [ ] Tạo file mapping tech (ví dụ `tech-catalog.json` trong resources): tên dependency → tên tech chuẩn + category. Bắt đầu với ~50 tech phổ biến, mở rộng dần
- [ ] Viết unit test cho `TechDetector` với vài file dependency mẫu
- [ ] Migration `V2__normalized.sql`: bảng `normalized_repo` + `detected_tech` (DDL sẵn trong [docs/database.md](./docs/database.md))
- [ ] Endpoint `POST /api/analyze/{username}` chạy analyzer trên raw data đã có
- [ ] Loại trừ repo fork không có commit của chính user (tránh nhiễu)

**Definition of Done**: Sau khi analyze, DB có danh sách repo chuẩn hoá kèm tech đã phát hiện, mỗi tech ghi rõ phát hiện từ nguồn nào.

---

## Milestone 3 — Twin Engine + Evidence (ước tính: 1–2 tuần)

Mục tiêu: sinh Developer Twin đầu tiên — bản JSON tổng hợp có evidence cho mọi kết luận.

Nguyên tắc từ Readme: **không có evidence thì không có kết luận.**

- [ ] Migration `V3__twin.sql`: bảng `twin`, `skill_claim`, `evidence` (DDL sẵn trong [docs/database.md](./docs/database.md))
- [ ] Viết `TwinEngine` trong package `twinengine`:
  - Input: danh sách `NormalizedRepo` — **không đọc raw data, không gọi API** (đúng philosophy)
  - Gom tech từ tất cả repo → sinh `SkillClaim` + `Evidence`
  - Tính timeline đơn giản: tech này dùng từ năm nào đến năm nào (dựa trên created_at / pushed_at của repo)
- [ ] Endpoint `GET /api/twin/{username}` trả Twin JSON đầy đủ: profile, skills (kèm evidence), top repos, timeline
- [ ] Endpoint `POST /api/twin/{username}/build` chạy cả pipeline: ingest → analyze → build twin

**Definition of Done**: `GET /api/twin/{username}` trả về JSON có ít nhất: danh sách skill, mỗi skill kèm evidence trỏ đến repo/file cụ thể.

---

## Milestone 4 — AI Engine + UI (ước tính: 1–2 tuần)

Mục tiêu: hoàn thành V1 trong Readme — có giao diện nhập username và xem Twin, có phần mô tả do AI viết.

### AI

- [ ] Tạo Gemini API key tại [Google AI Studio](https://aistudio.google.com) (miễn phí), thêm vào `.env`
- [ ] Viết `GeminiClient` trong package `ai` (dùng Google GenAI Java SDK)
- [ ] Prompt: đưa Twin JSON (không đưa source code) → yêu cầu Gemini viết đoạn "Developer Summary" 3–5 câu
- [ ] Lưu summary vào bảng `twin`, chỉ gọi lại AI khi Twin thay đổi

### UI

- [ ] Trang Home: ô nhập GitHub username + nút "Build Twin"
- [ ] Gọi `POST /api/twin/{username}/build`, hiển thị trạng thái đang xử lý
- [ ] Trang Twin: hiển thị profile, AI summary, danh sách skill
- [ ] Click vào 1 skill → hiện danh sách evidence (repo nào, file nào)
- [ ] Danh sách top repos với tech đã phát hiện

**Definition of Done** = **V1 hoàn thành**: Nhập username bất kỳ → vài phút sau xem được Twin hoàn chỉnh với skill + evidence + AI summary trên giao diện web.

---

## Sau V1 (chưa cần nghĩ đến bây giờ)

- **V2**: Skill Graph (đồ thị quan hệ skill), Timeline chi tiết theo commit history
- **V3**: Resume Generator (PDF), Portfolio Generator (HTML tĩnh), Recruiter View
- **V4**: Chat with Twin, Career Coach
- Connector mới (GitLab, Dev.to...) — chỉ cần implement interface `Connector`, không sửa Twin Engine

---

## Ghi chú kỹ thuật nhanh

| Chủ đề | Quyết định | Lý do |
|---|---|---|
| Kiến trúc | Monolith module hoá, chưa microservice | Solo dev, tách sớm chỉ tốn công |
| Knowledge Graph | Postgres (bảng + JSONB), chưa dùng Neo4j | Đủ cho V1–V3, đổi sau được vì đã tách package `knowledge` |
| Background job | Chạy đồng bộ hoặc `@Async`, chưa cần queue | Đủ cho 1 user; thêm queue khi có nhiều user |
| Nginx | Không dùng ở local | Vite proxy lo CORS; production tính sau |
| AI | Chỉ nhận dữ liệu đã phân tích, không đọc source code | Đúng Development Philosophy trong Readme |
