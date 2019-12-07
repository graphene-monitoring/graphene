package com.graphene.reader.exceptions;

/**
 * @author Andrei Ivanov
 */
public class MissingParameterException extends ParameterParsingException {

    public MissingParameterException(String message) {
        super(message);
    }
}
