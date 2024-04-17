package dev.kviklet.kviklet.security

import dev.kviklet.kviklet.TestFixtures.createDatasourceConnectionRequest
import dev.kviklet.kviklet.controller.ConnectionController
import dev.kviklet.kviklet.db.ConnectionRepository
import dev.kviklet.kviklet.service.dto.ConnectionId
import dev.kviklet.kviklet.service.dto.Policy
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import java.util.stream.Stream

@ActiveProfiles("test")
class DatasourceConnectionSecurityTest(
    @Autowired val connectionRepository: ConnectionRepository,
    @Autowired val datasourceConnectionController: ConnectionController,
) : SecurityTestBase() {

    @BeforeEach
    fun setUp() {
        asAdmin {
            datasourceConnectionController.createConnection(
                createDatasourceConnectionRequest("db1-conn1"),
            )
            datasourceConnectionController.createConnection(
                createDatasourceConnectionRequest("db1-conn2"),
            )
            datasourceConnectionController.createConnection(
                createDatasourceConnectionRequest("db2-conn1"),
            )
            datasourceConnectionController.createConnection(
                createDatasourceConnectionRequest("db2-conn2"),
            )
        }
    }

    @AfterEach
    fun tearDown() {
        connectionRepository.deleteAllInBatch()
    }

    @ParameterizedTest
    @MethodSource
    fun testUpdateSuccessful(policies: List<Policy>) {
        val request = dev.kviklet.kviklet.controller.UpdateDatasourceConnectionRequest(
            displayName = "new name",
        )

        val response = mockMvc.perform(
            patch("/connections/db1-conn1").content(request).withContext(policies),
        )
            .andExpect(status().isOk).andReturn().parse<dev.kviklet.kviklet.controller.DatasourceConnectionResponse>()

        response.id shouldBe ConnectionId("db1-conn1")
        response.displayName shouldBe "new name"

        connectionRepository.findById("db1-conn1").get().displayName shouldBe "new name"
    }

    @ParameterizedTest
    @MethodSource
    fun testUpdateForbidden(policies: List<Policy>) {
        val request = dev.kviklet.kviklet.controller.UpdateDatasourceConnectionRequest(
            displayName = "new name",
        )

        mockMvc.perform(patch("/connections/db1-conn1").content(request).withContext(policies))
            .andExpect(status().isForbidden)

        connectionRepository.findById("db1-conn1").get().displayName shouldBe "display name"
    }

    @ParameterizedTest
    @MethodSource
    fun testCreateDatasourceConnectionSuccessful(policies: List<Policy>) {
        val request = createDatasourceConnectionRequest("db1-conn3", displayName = "new name")
        mockMvc.perform(post("/connections/").content(request).withContext(policies))
            .andExpect(status().isOk)

        connectionRepository.findById("db1-conn3").get().displayName shouldBe "new name"
    }

    @ParameterizedTest
    @MethodSource
    fun testCreateDatasourceConnectionForbidden(policies: List<Policy>) {
        val request = createDatasourceConnectionRequest("db1-conn3")
        mockMvc.perform(post("/connections/").content(request).withContext(policies))
            .andExpect(status().isForbidden)

        connectionRepository.findById("db1-conn3") shouldBe Optional.empty()
    }

    companion object {
        @JvmStatic
        fun testCreateDatasourceConnectionSuccessful(): Stream<List<Policy>> = Stream.of(
            listOf(
                allow("*", "*"),
            ),
            listOf(
                allow("datasource_connection:create", "db1-conn3"),
                allow("datasource_connection:get", "db1-conn3"),
            ),
            listOf(
                allow("datasource_connection:create", "db1-*"),
                allow("datasource_connection:get", "db1-*"),
            ),
        )

        @JvmStatic
        fun testCreateDatasourceConnectionForbidden(): Stream<List<Policy>> = Stream.of(
            listOf(
                allow("datasource_connection:create", "db1-conn3"),
            ),
            listOf(
                allow("datasource_connection:get", "db1-conn3"),
                allow("datasource:get", "db1"),
            ),
        )

        @JvmStatic
        fun testUpdateSuccessful(): Stream<List<Policy>> = Stream.of(
            listOf(
                allow("datasource_connection:edit", "db1-conn1"),
                allow("datasource_connection:get", "db1-conn1"),
                allow("datasource:get", "db1"),
            ),
            listOf(allow("datasource_connection:*", "*"), allow("datasource:*", "*")),
        )

        @JvmStatic
        fun testUpdateForbidden(): Stream<List<Policy>> = Stream.of(
            listOf(
                allow("datasource_connection:get", "db1-conn1"),
            ),
            listOf(
                allow("datasource_connection:edit", "db1-conn1"),
            ),
            listOf(
                allow("datasource_connection:edit", "db1-conn2"),
                allow("datasource_connection:get", "db1-conn1"),
            ),
            listOf(
                allow("datasource_connection:edit", "db1-conn1"),
                allow("datasource_connection:get", "db1-conn2"),
            ),
        )

        @JvmStatic
        fun testGetSuccessful(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(allow("datasource_connection:get", "*"), allow("datasource:get", "foo")), 0),
            Arguments.of(listOf(allow("datasource_connection:get", "*"), allow("datasource:get", "db1")), 2),
            Arguments.of(listOf(allow("datasource_connection:get", "db2-*"), allow("datasource:get", "db1")), 0),
            Arguments.of(listOf(allow("datasource_connection:get", "db1-*"), allow("datasource:get", "db1")), 2),
            Arguments.of(listOf(allow("datasource_connection:get", "db1-conn1"), allow("datasource:get", "db1")), 1),
            Arguments.of(
                listOf(
                    allow("datasource_connection:get", "db1-conn1"),
                    allow("datasource_connection:get", "db1-conn2"),
                    allow("datasource:get", "db1"),
                ),
                2,
            ),
            Arguments.of(listOf(allow("datasource_connection:get", "*"), allow("datasource:get", "*")), 4),
            Arguments.of(listOf(allow("datasource_connection:get", "db*"), allow("datasource:get", "db*")), 4),
        )

        @JvmStatic
        fun testGetForbidden(): Stream<List<Policy>> = Stream.of(
            listOf(allow("datasource_connection:get", "*")),
            listOf(allow("datasource:get", "*")),
        )
    }
}
