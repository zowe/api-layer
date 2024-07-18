package org.zowe.apiml.client.model.graphql;

import java.util.Arrays;
import java.util.List;

public record Author (String authorId, String firstName, String lastName) {

    private static List<Author> authors = Arrays.asList(
        new Author("author-1", "Joshua", "Bloch"),
        new Author("author-2", "Douglas", "Adams"),
        new Author("author-3", "Bill", "Bryson")
    );

    public static Author getById(String id) {
        return authors.stream()
            .filter(author -> author.authorId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
