# ApplicationContext leak between test classes (JUnit 5 + kotlinx-coroutines-test + Spring)

## What is reproduced

After a test class finishes, its **closed** `ApplicationContext` remains
**strongly reachable** after `System.gc()` — GC does not collect it. On a full test
run (hundreds of classes with `@DirtiesContext`) this accumulates hundreds of MB of heap.

## Minimal recipe

All of the following are required simultaneously:

1. **JUnit 5 `@ParameterizedTest`** (multiple invocations, multiple groups);
2. test body — **`kotlinx.coroutines.test.runTest { ... }`**;
3. a test class with **Spring constructor injection** (`SpringExtension` resolves constructor parameters);
4. Spring `@DirtiesContext` (the context is closed after the class);
5. a following test class (the leak is detected there).

Remove any of the items — the leak disappears (verified by feature bisection):
- without `runTest` — clean;
- a single `@Test` instead of parameterized ones — clean;
- without constructor injection — clean;
- 2 invocations instead of 17 — clean (threshold).

## Mechanism (from heap dump, MAT path2gc)

```
GC root: Test worker (Java Local — live stack slot)
  → finished instance of the test class (applicationContext field)
    → closed AnnotationConfigReactiveWebServerApplicationContext (24 MB retained)
```

The finished test instance outlives its class in a **stale stack slot** of
Test worker (the value is semantically dead, but remains in the oop map of a live frame),
and through its `@Autowired ApplicationContext` it holds the entire closed context.
The only path from the GC root is this slot (merge_shortest_paths), there are no heap paths.

## How to run

```bash
./gradlew test
```

Expected result — `BUILD FAILED`, in the test report:

```
java.lang.IllegalStateException: REPRODUCED: closed ApplicationContexts
are held by a strong reference after GC: 1 item.
```

Reproduces reliably (6/6 runs). The leak is nondeterministic by nature
(a stale stack slot), so the detecting class is parameterized too —
this makes reproduction stable.

## Detector

`test-infra/src/main/kotlin/leak/BaseIntegrationTest.kt`: every `@BeforeEach`
registers the current context in a `WeakReference`, runs `System.gc()` twice and
fails if any context of a finished class is still strongly reachable.

## Environment

- Gradle 9.3.1 (Test worker, `useJUnitPlatform()`)
- JUnit Jupiter 5.14.4
- kotlinx-coroutines-test 1.10.2
- Spring Framework 6.2.11 (spring-test/spring-context)
- JDK 21+

## Root cause (from bisection and MAT data)

The finished test instance outlives its class in a **Test worker stack slot**: a live frame
of the JUnit engine has a reference local that is considered live by the oop map, but is actually unassigned
(or semantically dead) — the slot physically retains a reference to the instance of the previous class.
Through the `@Autowired applicationContext` field, the instance holds the entire closed context.

Experimentally ruled out: Gradle (8.14.1 and 9.3.1 leak identically — not a 9.x regression),
`TimeoutExtension` (leaks without `junit.jupiter.execution.timeout.default`), the Spring context cache
(the context is really closed, `closed=1`), heap paths (merge_shortest_paths — the only root:
Test worker, Java Local).

Provoked by the stack shape: parameterized invocations + a blocking coroutine event loop
(`runTest` OR `runBlocking` — both leak) + constructor injection. Remove any one — clean.

The exact culprit frame can only be attributed in MAT GUI (per-frame locals); headless CLI
only gives "Java Local" without a frame.

## Where to file the issue

**junit-team/junit5** — the test instance being retained by a live engine frame after the class has finished
(the instance is created and passed around as a local by engine frames; Gradle/Spring are ruled out).
If the analysis shows the slot belongs to a `runBlocking`/`joinBlocking` frame —
then kotlinx/kotlinx-coroutines.

## Workaround (commented out in `BaseIntegrationTest`)

Break the instance → context link: null out `applicationContext` in `@AfterEach`.
The instance remains pinned by the stack slot, but the context becomes collectible.
By default the workaround is DISABLED (the reproducer fails with REPRODUCED); instructions for enabling it are
in the `WORKAROUND` comment in `BaseIntegrationTest.kt`. With the workaround: 4/4 green; without: 4/4 REPRODUCED.
