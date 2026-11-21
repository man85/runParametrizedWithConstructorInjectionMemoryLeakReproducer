# Memory leaks reproducer 

When there are @ParameterizedTest methods, the Spring context is not closed despite the @DirtiesContext annotation on the class.

Check with `./gradlew test`.

`BaseIntegrationTest::assertNoLeakedContexts` fails if a previous Spring context is still alive after GC.