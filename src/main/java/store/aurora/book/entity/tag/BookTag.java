package store.aurora.book.entity.tag;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import store.aurora.book.entity.Book;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "book_tags")
public class BookTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    public BookTag(Tag tag) {
        this.tag = tag;
    }

    public BookTag(Tag tag,Book book) {
        this.book = book;
        this.tag = tag;
    }
}
