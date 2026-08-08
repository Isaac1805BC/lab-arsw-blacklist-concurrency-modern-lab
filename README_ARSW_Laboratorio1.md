# ARSW — Blacklist Concurrency Laboratory

> **Java 21 laboratory on concurrency, performance measurement, fixed thread pools, and virtual threads.**

**Course:** Arquitecturas de Software — ARSW  
**Institution:** Universidad Escuela Colombiana de Ingeniería Julio Garavito  
**Professor:** Javier Iván Toquica  
**Work mode:** Teams of three students  
**Technology:** Java 21 · Maven · JUnit 5  
**Submission deadline:** Defined in the institutional platform

---

## 1. Laboratory purpose

This laboratory evaluates the implementation and experimental comparison of three strategies for consulting blacklist providers:

1. Sequential execution.
2. Concurrent execution with a fixed-size thread pool.
3. Concurrent execution with Java 21 virtual threads.

The goal is not merely to identify the fastest implementation. Each team must produce evidence to explain:

- When concurrency improves performance.
- When task coordination introduces more overhead than benefit.
- How blocking operations affect the choice of concurrency model.
- How correctness is preserved when several tasks execute concurrently.
- What architectural trade-offs exist among performance, complexity, scalability, and maintainability.

> **Correctness comes before performance.** A benchmark is invalid when the compared strategies do not produce equivalent results.

---

## 2. Relationship with Workshop 1

Workshop 1 and Laboratory 1 use the same case, but they are different activities.

### Workshop 1

- Inspect the starter project.
- Execute the sequential implementation.
- Analyze architectural decisions, quality attributes, metrics, and trade-offs.
- Do not implement the concurrent solutions.

### Laboratory 1

- Implement the missing concurrent strategies.
- Create automated tests.
- Execute a controlled benchmark.
- Analyze experimental evidence.
- Document and defend the resulting architectural recommendation.

The decision matrix completed during the workshop is **not** a laboratory deliverable. The laboratory grade is based on implementation, correctness, measurement, analysis, and repository evidence.

---

## 3. Problem statement

A system receives an IP address and asks multiple blacklist providers whether that address has been reported.

The starter project creates 100 deterministic providers. A provider can optionally simulate a blocking I/O operation by waiting for a controlled amount of time.

The supplied sequential implementation:

- Consults all providers.
- Collects the identifiers of matching providers.
- Reports the number of consulted providers.
- Measures elapsed time.
- Classifies the IP according to an alarm threshold.

The laboratory must preserve the same functional result while changing the execution strategy.

---

## 4. Starter project

The repository includes the following relevant classes:

```text
src/
├── main/
│   └── java/edu/eci/arsw/blacklist/
│       ├── BenchmarkRunner.java
│       ├── BlackListProvider.java
│       ├── BlackListSearch.java
│       ├── FixedPoolBlackListSearch.java
│       ├── MockBlackListProvider.java
│       ├── ProviderFactory.java
│       ├── SearchResult.java
│       ├── SequentialBlackListSearch.java
│       └── VirtualThreadBlackListSearch.java
└── test/
    └── java/edu/eci/arsw/blacklist/
        └── SequentialBlackListSearchTest.java
```

### Supplied implementation

`SequentialBlackListSearch` is complete and must be used as the functional baseline.

### Pending implementations

The following classes intentionally contain `TODO` work:

- `FixedPoolBlackListSearch`
- `VirtualThreadBlackListSearch`

`BenchmarkRunner` initially executes only the sequential strategy. Each team must extend it to run the required benchmark configurations.

---

## 5. Technical requirements

Before starting, verify:

```bash
java -version
mvn -version
```

Required versions:

- JDK 21.
- Maven 3.9 or later.
- Git.
- A GitHub account.

Compile and execute the supplied baseline:

```bash
mvn clean test
mvn exec:java
```

Execute the baseline with and without simulated I/O:

```bash
mvn exec:java -Dexec.args="202.24.34.55 true"
mvn exec:java -Dexec.args="202.24.34.55 false"
```

The default IP address is:

```text
202.24.34.55
```

---

## 6. Repository setup

Each team must create its own repository from this template.

Suggested repository name:

```text
arsw-blacklist-lab-gXX
```

Example:

```text
arsw-blacklist-lab-g03
```

Before modifying the code:

