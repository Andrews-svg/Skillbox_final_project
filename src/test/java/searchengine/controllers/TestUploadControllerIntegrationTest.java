//package searchengine.controllers;
//
//import com.example.searchengine.Application;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
//
//import java.nio.charset.StandardCharsets;
//
//
//@SpringBootTest(classes = Application.class)
//@AutoConfigureMockMvc
//public class TestUploadControllerIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Test
//    void shouldSuccessfullyUploadAFile() throws Exception {
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.txt",
//                "text/plain",
//                "Hello World!".getBytes(StandardCharsets.UTF_8));
//
//        mockMvc.perform(MockMvcRequestBuilders
//                        .multipart("/test/upload")
//                        .file(file))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.content()
//                        .string("Файл успешно загружен."));
//    }
//
//    @Test
//    void shouldReturnBadRequestForEmptyFile() throws Exception {
//        mockMvc.perform(MockMvcRequestBuilders
//                        .multipart("/test/upload"))
//                .andExpect(MockMvcResultMatchers.status().isBadRequest());
//    }
//}