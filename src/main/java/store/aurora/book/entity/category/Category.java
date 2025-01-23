package store.aurora.book.entity.category;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import store.aurora.book.entity.Book;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    private Integer depth;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookCategory> bookCategories = new ArrayList<>();

    public void addChild(Category child) {
        if (this.depth >= 2) {
            throw new IllegalStateException("더 이상 하위 계층을 추가할 수 없습니다.");
        }
        children.add(child);
        child.setParent(this);
        child.setDepth(this.depth + 1);
    }

    public void addBookCategory(BookCategory bookCategory) {
            bookCategories.add(bookCategory);
            bookCategory.setCategory(this);
    }

    // 카테고리에서 직접 책 리스트 조회
    public List<Book> getBooks() {
        return bookCategories.stream()
                .map(BookCategory::getBook)
                .toList();
    }
    public Category(String name, Category parent, Integer depth) {
        this.name = name;
        this.parent = parent;
        this.depth = depth;

    }
}

