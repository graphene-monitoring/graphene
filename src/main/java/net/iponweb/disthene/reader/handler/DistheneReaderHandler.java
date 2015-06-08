package net.iponweb.disthene.reader.handler;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import net.iponweb.disthene.reader.exceptions.MissingParameterException;
import net.iponweb.disthene.reader.exceptions.UnsupportedMethodException;

/**
 * @author Andrei Ivanov
 */
public interface DistheneReaderHandler {

    FullHttpResponse handle(HttpRequest request) throws UnsupportedMethodException, MissingParameterException;
}
