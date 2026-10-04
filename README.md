# Skill Studio

Standalone Spring Boot classroom extracted from the job-monitor learning huddle. Authored catalogs for senior Java backend interviews: skill and concept filters, in-depth notes, interview Q&A, programming examples, coding standards, design patterns, a downloadable studio zip, and a trainer voice that skips code.

No database, no LLM, no mail, no job search. Unknown skills get a generic huddle.

## Stack

- Java 21, Spring Boot 3.5.5
- Thymeleaf + Bootstrap 5
- Spring Security (`permitAll`, CSRF defaults; JSON and zip are GET)
- Actuator `health` and `info`
- No datasource

## Run locally

If a sibling [job-monitor](../job-monitor) checkout has `.tools` JDK 21 and Maven:

```bash
chmod +x run.sh
./run.sh
```

Otherwise:

```bash
export JAVA_HOME=...   # JDK 21
export PATH="$JAVA_HOME/bin:$PATH"
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

The app binds `server.port=${PORT:8080}` so PaaS port injection works.

## Deploy on Render (free)

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/Pavithra-soft/skill-studio)

1. Push is already on GitHub: [Pavithra-soft/skill-studio](https://github.com/Pavithra-soft/skill-studio).
2. Open [Deploy to Render](https://render.com/deploy?repo=https://github.com/Pavithra-soft/skill-studio) (or **New → Blueprint** and select the repo).
3. Render reads `render.yaml` + `Dockerfile`. No API keys required.
4. Open the `*.onrender.com` URL. Health check is `/actuator/health`.
5. Free instances sleep after idle time; the first request after sleep can take a minute.

## HTTP

| Path | Purpose |
|---|---|
| `GET /` | Classroom UI |
| `GET /api/skills.json` | Catalog (+ generated) skills |
| `GET /api/lesson.json?skill=&concept=&page=&size=` | Paginated huddle (default size 4, page 0-based) |
| `GET /api/generate?skill=` | Catalog lesson or generic huddle |
| `GET /api/project.zip?skills=&concepts=` | Downloadable Spring Boot studio |
| `GET /actuator/health` | Liveness for Render |

When `concept` is set, `page` is ignored and one concept is returned. Concept chips still list the full `catalog`.
