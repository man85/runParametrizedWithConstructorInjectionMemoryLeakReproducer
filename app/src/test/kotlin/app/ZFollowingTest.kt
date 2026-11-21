package app

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ZFollowingTest : BaseIntegrationTest() {

    @ParameterizedTest
    @ValueSource(ints = [1])
    fun `second test`() {
    }
}
