package store.aurora.book.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.aurora.book.dto.author.ParsedAuthorDto;
import store.aurora.book.entity.BookAuthor;
import store.aurora.book.repository.author.AuthorRoleRepository;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthorParser {

    private final AuthorRoleRepository authorRoleRepository;
    private static final String DEFAULT_ROLE = "지은이";


    public List<ParsedAuthorDto> parseAuthors(String authorsString) {
        if (authorsString == null || authorsString.trim().isEmpty()) {
            return Collections.emptyList(); // null 또는 빈 문자열이면 빈 리스트 반환
        }
        List<ParsedAuthorDto> parsedAuthors = new ArrayList<>();
        List<String> pendingAuthors = new ArrayList<>();
        String currentRole = null;
        String[] entries = authorsString.split("\\s*,\\s*");


        for (String entry : entries) {
            if (entry.contains("(") && entry.contains(")")) {
                int roleStart = entry.lastIndexOf("(");
                int roleEnd = entry.lastIndexOf(")");
                currentRole = entry.substring(roleStart + 1, roleEnd).trim();

                String[] names = entry.substring(0, roleStart).split(", ");
                pendingAuthors.addAll(Arrays.asList(names));

                for (String author : pendingAuthors) {
                    parsedAuthors.add(new ParsedAuthorDto(author.trim(), currentRole));
                }
                pendingAuthors.clear();
            } else {
                pendingAuthors.add(entry.trim());
            }
        }
        for (String author : pendingAuthors) {
            parsedAuthors.add(new ParsedAuthorDto(author.trim(), DEFAULT_ROLE));
        }
        return parsedAuthors;
    }

    public String formatAuthors(List<BookAuthor> bookAuthors) {
        Map<String, List<String>> groupedAuthors = new LinkedHashMap<>();

        for (BookAuthor bookAuthor : bookAuthors) {
            String role = bookAuthor.getAuthorRole() != null ? bookAuthor.getAuthorRole().getRole() : DEFAULT_ROLE;
            String authorName = bookAuthor.getAuthor().getName();
            groupedAuthors.computeIfAbsent(role, k -> new ArrayList<>()).add(authorName);
        }

        return groupedAuthors.entrySet().stream()
                .map(entry -> {
                    String authors = String.join(", ", entry.getValue());
                    return authors + " (" + entry.getKey() + ")";
                })
                .collect(Collectors.joining(", "));
    }

}
