package app

import leak.BaseIntegrationTest
import leak.Greeter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

/**
 * The class that runs AFTER LeakingTest: each of its @BeforeEach checks
 * that the contexts of all finished classes have been collected by GC.
 */
class ZFollowingTest(
    @Autowired val greeter: Greeter,
) : BaseIntegrationTest() {

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4])
    fun `context of previous class must be collected`(i: Int) = runTest {
        check(greeter.greet() == "hello" && i > 0)
    }
}
