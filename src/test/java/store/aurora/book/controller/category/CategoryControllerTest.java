package store.aurora.book.controller.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import store.aurora.book.dto.category.CategoryRequestDTO;
import store.aurora.book.dto.category.CategoryResponseDTO;
import store.aurora.book.service.category.CategoryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("모든 카테고리 조회 테스트 (GET /api/categories)")
    void getCategoriesTest() throws Exception {
        // given
        List<CategoryResponseDTO> mockCategoryList = List.of(
                new CategoryResponseDTO(1L, "카테고리 1"),
                new CategoryResponseDTO(2L, "카테고리 2"),
                new CategoryResponseDTO(3L, "카테고리 3")
        );

        when(categoryService.getCategories()).thenReturn(mockCategoryList);

        // when & then
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("카테고리 1"));

        verify(categoryService, times(1)).getCategories();
    }

    @Test
    @DisplayName("최상위 카테고리 조회 테스트 - 페이징 (GET /api/categories/root)")
    void getRootCategoriesTest() throws Exception {
        // given
        List<CategoryResponseDTO> mockRootCategories = List.of(
                new CategoryResponseDTO(1L, "최상위 카테고리 1"),
                new CategoryResponseDTO(2L, "최상위 카테고리 2"),
                new CategoryResponseDTO(3L, "최상위 카테고리 3"),
                new CategoryResponseDTO(4L, "최상위 카테고리 4"),
                new CategoryResponseDTO(5L, "최상위 카테고리 5"),
                new CategoryResponseDTO(6L, "최상위 카테고리 6") // 2페이지로 넘어감
        );
        Page<CategoryResponseDTO> mockPage = new PageImpl<>(mockRootCategories.subList(0, 5), PageRequest.of(0, 5), 6);

        when(categoryService.getRootCategories(any(PageRequest.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/api/categories/root")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1L));

        verify(categoryService, times(1)).getRootCategories(any(PageRequest.class));
    }

    @Test
    @DisplayName("하위 카테고리 조회 테스트 - 페이징 (GET /api/categories/{parent-id}/children)")
    void getChildrenCategoriesTest() throws Exception {
        // given
        Long parentId = 1L;
        List<CategoryResponseDTO> mockChildrenCategories = List.of(
                new CategoryResponseDTO(10L, "하위 카테고리 1"),
                new CategoryResponseDTO(11L, "하위 카테고리 2"),
                new CategoryResponseDTO(12L, "하위 카테고리 3"),
                new CategoryResponseDTO(13L, "하위 카테고리 4"),
                new CategoryResponseDTO(14L, "하위 카테고리 5"),
                new CategoryResponseDTO(15L, "하위 카테고리 6") // 2페이지로 넘어감
        );
        Page<CategoryResponseDTO> mockPage = new PageImpl<>(mockChildrenCategories.subList(0, 5), PageRequest.of(0, 5), 6);

        when(categoryService.getChildrenCategories(eq(parentId), any(PageRequest.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/api/categories/{parent-id}/children", parentId)
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(10L));

        verify(categoryService, times(1)).getChildrenCategories(eq(parentId), any(PageRequest.class));
    }

    @Test
    @DisplayName("카테고리 생성 테스트 (POST /api/categories)")
    void createCategoryTest() throws Exception {
        // given
        CategoryRequestDTO requestDto = new CategoryRequestDTO(1L,"새 카테고리");
        doNothing().when(categoryService).createCategory(any(CategoryRequestDTO.class));

        // when & then
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        verify(categoryService, times(1)).createCategory(any(CategoryRequestDTO.class));
    }

    @Test
    @DisplayName("카테고리 수정 테스트 (PATCH /api/categories/{category-id})")
    void updateCategoryTest() throws Exception {
        // given
        Long categoryId = 1L;
        CategoryRequestDTO requestDto = new CategoryRequestDTO(null,"수정된 카테고리");

        doNothing().when(categoryService).updateCategory(eq(categoryId), any());

        // when & then
        mockMvc.perform(patch("/api/categories/{category-id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).updateCategory(eq(categoryId), any());
    }

    @Test
    @DisplayName("카테고리 삭제 테스트 (DELETE /api/categories/{category-id})")
    void deleteCategoryTest() throws Exception {
        // given
        Long categoryId = 1L;
        doNothing().when(categoryService).deleteCategory(categoryId);

        // when & then
        mockMvc.perform(delete("/api/categories/{category-id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory(categoryId);
    }

    @Test
    @DisplayName("최상위 카테고리 전체 조회 테스트 (GET /api/categories/root/all)")
    void getAllRootCategoriesTest() throws Exception {
        // given
        List<CategoryResponseDTO> mockRootCategories = List.of(
                new CategoryResponseDTO(1L, "최상위 카테고리 1"),
                new CategoryResponseDTO(2L, "최상위 카테고리 2"),
                new CategoryResponseDTO(3L, "최상위 카테고리 3")
        );

        when(categoryService.getAllRootCategories()).thenReturn(mockRootCategories);

        // when & then
        mockMvc.perform(get("/api/categories/root/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))) // 총 3개 카테고리 확인
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("최상위 카테고리 1"));

        verify(categoryService, times(1)).getAllRootCategories();
    }

    @Test
    @DisplayName("하위 카테고리 전체 조회 테스트 (GET /api/categories/{parent-id}/children/all)")
    void getAllChildrenCategoriesTest() throws Exception {
        // given
        Long parentId = 1L;
        List<CategoryResponseDTO> mockChildrenCategories = List.of(
                new CategoryResponseDTO(10L, "하위 카테고리 1"),
                new CategoryResponseDTO(11L, "하위 카테고리 2"),
                new CategoryResponseDTO(12L, "하위 카테고리 3")
        );

        when(categoryService.getAllChildrenCategories(parentId)).thenReturn(mockChildrenCategories);

        // when & then
        mockMvc.perform(get("/api/categories/{parent-id}/children/all", parentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))) // 총 3개 하위 카테고리 확인
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].name").value("하위 카테고리 1"));

        verify(categoryService, times(1)).getAllChildrenCategories(parentId);
    }
    @Test
    @DisplayName("특정 부모 ID의 카테고리 조회 테스트 (GET /api/categories/{category-id})")
    void getCategoriesByParentIdTest() throws Exception {
        // given
        Long parentId = 1L;
        CategoryResponseDTO mockCategory = new CategoryResponseDTO(parentId, "부모 카테고리", null, null, 0, List.of(
                new CategoryResponseDTO(10L, "하위 카테고리 1"),
                new CategoryResponseDTO(11L, "하위 카테고리 2")
        ));

        when(categoryService.findById(anyLong())).thenReturn(mockCategory);

        // when & then
        mockMvc.perform(get("/api/categories/{category-id}", parentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parentId))
                .andExpect(jsonPath("$.name").value("부모 카테고리"))
                .andExpect(jsonPath("$.children", hasSize(2)))
                .andExpect(jsonPath("$.children[0].id").value(10L))
                .andExpect(jsonPath("$.children[0].name").value("하위 카테고리 1"))
                .andExpect(jsonPath("$.children[1].id").value(11L))
                .andExpect(jsonPath("$.children[1].name").value("하위 카테고리 2"));

        verify(categoryService, times(1)).findById(anyLong());
    }
}