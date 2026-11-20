package app

import leak.BaseIntegrationTest
import leak.Greeter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

/**
 * Minimal "leaking" test: parameterized methods, each body is runTest,
 * a class with constructor injection of a Spring bean.
 * After the class finishes, its closed ApplicationContext remains strongly reachable.
 */
class LeakingTest(
    @Autowired val greeter: Greeter,
) : BaseIntegrationTest() {

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    fun `group one`(i: Int) = runTest {
        check(greeter.greet() == "hello" && i > 0)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30])
    fun `group two`(i: Int) = runTest {
        check(greeter.greet() == "hello" && i > 0)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2])
    fun `group three`(i: Int) = runTest {
        check(greeter.greet() == "hello" && i > 0)
    }

    @ParameterizedTest
    @ValueSource(ints = [1])
    fun `group four`(i: Int) = runTest {
        check(greeter.greet() == "hello" && i > 0)
    }
}
