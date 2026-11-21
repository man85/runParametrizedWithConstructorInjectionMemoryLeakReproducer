package app

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LeakingTest : BaseIntegrationTest() {

    @ParameterizedTest
    @ValueSource(ints = [1])
    fun `first test`() {
    }
}
