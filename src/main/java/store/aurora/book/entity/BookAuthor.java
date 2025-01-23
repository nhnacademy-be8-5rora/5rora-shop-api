package store.aurora.book.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "book_authors")
public class BookAuthor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @ManyToOne
    @JoinColumn(name = "author_role_id", nullable = false)
    private AuthorRole authorRole;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    public BookAuthor(Author author, AuthorRole authorRole, Book book) {
        this.author = author;
        this.authorRole = authorRole;
        this.book = book;
    }

}
