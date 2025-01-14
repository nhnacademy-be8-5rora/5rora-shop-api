package store.aurora.book.repository.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import store.aurora.book.entity.tag.Tag;

import java.util.List;
import java.util.Optional;


public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    List<Tag> findByNameContaining(String keyword);
}