1. Add the three team members as collaborators.
2. Clone the team repository.
3. Verify Java 21 and Maven.
4. Run `mvn clean test`.
5. Execute the sequential baseline.
6. Create issues or tasks for the work distribution.
7. Record the baseline result in this README.

Every team member must contribute meaningful commits and must understand the complete solution.

---

# Part A — Concurrent implementation

## 7. Task 1: Fixed-size thread pool

Complete:

```text
FixedPoolBlackListSearch.java
```

The implementation must:

- Implement the `BlackListSearch` interface.
- Receive the provider list and pool size through the constructor.
- Validate that the pool size is greater than zero.
- Use `ExecutorService`.
- Create the executor with `Executors.newFixedThreadPool(poolSize)`.
- Submit provider consultations as concurrent tasks.
- Wait for all submitted tasks.
- Collect each matching provider identifier exactly once.
- Return deterministic results.
- Report the correct number of consulted providers.
- Measure elapsed time with `System.nanoTime()`.
- Close the executor correctly.
- Preserve interruption when an `InterruptedException` occurs.
- Avoid unsafe shared mutable state.

The required pool sizes are:

```text
2, 4, and 8 platform threads
```

### Implementation restrictions

The following approaches do not satisfy this task:

- Replacing the implementation with `parallelStream()`.
- Using the common `ForkJoinPool`.
- Protecting the entire search method with `synchronized`.
- Delegating the search to `SequentialBlackListSearch`.
- Removing or modifying the provider latency to improve results.
- Returning hard-coded matches.

A valid design may use tasks that return their own result and then consolidate those results after calling `Future.get()`.

---

## 8. Task 2: Java 21 virtual threads

Complete:

```text
VirtualThreadBlackListSearch.java
```

The implementation must:

- Implement the `BlackListSearch` interface.
- Use `Executors.newVirtualThreadPerTaskExecutor()`.
- Create one independent task per provider.
- Wait for all tasks to finish.
- Collect each matching provider identifier exactly once.
- Return deterministic results.
- Report the correct number of consulted providers.
- Measure elapsed time with `System.nanoTime()`.
- Close the executor correctly.
- Preserve interruption and provide meaningful error handling.
- Produce a result equivalent to the sequential baseline.

The virtual-thread implementation must not create a manually sized platform-thread pool.

---

## 9. Required result contract

For the mandatory part of this laboratory, all strategies must perform a **complete scan** of the provider list.

For the same IP address and provider configuration:

```text
Sequential result = Fixed-pool result = Virtual-thread result
```

The following values must be equivalent:

- Matching provider identifiers.
- Number of matching providers.
- Trustworthiness classification.
- Number of consulted providers.

Because concurrent tasks can finish in a different order, the returned matching provider identifiers must be ordered before constructing the final `SearchResult`.

For the supplied set of 100 providers:

```text
consultedProviders = 100
```

Early termination at five matches is not part of the mandatory implementation because it changes the amount of evidence collected. It appears only as an optional extension at the end of this document.

---

# Part B — Automated verification

## 10. Task 3: Tests

Add automated tests for the concurrent implementations.

At minimum, the test suite must verify:

1. The sequential implementation is deterministic.
2. A pool of 2 threads returns the same provider identifiers as the sequential baseline.
3. A pool of 4 threads returns the same provider identifiers as the sequential baseline.
4. A pool of 8 threads returns the same provider identifiers as the sequential baseline.
5. The virtual-thread strategy returns the same provider identifiers as the sequential baseline.
6. Every mandatory strategy reports all 100 providers as consulted.
7. Matching provider identifiers contain no duplicates.
8. Matching provider identifiers are returned in ascending order.
9. Creating a fixed-pool search with a non-positive pool size fails with `IllegalArgumentException`.
10. The project passes all tests with simulated I/O disabled.

Run:

```bash
mvn clean test
```

Tests must validate behavior, not execution speed. Do not write tests that fail because one strategy took a few milliseconds more than another.

---

# Part C — Benchmark runner

## 11. Task 4: Extend `BenchmarkRunner`

Modify `BenchmarkRunner` so that it can select the execution strategy from command-line arguments.

Use the following command contract:

```text
<strategy> <ipAddress> <simulateIo> <warmups> <measuredRuns> [poolSize]
```

Accepted strategy values:

```text
SEQUENTIAL
FIXED
VIRTUAL
```

Examples:

