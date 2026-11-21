package app


import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

@SpringBootTest
@ContextConfiguration(classes = [BaseIntegrationTest.SomeConfig::class])
@DirtiesContext
abstract class BaseIntegrationTest {

    open class SomeConfig

    @Autowired
    protected lateinit var applicationContext: ApplicationContext

    companion object {
        private val contexts = ConcurrentHashMap<Int, WeakReference<ApplicationContext>>()

        @JvmStatic
        protected fun gc() {
            System.gc()
            System.gc()
            Thread.sleep(100)
        }

        @JvmStatic
        protected fun assertNoLeakedContexts(currentContext: ApplicationContext) {
            contexts[System.identityHashCode(currentContext)] = WeakReference(currentContext)
            gc()
            val leaked = contexts.entries
                .filter { it.key != System.identityHashCode(currentContext) }
                .mapNotNull { it.value.get() }
            check(leaked.isEmpty()) {
                "REPRODUCED: closed ApplicationContexts are held by a strong reference after GC: ${leaked.size} item(s) " +
                        "(this is exactly the memory leak between test classes)"
            }
        }
    }

    @BeforeEach
    fun checkForLeaks() {
        assertNoLeakedContexts(applicationContext)
    }
}
