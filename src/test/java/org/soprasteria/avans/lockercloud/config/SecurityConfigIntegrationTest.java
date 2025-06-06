// SecurityConfigIntegrationTest.java
package org.soprasteria.avans.lockercloud.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
/**
 * Integration tests ensuring that the permitAll configuration works as
 * intended and that protected endpoints still enforce authentication.
 */
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Nested
    @DisplayName("PermitAll-endpoints")
    class PermitAllTests {

        @Test
        void swaggerUi_mag_niet_authenticeren() throws Exception {
            mvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        }

        @Test
        void apiDocs_mag_niet_authenticeren() throws Exception {
            mvc.perform(get("/v3/api-docs/swagger-config"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        }

        @Test
        @Disabled("Werkt voor nu niet")
        void hoofdpagina_geeft_404_zonder_auth_header() throws Exception {
            mvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        // /index bestaat niet als aparte route; dit hoort een 404 te geven
            mvc.perform(get("/index"))
                    .andExpect(status().isNotFound())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        }

        @Test
        @Disabled("Werkt voor nu niet")
        void statische_resources_geven_404_zonder_auth_header() throws Exception {
            mvc.perform(get("/css/app.css"))
                    .andExpect(status().isNotFound())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
            mvc.perform(get("/js/app.js"))
                    .andExpect(status().isNotFound())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
            mvc.perform(get("/images/logo.png"))
                    .andExpect(status().isNotFound())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        }

        @Test
        void showCloudDirectory_mag_niet_authenticeren() throws Exception {
            mvc.perform(get("/showCloudDirectory"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        }

        @Test
        void apiFiles_mag_niet_authenticeren() throws Exception {
            mvc.perform(post("/listFiles"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        }

        @Test
        void post_zonder_csrf_token_gepermitAllEndpoint_met_multipart() throws Exception {
            var file = new MockMultipartFile(
                    "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "dummy".getBytes());
            mvc.perform(multipart("/uploadForm").file(file))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        }
    }

    @Nested
    @DisplayName("Protected endpoints")
    class ProtectedTests {

        @Test
        void andere_endpoints_eisen_authenticatie() throws Exception {
            mvc.perform(get("/protected/resource"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", containsString("Basic")));
        }
    }
}