```bash
mvn exec:java -Dexec.args="SEQUENTIAL 202.24.34.55 true 2 5"
```

```bash
mvn exec:java -Dexec.args="FIXED 202.24.34.55 true 2 5 4"
```

```bash
mvn exec:java -Dexec.args="VIRTUAL 202.24.34.55 true 2 5"
```

The runner must:

- Validate the arguments.
- Instantiate the selected strategy.
- Execute the requested warm-up runs without including them in the results.
- Execute the requested measured runs.
- Verify that every measured run produces the expected functional result.
- Calculate minimum, maximum, and average elapsed time.
- Print the selected configuration.
- Print individual measured times.
- Print a summary suitable for copying into `results.csv`.

Recommended output fields:

```text
scenario,strategy,pool_size,run,elapsed_ms,matches,consulted_providers
```

Example row:

```text
IO,FIXED,4,1,2845.327,7,100
```

Do not use IDE timestamps or manually measured wall-clock time. Use the elapsed duration returned by the search implementation.

---

# Part D — Experimental comparison

## 12. Task 5: Benchmark methodology

Use the same computer for all measurements.

Before measuring:

- Close unnecessary applications.
- Connect the computer to power when possible.
- Avoid changing the source code between compared runs.
- Run `mvn clean test`.
- Record the execution environment.
- Use two warm-up executions.
- Use five measured executions.

Required experiment matrix:

| Scenario | Strategy | Threads or tasks |
|---|---|---:|
| Local, no simulated I/O | Sequential | 1 |
| Local, no simulated I/O | Fixed pool | 2 |
| Local, no simulated I/O | Fixed pool | 4 |
| Local, no simulated I/O | Fixed pool | 8 |
| Local, no simulated I/O | Virtual threads | 100 tasks |
| Simulated blocking I/O | Sequential | 1 |
| Simulated blocking I/O | Fixed pool | 2 |
| Simulated blocking I/O | Fixed pool | 4 |
| Simulated blocking I/O | Fixed pool | 8 |
| Simulated blocking I/O | Virtual threads | 100 tasks |

### Important interpretation

The scenario without simulated I/O performs a small local calculation. It is useful for observing coordination overhead, but it is not a complete representation of every CPU-bound workload.

The scenario with simulated I/O represents blocking calls such as network, database, or external-service requests.

Do not invent expected times. Performance depends on the execution environment.

---

## 13. Metrics

For every configuration, report:

- Average elapsed time in milliseconds.
- Minimum elapsed time.
- Maximum elapsed time.
- Number of matches.
- Number of consulted providers.
- Speedup relative to the sequential strategy in the same scenario.

Calculate speedup as:

```text
Speedup = sequential average time / strategy average time
```

Interpretation examples:

- `1.00`: no improvement relative to sequential execution.
- Greater than `1.00`: faster than the sequential baseline.
- Less than `1.00`: slower than the sequential baseline.

Do not compare a strategy executed with simulated I/O against a baseline executed without simulated I/O.

---

## 14. Required results table

Complete this table with actual measurements:

| Scenario | Strategy | Pool size | Average ms | Minimum ms | Maximum ms | Speedup | Matches | Consulted |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| No simulated I/O | Sequential | — | 0.020 | 0.013 | 0.037 | 1.00 | 7 | 100 |
| No simulated I/O | Fixed pool | 2 | 1.025 | 0.684 | 1.596 | 0.02 | 7 | 100 |
| No simulated I/O | Fixed pool | 4 | 1.035 | 0.540 | 1.383 | 0.02 | 7 | 100 |
| No simulated I/O | Fixed pool | 8 | 2.848 | 2.229 | 3.631 | 0.01 | 7 | 100 |
| No simulated I/O | Virtual threads | — | 1.117 | 0.722 | 2.318 | 0.02 | 7 | 100 |
| Simulated I/O | Sequential | — | 11038.810 | 11013.471 | 11065.869 | 1.00 | 7 | 100 |
| Simulated I/O | Fixed pool | 2 | 5524.569 | 5511.102 | 5566.279 | 2.00 | 7 | 100 |
| Simulated I/O | Fixed pool | 4 | 2828.639 | 2826.499 | 2830.948 | 3.90 | 7 | 100 |
| Simulated I/O | Fixed pool | 8 | 1469.871 | 1468.798 | 1472.166 | 7.51 | 7 | 100 |
| Simulated I/O | Virtual threads | — | 205.158 | 199.505 | 214.991 | 53.81 | 7 | 100 |

