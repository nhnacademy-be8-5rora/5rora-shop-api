package store.aurora.book.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import store.aurora.user.entity.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "likes")
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private boolean isLike;

    public Like(Book book, User user, boolean isLike) {
        this.book = book;
        this.user = user;
        this.isLike = isLike;
    }
}
