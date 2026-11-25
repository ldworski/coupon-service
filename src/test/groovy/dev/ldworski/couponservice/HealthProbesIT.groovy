package dev.ldworski.couponservice

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration)
class HealthProbesIT extends IntegrationTestSpecification {

    @Autowired
    MockMvc mockMvc

    def "should report #probe probe as UP"() {
        when:
        def result = mockMvc.perform(get("/actuator/health/${probe}"))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.status').value("UP"))

        where:
        probe << ["liveness", "readiness"]
    }
}