Also include the raw measurements in:

```text
results/results.csv
```

Suggested repository location:

```text
results/
├── results.csv
└── environment.md
```

---

# Part E — Analysis and architectural recommendation

## 15. Task 6: Required analysis

Answer every question with evidence from the experiment.

### 15.1 Correctness

1. How did the team verify that the three strategies produce equivalent results? 

Answer: BenchmarkRunner runs the search once at the start to get a reference result, then compares every warm-up and measured run against it. If matches or the count of consulted providers ever differ, it throws an exception right away. We also checked manually: all three strategies returned the exact same matches [10, 23, 36, 49, 62, 75, 88] and consulted all 100 providers, across every configuration we ran.

2. Why can concurrent tasks return matches in a different order?

Answer: Each provider is checked in its own task, and the thread scheduler decides which one finishes first — that depends on timing, not on submission order. Sequential execution always goes provider by provider, so it's the only one with a guaranteed order.

3. What mechanism or design prevented lost or duplicated matches?

Answer: Each task only handles one provider and returns one result at most. Nothing gets written to a shared list until all tasks are done and we collect the results with Future.get(), so there's no race condition. We also sort the final list before returning it, which keeps the order consistent no matter which task finished first.

4. Why should performance not be compared before proving functional equivalence?

Answer: A fast result that's wrong is worse than useless — it can look like a win while actually skipping work or losing data. That's why we check correctness on every run, not just once: speed only matters once we know the answer is right.

### 15.2 Fixed thread pool

5. What changed when the pool increased from 2 to 4 threads?

Answer: With simulated I/O, the average time went down from 5524.569 ms to 2828.639 ms, while the speedup increased from 2.00x to 3.90x. The main reason is that the providers spend most of their time waiting in Thread.sleep() instead of using the CPU. Increasing the pool from 2 to 4 threads allows more of these waiting tasks to run at the same time, which reduces the total execution time.

6. What changed when the pool increased from 4 to 8 threads?

Answer: The average time decreased again, from 2828.639 ms to 1469.871 ms, and the speedup increased from 3.90x to 7.51x. We can see a similar improvement to the previous step, from 2 to 4 threads. Since the test machine has 12 logical processors (see Section 19), using 8 threads is still reasonable and allows more tasks to run at the same time without creating too much competition for the CPU.

7. Was the improvement proportional to the number of threads? Explain.

Answer: For the simulated-I/O case, the improvement was fairly close to proportional. When the pool size was doubled from 2 to 4 and then from 4 to 8 threads, the execution time was roughly cut in half each time. This is because the workload is I/O-bound, so the threads spend most of their time waiting instead of using the CPU. Adding more threads allows more waiting tasks to happen at the same time. However, this doesn't happen in the no-I/O case. The results were Fixed(2) = 1.025 ms, Fixed(4) = 1.035 ms, and Fixed(8) = 2.848 ms. In this case, adding more threads actually made the program slower. Since there is no waiting to overlap and the actual work is just a small hash computation, the extra threads mainly add scheduling and coordination overhead.

8. What costs are introduced by task creation, scheduling, context switching, and result consolidation?

Answer: Using multiple threads introduces some additional overhead. Creating the `ExecutorService` and the `Callable` tasks requires object allocation, and the JVM and operating system have to schedule those tasks and assign them to the available threads. When too many threads are running at the same time, context switching can also add extra work for the system. After that, the program still has to collect the results using `Future.get()` and sort the final list. These costs become more noticeable in the no-I/O case because the actual work is very small. The hash computation finishes quickly, so the time spent creating, scheduling, and coordinating the tasks can be significant compared with the time spent doing the actual computation.

9. What would happen if the pool size were much larger than the available platform threads?

Answer: The performance would most likely get worse instead of better. Each platform thread requires operating system resources, including memory for its stack and other thread-related structures. If there are many more threads than available CPU cores, the operating system has to spend more time switching between them instead of doing useful work. If the number of threads becomes too large, the program can also run into resource limitations such as high memory usage. This is one of the reasons virtual threads are useful for applications with a large number of I/O-bound tasks, since they can handle many concurrent waiting operations with less overhead than creating the same number of platform threads.


### 15.3 Virtual threads

