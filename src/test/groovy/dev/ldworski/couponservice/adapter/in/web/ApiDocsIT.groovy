package dev.ldworski.couponservice.adapter.in.web

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import dev.ldworski.couponservice.IntegrationTestSpecification
import dev.ldworski.couponservice.configuration.TestcontainersConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration)
class ApiDocsIT extends IntegrationTestSpecification {

    @Autowired
    MockMvc mockMvc

    def "should expose OpenAPI documentation"() {
        when:
        def result = mockMvc.perform(get("/v3/api-docs"))

        then:
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
    }
}
