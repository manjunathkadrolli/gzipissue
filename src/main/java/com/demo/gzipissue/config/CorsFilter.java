package com.demo.gzipissue.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

@Component
@Order(1)
public class CorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {

            // request body.
            if (request.getHeader(HttpHeaders.CONTENT_ENCODING) != null
                    && request.getHeader(HttpHeaders.CONTENT_ENCODING).contains("gzip")) {

                logger.debug("Recieved Content-Encoding:Gzip so will decompress the request.");
                request = new CorsFilter.GzippedInputStreamWrapper(request);
            }

            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");

            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
            if ("OPTIONS".equals(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                filterChain.doFilter(request, response);
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Failed to capture the request parameters.!!!");
            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }

    /**
     * Wrapper class that detects if the request is gzipped and ungzipps it.
     */
    final class GzippedInputStreamWrapper extends HttpServletRequestWrapper {

        /**
         * Serialized bytes array that is a result of unzipping gzipped body.
         */
        private byte[] bytes;

        /**
         * Constructs a request object wrapping the given request. In case if
         * Content-Encoding contains "gzip" we wrap the input stream into byte array to
         * original input stream has nothing in it but hew wrapped input stream always
         * returns reproducible ungzipped input stream.
         *
         * @param request request which input stream will be wrapped.
         * @throws java.io.IOException when input stream reqtieval failed.
         */
        public GzippedInputStreamWrapper(final HttpServletRequest request) throws IOException {
            super(request);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPInputStream in = new GZIPInputStream(request.getInputStream());
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) >= 0) {
                bos.write(buffer, 0, len);
            }
            in.close();
            bos.close();
            bytes = bos.toByteArray();
        }

        /**
         * @return reproduceable input stream that is either equal to initial servlet
         * input stream(if it was not zipped) or returns unzipped input stream.
         */
        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream sourceStream = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {
                public int read() {
                    return sourceStream.read();
                }

                public void close() throws IOException {
                    super.close();
                    sourceStream.close();
                }

                //@Override
                public boolean isFinished() {
                    // TODO Auto-generated method stub
                    return false;
                }

                //@Override
                public boolean isReady() {
                    // TODO Auto-generated method stub
                    return false;
                }

                //@Override
                public void setReadListener(ReadListener listener) {
                    // TODO Auto-generated method stub

                }
            };
        }

    }
}