10. In which scenario did virtual threads provide the clearest benefit?

Answer: Virtual threads showed the biggest improvement in the simulated-I/O scenario. They achieved an average time of 205.158 ms, which is a 53.81x speedup compared with Sequential. This was also much better than the best fixed-pool result, which was Fixed(8) with a 7.51x speedup. The main reason is that the workload spends most of its time waiting. Virtual threads allow all 100 provider consultations to run concurrently without requiring 100 real operating system threads.

11. Why are virtual threads especially relevant for blocking operations?

Answer: Virtual threads are especially useful when a program has many blocking operations. When a virtual thread reaches something that makes it wait, such as Thread.sleep() or network I/O, the JVM can temporarily remove it from its carrier thread. That carrier thread can then be used to run another virtual thread instead of staying blocked. This makes it possible to have thousands of tasks waiting at the same time while using a much smaller number of real OS threads. This fits our workload well because most of the time is spent waiting for the simulated external service.

12. Why do virtual threads not make local CPU work automatically faster?

Answer: Virtual threads mainly help reduce the cost of waiting; they don't make CPU computations faster. CPU-bound work still needs to run on actual CPU cores, and changing the type of thread doesn't increase the number of available cores. In the results that we have is clearly in the no-I/O scenario, Virtual averaged 1.117 ms, while Sequential only took 0.020 ms. In this case, the work is just a small hash computation with no waiting, so the overhead of creating and scheduling the virtual threads is greater than the benefit of running the tasks concurrently.

13. What trade-offs remain even when virtual threads are lightweight?

Answer: Virtual threads are lightweight, but they still have some overhead for creation and scheduling. Because of this, very small CPU-bound tasks can still be faster when executed sequentially, as we saw in the no-I/O results. For larger CPU-bound workloads, virtual threads also cannot create more computing power than the machine actually has, since the work still has to run on a limited number of CPU cores.

### 15.4 Architectural decision

14. Which strategy would the team recommend for a system dominated by blocking external calls?
15. Which strategy would the team recommend for a small local workload?
16. Under what conditions would a fixed pool still be preferable?
17. What evidence from the measurements supports the recommendation?
18. What limitations prevent generalizing the conclusion to every production system?

Answers such as “virtual threads are better” or “more threads are faster” are insufficient without conditions and evidence.

---

## 16. Architectural conclusion

Write a team conclusion of 150 to 250 words.

The conclusion must include:

- The dominant workload characteristic.
- The measured evidence.
- The recommended strategy.
- The conditions under which the recommendation is valid.
- At least one trade-off.
- At least one limitation of the experiment.

### Team conclusion

> Replace this text with the team conclusion.

---

## 17. Individual conclusions

Each student must add an individual conclusion of 80 to 120 words.

### Student 1

**Name:** TOMAS OLAYA DIAZ 

> Durante este laboratorio implementé el BenchmarkRunner, agregando soporte para las tres estrategias secuencial, pool fijo y virtual threads, validación automática de equivalencia entre ejecuciones, y generación de datos en el formato CSV el cual se pedia en los requerimientos del laboratorio. Lo que más me sorprendió fue que, sin latencia simulada, las estrategias concurrentes resultaron más lentas que la secuencial es decir speedup menor a 1, porque el costo de crear y coordinar tareas superó el trabajo real, casi instantáneo. En cambio, con I/O simulado, los virtual threads alcanzaron un speedup de casi 54x frente a la secuencial, mientras que el pool fijo se estancó al pasar de 4 a 8 hilos. Esto me dejó claro que la concurrencia no siempre mejora el rendimiento: depende completamente de si el trabajo es de espera (I/O) o de cómputo local. 

### Student 2

**Name:** Isaac Burgos

> En este laboratorio implementé las estrategias concurrentes `FixedPoolBlackListSearch` y `VirtualThreadBlackListSearch`, ambas usando un `ExecutorService`: un pool fijo de hilos de plataforma en el primer caso, y `Executors.newVirtualThreadPerTaskExecutor()` en el segundo. El diseño evita estado compartido mutable, ya que cada tarea consulta un único proveedor y devuelve su propio resultado, así que no hizo falta sincronización explícita. Los resultados se ordenan al final para garantizar determinismo frente al orden no determinista de finalización de las tareas. Al verificar contra el baseline secuencial confirmé que las tres estrategias producen exactamente los mismos matches y consultan los 100 proveedores. Lo que más reforzó mi entendimiento fue ver, con datos reales, que sin I/O simulado la concurrencia resulta más lenta que la secuencial, porque el costo de crear y coordinar hilos supera un cálculo local casi instantáneo. Con I/O bloqueante, en cambio, los virtual threads llegaron a un speedup de casi 54x. Eso deja claro que la estrategia correcta depende del tipo de carga, no de una regla general.

