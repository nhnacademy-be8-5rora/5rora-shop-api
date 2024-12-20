package store.aurora.book.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import store.aurora.book.entity.category.BookCategory;
import store.aurora.book.entity.category.Category;
import store.aurora.book.entity.tag.BookTag;
import store.aurora.book.entity.tag.Tag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int regularPrice;

    @Column(nullable = false)
    private int salePrice;

    private Integer stock = 100;

    @Column(nullable = false)
    private boolean isSale;

    @Column(nullable = false, unique = true, length = 15)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String contents;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    private boolean packaging = false;

    @Column(nullable = false)
    private LocalDate publishDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private Publisher publisher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private Series series;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookCategory> bookCategories = new ArrayList<>();

    public List<Category> getCategories() {
        return bookCategories.stream()
                .map(BookCategory::getCategory)
                .toList();
    }

    public void addBookCategory(BookCategory bookCategory) {
        if (!bookCategories.contains(bookCategory)) {
            bookCategories.add(bookCategory);
            bookCategory.setBook(this);
        }
    }
    public void removeBookCategory(BookCategory bookCategory) {
        if (bookCategories.contains(bookCategory)) {
            bookCategories.remove(bookCategory);
            bookCategory.setBook(null);
        }
    }
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookTag> bookTags;

}
