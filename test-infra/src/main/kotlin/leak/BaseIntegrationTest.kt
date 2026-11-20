package leak


import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class TestConfig {
    @Bean
    open fun greeter() = Greeter()
}

class Greeter {
    fun greet() = "hello"
}

/**
 * Every test class gets its own ApplicationContext (@DirtiesContext AFTER_CLASS).
 * In @BeforeEach we register the current context in a WeakReference, run System.gc() x2
 * and verify that all contexts of FINISHED classes have already been collected.
 * If not — a leak: something still holds a strong reference.
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfig::class])
@DirtiesContext
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var applicationContext: ApplicationContext

    /**
     * WORKAROUND (DISABLED by default — with it the leak does not reproduce, the build is green):
     * the test instance outlives its class in a stale Test worker stack slot
     * (live in the oop map, but actually an unassigned reference local of a JUnit engine frame).
     * The instance itself cannot be unpinned, but the instance → context link can be broken:
     * we null out the field in @AfterEach — the context becomes collectible even if the instance stays pinned.
     *
     * To enable: replace the field above with
     *   `@Autowired protected var applicationContext: ApplicationContext? = null`,
     * and in checkForLeaks below — with `assertNoLeakedContexts(applicationContext!!)`,
     * then uncomment the method:
     *
     * @AfterEach
     * fun breakInstanceToContextLink() {
     *     applicationContext = null
     * }
     */

    companion object {
        private val seen = ConcurrentHashMap<Int, WeakReference<ApplicationContext>>()

        @JvmStatic
        protected fun gc() {
            System.gc()
            System.gc()
            Thread.sleep(100)
        }

        @JvmStatic
        protected fun assertNoLeakedContexts(current: ApplicationContext) {
            seen[System.identityHashCode(current)] = WeakReference(current)
            gc()
            val leaked = seen.entries
                .filter { it.key != System.identityHashCode(current) }
                .mapNotNull { it.value.get() }
            check(leaked.isEmpty()) {
                "REPRODUCED: closed ApplicationContexts are held by a strong reference after GC: ${leaked.size} item(s) " +
                    "(this is exactly the memory leak between test classes)"
            }
        }
    }

    @BeforeEach
    fun checkForLeaks(testInfo: TestInfo) {
        assertNoLeakedContexts(applicationContext)
    }
}