### Student 3

**Name:** Javier Romero

> En este laboratorio implementé la suite de pruebas automatizadas que valida la equivalencia funcional entre las cinco estrategias (`Sequential`, `FixedPool` (2/4/8) y `Virtual Threads`), utilizando tanto la latencia simulada como sin ella. Cada test compara los `matchingProviderIds` y el conteo de los proveedores consultados contra la línea de base secuencial, además verifico la ausencia de duplicados, el orden ascendente y la validación de los argumentos inválidos. Aprendí a balancear los tamaños de los datos de pruebas (20 vs 100 proveedores) debido a que esto aumenta el tiempo en el que se ejecutan las pruebas, gracias a esto se puede confirmar que cada proveedor extra es una espera adicional, se ve cuando se hace `Threed.sleap()` en `IsBlacklisted`, entonces cambiar de 20 a 100 proveedores en el test de equivalencia con I/O se reduce el tiempo de la prueba sin reducir la capacidad de la prueba en detectar un bug de correción, porque esa capacidad que se cambia no depende la escala para este ejercicio.

---

# Part F — Submission

## 18. Required deliverables

The repository must contain:

- Functional sequential baseline.
- Functional fixed-thread-pool implementation.
- Functional virtual-thread implementation.
- Extended `BenchmarkRunner`.
- Automated tests.
- `results/results.csv`.
- `results/environment.md`.
- Completed results table.
- Answers to all analysis questions.
- Team architectural conclusion.
- Three individual conclusions.
- AI-use declaration.
- Meaningful Git history from all team members.

The repository must compile from a clean clone:

```bash
mvn clean test
```

---

## 19. Execution environment

Complete:

| Item | Value |
|---|---|
| Operating system | Windows 11 |
| CPU model | Intel Core i5 (13th Gen) |
| Logical processors | 12 |
| RAM | 8 GB |
| JDK vendor and version | Microsoft Build of OpenJDK 21.0.6 |
| Maven version | Apache Maven 3.9.16 |
| Measurement date | 2026-08-06 |
---

## 20. Team members and contribution evidence

| Student | GitHub username | Main contribution | Relevant commits |
|---|---|---|---|
| Tomas Olaya Diaz | iAxstral | Extended BenchmarkRunner to support SEQUENTIAL, FIXED and VIRTUAL strategies with warmups, equivalence validation and CSV output; executed the 10 mandatory benchmark configurations and documented results and environment. | 31a328b  add my conclusion of the lab
21798ba  add benchmark results and environment document
3ea7dd9  feat: implement BenchmarkRunner, equivalence validation and CSV output. |
| Pending | Pending | Pending | Pending |
| Pending | Pending | Pending | Pending |

Each student must have at least two meaningful commits.

Examples of meaningful commits:

```text
Implement fixed thread pool search
Add virtual-thread search strategy
Add equivalence and ordering tests
Extend benchmark runner and CSV output
Document benchmark analysis and trade-offs
```

Formatting-only changes, name changes, or typo corrections do not count as sufficient contribution evidence.

---

## 21. Final submission tag

After verifying the final version:

```bash
git status
mvn clean test
git tag -a lab-1-final -m "Laboratory 1 final submission"
git push origin lab-1-final
```

Submit the repository URL and confirm that the `lab-1-final` tag is available remotely.

---

# Part G — Grading rubric

## 22. Rubric

| Criterion | Weight | Maximum grade |
|---|---:|---:|
| Correctness and equivalence of results | 20% | 1.00 |
| Fixed-pool and virtual-thread implementations | 20% | 1.00 |
| Benchmark methodology and reproducibility | 25% | 1.25 |
| Analysis and architectural trade-offs | 25% | 1.25 |
| Repository, documentation, and individual traceability | 10% | 0.50 |
| **Total** | **100%** | **5.00** |

### 22.1 Correctness and equivalence — 1.00

Full credit requires:

