# AI Framework - Spring MVC (no Boot) + LangChain4j + Claude

Small Spring MVC app showing agentic patterns in Java: tools, memory, sequence, loop, parallel, routing.

## Daily commands (VS Code terminal, project folder, Docker Desktop running)

| What | Command |
|---|---|
| Set the API key (once per terminal, hidden, not saved) | `$env:ANTHROPIC_API_KEY = [Net.NetworkCredential]::new('', (Read-Host 'API key' -AsSecureString)).Password` |
| Build the war (Maven runs in Docker) | `mvn package` |
| Start Tomcat (also after setting the key) | `docker compose up -d` |
| Load a new build into Tomcat | `docker compose restart` |
| See Tomcat logs | `docker logs -f ai-framework-tomcat` |
| Stop Tomcat | `docker compose down` |

Open: http://localhost:8080/

Keep Claude Code in a separate terminal WITHOUT the key.

## Pages

| URL | What it does |
|---|---|
| `/` | Home page with links |
| `/ask` | Chat with Claude: tools (6.1) + memory per browser session (6.2), "New chat" button |
| `/workflows` | Run a pattern: Sequence (6.3), Loop (6.4), Parallel (6.5a), Routing (6.5b) - shows each agent step |
| `/health` | Returns `OK` |

## Code map

```
pom.xml                          Maven: Spring MVC 7, JSTL, LangChain4j (+ anthropic, agentic), builds deploy/ROOT.war
compose.yaml                     Docker: Tomcat 11 + Java 21, serves deploy/ROOT.war, passes ANTHROPIC_API_KEY
src/main/java/com/pravin/ai/
  config/WebAppInitializer.java   replaces web.xml, starts Spring's DispatcherServlet
  config/WebConfig.java           Spring MVC config, JSP view resolver
  config/ClaudeModel.java         the one shared Claude connection (model, thinking, fallbacks)
  controller/HomeController.java  GET /, GET /health
  controller/AskController.java   GET/POST /ask, POST /ask/clear
  controller/WorkflowController.java  GET/POST /workflows
  service/Assistant.java          AI service interface for /ask (system message, @MemoryId)
  service/AssistantService.java   /ask brain: Claude + tools + chat memory
  service/WorkflowService.java    builds the 4 workflows from the agents
  agents/Agents.java              all small single-job agents (writer, reviewer, scorer, fixer, ...)
  tools/ShopTools.java            @Tool methods Claude can call (date/time, order status)
src/main/webapp/WEB-INF/views/    home.jsp, ask.jsp, workflows.jsp
```

## Patterns

| Pattern | Agents | How |
|---|---|---|
| Tools | Assistant + ShopTools | Claude asks for a method call, LangChain4j runs it, result goes back to Claude |
| Memory | Assistant | last 20 messages per browser session (`@MemoryId` = session id) |
| Sequence | CodeWriter -> CodeReviewer | `sequenceBuilder` - each agent's output is the next one's input |
| Loop | CodeWriter, then (CodeReviewer -> CodeScorer -> CodeFixer) | `loopBuilder` - repeat until score >= 0.8, max 3 rounds |
| Parallel | Security / Performance / Readability reviewers | `parallelBuilder` - all at once, results combined |
| Routing | Router -> CodeExpert / OrderAgent / GeneralAgent | `conditionalBuilder` - only the matching expert runs |

Agents share state through the AgenticScope: each agent's answer is stored under its `outputKey`
and read by later agents as `{{name}}` in their prompts.
