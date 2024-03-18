package com.demo.gzipissue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TestGzipIssue {

    public static void main(String[] args) {
        try {
            // API endpoint URL
            String apiUrl = "http://localhost:8080/processString";
            String jsonBody = " {\n" +
                    "  \"obj1\": 17.92,\n" +
                    "  \"obj2\": \"2031\",\n" +
                    "  \"obj3\": \"2024-02-07\",\n" +
                    "  \"obj4\": 5,\n" +
                    "  \"obj5\": 92,\n" +
                    "  \"obj6\": \"2024-02-07T12:18:40.253\",\n" +
                    "  \"obj7\": \"4000\",\n" +
                    "  \"obj8\": \"2024-02-07\",\n" +
                    "  \"obj9\": 116,\n" +
                    "  \"obj8\": \"LAKE\",\n" +
                    "  \"obj9\": false\n" +
                    "}";


            // Compress the JSON body using GZIP
            byte[] compressedBody = compressString1(jsonBody);

            System.out.println("DECOM:"+ decompress(compressedBody));

            // Create URL object
            URL url = new URL(apiUrl);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("PUT");

            // Set the content type and encoding
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Encoding", "gzip");
            // Enable input/output streams
            connection.setDoOutput(true);

            // Write the compressed body to the output stream
            try (OutputStream os = connection.getOutputStream()) {
                os.write(compressedBody);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response content
            String responseContent = readResponse(connection);
            System.out.println("Response Content: " + responseContent);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to compress a string to GZIP format
    private static byte[] compressString(String input) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {

            gzipStream.write(input.getBytes(StandardCharsets.UTF_8));

            return byteStream.toByteArray();
        }
    }

    private static byte[] compressString1(String input) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {

            gzipStream.write(input.getBytes());
            gzipStream.close();

            return byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decompress(final byte[] compressed) throws IOException {
        final StringBuilder outStr = new StringBuilder();
        if ((compressed == null) || (compressed.length == 0)) {
            return "";
        }
        if (isCompressed(compressed)) {
            final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outStr.append(line);
            }
        } else {
            outStr.append(compressed);
        }
        return outStr.toString();
    }

    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }


    // Method to read the response content from the connection
    private static String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();

        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            // If there's an error reading the input stream, you might want to check the error stream
            try (var errorReader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))) {

                String line;
                while ((line = errorReader.readLine()) != null) {
                    response.append(line);
                }
            }
        }

        return response.toString();
    }
}
