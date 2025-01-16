package store.aurora.book.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import store.aurora.document.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SearchBookDTO {

    private Long id;
    private String title;
    private int regularPrice;
    private int salePrice;
    private Integer stock;
    private boolean isSale;
    private String isbn;
    private String contents;
    private String explanation;
    private List<AuthorDocument> authors;
    private boolean packaging;

    private String publishDate;

    private String coverImage;
    private PublisherDocument publisher;
    private List<CategoryDocument> categories;
    private List<TagDocument> bookTags;
    //디코딩 문제때문에 일단 주석처리. 리스트 조회시에는 db에서 썸네일 가져와서 오류는 나지않음.
//    private List<BookImageDocument> bookImages;
}
