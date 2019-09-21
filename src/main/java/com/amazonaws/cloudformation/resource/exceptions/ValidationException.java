/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.cloudformation.resource.exceptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 42L;

    /**
     * Error messages thrown for these keywords don't contain values
     */
    private static final List<String> SAFE_KEYWORDS = Arrays.asList(
        // object keywords
        "required", "minProperties", "maxProperties", "dependencies", "additionalProperties",
        // string keywords
        "minLength", "maxLength",
        // array keywords
        "minItems", "maxItems", "uniqueItems", "contains",
        // misc keywords
        "type", "allOf", "anyOf", "oneOf");

    private final List<ValidationException> causingExceptions;
    private final String keyword;
    private final String schemaPointer;

    public ValidationException(final org.everit.json.schema.ValidationException validationException) {
        this(validationException.getMessage(), validationException);
    }

    public ValidationException(final String errorMessage,
                               final org.everit.json.schema.ValidationException validationException) {
        super(errorMessage);
        this.keyword = validationException.getKeyword();
        this.schemaPointer = validationException.getPointerToViolation();

        final List<ValidationException> causingExceptions = new ArrayList<>();
        if (validationException.getCausingExceptions() != null) {
            for (final org.everit.json.schema.ValidationException e : validationException.getCausingExceptions()) {
                causingExceptions.add(newScrubbedException(e));
            }
        }
        this.causingExceptions = Collections.unmodifiableList(causingExceptions);
    }

    public ValidationException(final String message,
                               final String keyword,
                               final String schemaPointer) {
        this(message, Collections.emptyList(), keyword, schemaPointer);
    }

    public ValidationException(final String message,
                               final List<ValidationException> causingExceptions,
                               final String keyword,
                               final String schemaPointer) {
        super(message);
        this.causingExceptions = Collections.unmodifiableList(causingExceptions);
        this.keyword = keyword;
        this.schemaPointer = schemaPointer;
    }

    /**
     * In order to ensure sensitive properties aren't displayed, scrub any error
     * messages that emit property values
     */
    public static ValidationException newScrubbedException(final org.everit.json.schema.ValidationException e) {
        // A parent exception has multiple errors in the subSchema, and will just emit
        // "{X} schema validations found"
        final boolean isParentException = e.getKeyword() == null && e.getCausingExceptions() != null
            && !e.getCausingExceptions().isEmpty();
        if (isParentException || SAFE_KEYWORDS.contains(e.getKeyword())) {
            return new ValidationException(e);
        } else {
            final String errorMessage = String.format("%s: failed validation constraint for keyword [%s]",
                e.getPointerToViolation(), e.getKeyword());

            return new ValidationException(errorMessage, e);
        }
    }

    public static String buildFullExceptionMessage(final ValidationException e) {
        return buildFullExceptionMessageHelper(e).trim();
    }

    private static String buildFullExceptionMessageHelper(final ValidationException e) {
        StringBuilder builder = new StringBuilder();
        final boolean isParentException = e.getKeyword() == null && e.getCausingExceptions() != null
            && !e.getCausingExceptions().isEmpty();
        if (!isParentException) {
            builder.append(e.getMessage() + "\n");
        }
        for (ValidationException cause : e.getCausingExceptions()) {
            builder.append(buildFullExceptionMessageHelper(cause));
        }
        return builder.toString();
    }
}