- All strategies return equivalent matches.
- All mandatory strategies consult 100 providers.
- Results contain no duplicates.
- Results are deterministic and ordered.
- Automated tests pass.

### 22.2 Concurrent implementations — 1.00

Full credit requires:

- Correct use of a fixed `ExecutorService`.
- Correct use of Java 21 virtual threads.
- Proper executor lifecycle.
- Appropriate exception and interruption handling.
- No unsafe global state.
- No sequential delegation disguised as concurrency.

### 22.3 Benchmark methodology — 1.25

Full credit requires:

- All ten mandatory configurations.
- Two warm-ups and five measured executions.
- Same environment and baseline per scenario.
- Raw data and summary metrics.
- Reproducible commands.
- Correct speedup calculations.

### 22.4 Analysis and trade-offs — 1.25

Full credit requires:

- Evidence-based interpretation.
- Correct distinction between blocking and local work.
- Analysis of pool size.
- Analysis of virtual threads.
- Architectural recommendation with conditions.
- Explicit limitations and trade-offs.

### 22.5 Repository and traceability — 0.50

Full credit requires:

- Clear documentation.
- Clean repository structure.
- Meaningful contributions from all students.
- Complete AI-use declaration.
- Final submission tag.
- Successful execution from a clean clone.

---

## 23. Oral verification

Any team member may be selected to:

- Explain a section of the concurrent implementation.
- Describe how race conditions were avoided.
- Explain a benchmark result.
- Reproduce a command.
- Justify the architectural recommendation.
- Explain code produced or modified with AI assistance.

The individual grade may be adjusted when a student cannot demonstrate understanding or contribution.

---

## 24. Use of artificial intelligence

AI tools may be used as support, but every student must understand and defend the submitted work.

Complete the following table:

| Tool | Purpose | Main prompts or activities | Validation performed | Changes made by the team |
|---|---|---|---|---|
| Claude (Anthropic) | Support for implementing `BenchmarkRunner` (Task 4), understanding concurrency concepts, and troubleshooting Git/environment setup. | - Explained concurrency, parallelism and Amdahl's Law concepts from Week 1.<br>- Guided the extension of `BenchmarkRunner` to support SEQUENTIAL/FIXED/VIRTUAL strategies, warm-ups, equivalence validation and CSV output. | - Reviewed metrics for consistency with theory (e.g., speedup near pool size for FIXED, diminishing returns from pool-4 to pool-8). | Equivalence-validation logic and CSV format were checked against the README contract before acceptance. |

Requirements:

- Do not submit code that the team cannot explain.
- Validate generated code through tests and review.
- Record relevant AI assistance.
- Do not use AI output as a replacement for experimental evidence.
- Plagiarism or duplicated repository content is subject to the course academic-integrity rules.

---

# Optional extensions

These extensions do not replace any mandatory requirement.

## A. Early termination

Create a separate strategy that stops after finding five matches.

Analyze:

- Whether the final classification remains valid.
- Whether the complete evidence list is preserved.
- How pending tasks are cancelled.
- How many providers are actually consulted.
- What happens to tasks already running.
- How early termination changes comparability with the complete-scan benchmark.

Do not replace the mandatory complete-scan strategies with this extension.

## B. Five-minute cache

Add a cache with a five-minute TTL.

Analyze:

- Cache key.
- Thread safety.
- Expiration.
- Stale information.
- Cache hit ratio.
- Effect on elapsed time.
- Effect on correctness and freshness.

---

# Final checklist

Before submission, verify:

- [ ] The project uses Java 21.
- [ ] `mvn clean test` passes.
- [ ] Fixed pools of 2, 4, and 8 threads work.
- [ ] The virtual-thread strategy works.
- [ ] All mandatory strategies return equivalent results.
- [ ] Results are ordered and contain no duplicates.
- [ ] The benchmark runner supports the required arguments.
- [ ] Two warm-ups and five measured runs were executed.
- [ ] All ten required configurations were measured.
- [ ] `results/results.csv` contains raw measurements.
- [ ] The environment is documented.
- [ ] The results table is complete.
- [ ] All analysis questions are answered.
- [ ] The team conclusion is complete.
- [ ] Every student added an individual conclusion.
- [ ] Every student has meaningful commits.
- [ ] AI use is declared.
- [ ] The `lab-1-final` tag was pushed.
- [ ] The repository URL was submitted in the institutional platform.
