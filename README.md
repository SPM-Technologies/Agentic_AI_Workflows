# Agentic AI Workflows

Learning projects for building agentic AI workflows with Claude.

| Project | Stack | What it shows |
|---|---|---|
| [spring-mvc-langchain4j](spring-mvc-langchain4j/) | Java 21, Spring MVC 7 (no Boot), LangChain4j, Tomcat 11 in Docker | Tools, memory, sequence, loop, parallel and routing workflows with Claude |
| [appeal-assistant-demo](appeal-assistant-demo/) | Same stack, port 8090 | One end-to-end business workflow (denied-claim appeal) with routing, tools, check/revise loop, background work queue, **human approval** and audit log - synthetic data |

Each project folder has its own README and a `docs/` folder with a full HTML guide (architecture, flows, data model, classes, dependencies, testing).

**Only Docker is needed** - no Java or Maven on the machine. In a project folder:
```
docker compose up -d --build
```
(Set `ANTHROPIC_API_KEY` in the same terminal first - see each project's README.)
