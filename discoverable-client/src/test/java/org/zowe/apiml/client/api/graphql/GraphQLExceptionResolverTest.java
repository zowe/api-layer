package org.zowe.apiml.client.api.graphql;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zowe.apiml.client.exception.BookAlreadyExistsException;
import org.zowe.apiml.client.exception.BookNotFoundException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GraphQLExceptionResolverTest {

    private GraphQLExceptionResolver resolver;

    @Mock
    private DataFetchingEnvironment env;

    @Mock
    private ExecutionStepInfo executionStepInfo;

    @Mock
    private ResultPath resultPath;

    @Mock
    private SourceLocation sourceLocation;

    @Mock
    private Field field;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new GraphQLExceptionResolver();

        // Mock ExecutionStepInfo and its methods
        when(env.getExecutionStepInfo()).thenReturn(executionStepInfo);
        when(executionStepInfo.getPath()).thenReturn(resultPath);
        when(resultPath.toList()).thenReturn(Collections.singletonList("testPath"));

        // Mock SourceLocation and its methods
        when(env.getField()).thenReturn(field);
        when(env.getField().getSourceLocation()).thenReturn(sourceLocation);
    }

    @Test
    void resolveToSingleError_BookAlreadyExistsException() {
        Throwable ex = new BookAlreadyExistsException();

        GraphQLError error = resolver.resolveToSingleError(ex, env);

        assertEquals("A book with the given name, page count, and author ID already exists.", error.getMessage());
        assertEquals("BAD_REQUEST", error.getErrorType().toString());
    }

    @Test
    void resolveToSingleError_BookNotFoundException() {
        Throwable ex = new BookNotFoundException();

        GraphQLError error = resolver.resolveToSingleError(ex, env);

        assertEquals("Book has not been found", error.getMessage());
        assertEquals("BAD_REQUEST", error.getErrorType().toString());
    }

    @Test
    void resolveToSingleError_UnexpectedException() {
        Throwable ex = new RuntimeException("Unexpected error");

        GraphQLError error = resolver.resolveToSingleError(ex, env);

        assertTrue(error.getMessage().contains("An unexpected error occurred: Unexpected error"));
        assertEquals("INTERNAL_ERROR", error.getErrorType().toString());
    }
}
