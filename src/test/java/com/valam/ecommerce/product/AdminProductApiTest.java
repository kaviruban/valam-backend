package com.valam.ecommerce.product;

import com.valam.ecommerce.product.model.Category;
import com.valam.ecommerce.product.repository.CategoryRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the admin panel's product and category flows against the real database; each test rolls back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminProductApiTest {

    @TempDir
    static Path uploadDir;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("app.storage.local-dir", () -> uploadDir.toString());
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryRepository categories;

    @Autowired
    JsonMapper jsonMapper;

    long snacksId;

    @BeforeEach
    void setUp() {
        snacksId = categories.findAll().stream()
                .filter(c -> c.getName().equals("Snacks"))
                .map(Category::getId)
                .findFirst().orElseThrow();
    }

    private String product(String name, String price, String mrp, int stock, String extra) {
        return """
                {"name": "%s", "categoryId": %d, %s
                 "variants": [{"quantity": 250, "unit": "G", "price": %s, "mrp": %s, "stockQuantity": %d}]}
                """.formatted(name, snacksId, extra, price, mrp, stock);
    }

    private ResultActions create(String body) throws Exception {
        return mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createdId(String body) throws Exception {
        String location = create(body).andExpect(status().isCreated()).andReturn()
                .getResponse().getHeader(HttpHeaders.LOCATION);
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void dropdownListsSeededCategoriesInOrder() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Food Products", "Snacks", "Powder Products",
                        "Cow Dung Products", "Traditional Seeds", "Everyday Essential Products")))
                .andExpect(jsonPath("$[0].allowedUnits", contains("G", "KG")));
    }

    @Test
    void createsProductWithFullDetails() throws Exception {
        String extra = """
                "shortDescription": "Crunchy rice murukku", "description": "Made the traditional way.",
                "images": [{"url": "https://cdn.example.com/m1.jpg", "altText": "Murukku pack"},
                           {"url": "https://cdn.example.com/m2.jpg"}],
                "highlights": ["No palm oil", "Hand made"], "ingredients": "Rice flour, urad dal, salt",
                "shelfLife": "45 days", "hsnCode": "1905", "gstRate": 5, "featured": true,
                "metaTitle": "Rice Murukku", "metaDescription": "Traditional rice murukku",
                """;
        create(product("Rice Murukku", "85", "100", 12, extra))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("http://localhost/api/v1/admin/products/")))
                .andExpect(jsonPath("$.slug").value("rice-murukku"))
                .andExpect(jsonPath("$.categoryName").value("Snacks"))
                .andExpect(jsonPath("$.measureType").value("WEIGHT"))
                .andExpect(jsonPath("$.thumbnailUrl").value("https://cdn.example.com/m1.jpg"))
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.highlights", contains("No palm oil", "Hand made")))
                .andExpect(jsonPath("$.countryOfOrigin").value("India"))
                .andExpect(jsonPath("$.gstRate").value(5))
                .andExpect(jsonPath("$.featured").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.minPrice").value(85))
                .andExpect(jsonPath("$.totalStock").value(12))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.productCode", matchesPattern("PRD-\\d{6}")))
                .andExpect(jsonPath("$.variants[0].isbn", matchesPattern("200\\d{10}")))
                .andExpect(jsonPath("$.variants[0].label").value("250 g"))
                .andExpect(jsonPath("$.variants[0].mrp").value(100))
                .andExpect(jsonPath("$.variants[0].discountPercent").value(15));
    }

    @Test
    void sameNameGetsUniqueSlug() throws Exception {
        create(product("Kara Sev", "50", "null", 1, "")).andExpect(jsonPath("$.slug").value("kara-sev"));
        create(product("Kara Sev", "50", "null", 1, "")).andExpect(jsonPath("$.slug").value("kara-sev-2"));
    }

    @Test
    void rejectsInvalidProducts() throws Exception {
        create(product("Bad MRP", "120", "100", 1, "")).andExpect(status().isBadRequest());
        create(product("Bad slug", "10", "null", 1, "\"slug\": \"Not A Slug\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.slug").exists());
        create("""
                {"name": "Wrong unit", "categoryId": %d,
                 "variants": [{"quantity": 1, "unit": "L", "price": 10, "stockQuantity": 1}]}
                """.formatted(snacksId)).andExpect(status().isBadRequest());
    }

    @Test
    void generatesUniqueIdentifiersAndKeepsThemOnUpdate() throws Exception {
        String body = """
                {"name": "Mysore Pak", "categoryId": %d,
                 "variants": [{"quantity": 250, "unit": "G", "price": 120, "stockQuantity": 4},
                              {"quantity": 500, "unit": "G", "price": 230, "stockQuantity": 2}]}
                """.formatted(snacksId);
        JsonNode first = json(create(body).andExpect(status().isCreated()));
        JsonNode second = json(create(body).andExpect(status().isCreated()));

        String code = first.get("productCode").asString();
        String isbn = first.get("variants").get(0).get("isbn").asString();
        assertThat(code).matches("PRD-\\d{6}").isNotEqualTo(second.get("productCode").asString());
        assertThat(isbn).matches("200\\d{10}").satisfies(AdminProductApiTest::hasValidEan13CheckDigit);
        Set<String> isbns = Stream.of(first, second)
                .flatMap(p -> p.get("variants").valueStream())
                .map(v -> v.get("isbn").asString())
                .collect(Collectors.toSet());
        assertThat(isbns).hasSize(4);

        // Identifiers sent by the client are ignored, and an updated variant keeps its ISBN.
        String update = """
                {"name": "Mysore Pak Ghee", "categoryId": %d, "productCode": "PRD-999999",
                 "variants": [{"id": %d, "isbn": "0000000000000", "quantity": 250, "unit": "G", "price": 130,
                               "stockQuantity": 4}]}
                """.formatted(snacksId, first.get("variants").get(0).get("id").asLong());
        mvc.perform(put("/api/v1/admin/products/" + first.get("id").asLong())
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value(code))
                .andExpect(jsonPath("$.variants[0].isbn").value(isbn));
    }

    private static void hasValidEan13CheckDigit(String code) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (code.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        assertThat(code.charAt(12) - '0').isEqualTo((10 - sum % 10) % 10);
    }

    private JsonNode json(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void listsWithFiltersAndSearch() throws Exception {
        long pakodaId = createdId(product("Ribbon Pakoda", "60", "null", 0, ""));
        createdId(product("Mixture", "70", "null", 5, ""));

        mvc.perform(get("/api/v1/admin/products").param("categoryId", String.valueOf(snacksId))
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", contains("Mixture", "Ribbon Pakoda")));
        JsonNode pakoda = json(mvc.perform(get("/api/v1/admin/products/" + pakodaId)));
        mvc.perform(get("/api/v1/admin/products").param("q", pakoda.get("productCode").asString().toLowerCase()))
                .andExpect(jsonPath("$.content[*].name", contains("Ribbon Pakoda")));
        mvc.perform(get("/api/v1/admin/products").param("q", pakoda.get("variants").get(0).get("isbn").asString()))
                .andExpect(jsonPath("$.content[*].name", contains("Ribbon Pakoda")));
        mvc.perform(get("/api/v1/admin/products").param("q", "pakoda").param("inStock", "false"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/admin/products").param("sort", "price,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesWithVersionCheck() throws Exception {
        long id = createdId(product("Thattai", "40", "null", 3, "\"highlights\": [\"Crispy\"],"));

        String update = """
                {"name": "Thattai Special", "categoryId": %d, "version": %d, "slug": "thattai-special",
                 "images": [{"url": "https://cdn.example.com/t.jpg"}],
                 "variants": [{"quantity": 0.5, "unit": "KG", "price": 75, "stockQuantity": 8}]}
                """;
        mvc.perform(put("/api/v1/admin/products/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(update.formatted(snacksId, 7)))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/v1/admin/products/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(update.formatted(snacksId, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thattai Special"))
                .andExpect(jsonPath("$.slug").value("thattai-special"))
                .andExpect(jsonPath("$.highlights", hasSize(0)))
                .andExpect(jsonPath("$.thumbnailUrl").value("https://cdn.example.com/t.jpg"))
                .andExpect(jsonPath("$.variants", hasSize(1)))
                .andExpect(jsonPath("$.variants[0].label").value("500 g"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void deletesProductAndProtectsUsedCategory() throws Exception {
        long id = createdId(product("Seedai", "45", "null", 2, ""));

        mvc.perform(delete("/api/v1/admin/categories/" + snacksId)).andExpect(status().isBadRequest());

        mvc.perform(delete("/api/v1/admin/products/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/products/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void adminCanAddCategory() throws Exception {
        mvc.perform(post("/api/v1/admin/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Oils\", \"measureType\": \"VOLUME\", \"sortOrder\": 70}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allowedUnits", contains("ML", "L")));
        mvc.perform(post("/api/v1/admin/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"snacks\", \"measureType\": \"WEIGHT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadsImagesAndRejectsOtherFiles() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};
        String url = mvc.perform(multipart("/api/v1/admin/uploads/images")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", png)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", startsWith("http://localhost/uploads/images/")))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mvc.perform(get(url.replace("http://localhost", ""))).andExpect(status().isOk());

        mvc.perform(multipart("/api/v1/admin/uploads/images")
                        .file(new MockMultipartFile("file", "evil.png", "image/png", "<script>".getBytes())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowsAdminPanelOrigin() throws Exception {
        mvc.perform(options("/api/v1/admin/products")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
