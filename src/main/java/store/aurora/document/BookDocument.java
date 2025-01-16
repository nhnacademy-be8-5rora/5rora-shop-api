package store.aurora.document;

import org.springframework.data.elasticsearch.annotations.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "5rora")  // Elasticsearch의 books 인덱스에 저장
public class BookDocument {
    private Long id;

    private String title;

    private int regularPrice;

    private int salePrice;

    private Integer stock = 100;

    private boolean isSale;

    private String isbn;

    private String contents;

    private String explanation;

    private List<AuthorDocument> authors;

    private boolean packaging = false;

    private String publishDate;

    private String coverImage;

    // Publisher, Series 등 다른 관계들은 Object로 처리할 수 있습니다.
    private PublisherDocument publisher;

    private SeriesDocument series;

    private List<CategoryDocument> categories = new ArrayList<>();

    private List<TagDocument> bookTags = new ArrayList<>();

    private List<BookImageDocument> bookImages = new ArrayList<>();

    private boolean active;
}
