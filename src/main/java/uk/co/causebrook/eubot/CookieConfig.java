package uk.co.causebrook.eubot;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.HandshakeResponse;
import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CookieConfig {
    private final Path cookieFile;

    private static final Logger logger = Logger.getLogger("connection-log");

    public CookieConfig(Path cookieFile) {
        this.cookieFile = cookieFile;
    }

    public CookieConfig(String cookieFile) {
        this.cookieFile = Paths.get(cookieFile);
    }

    public ClientEndpointConfig.Configurator get() {
        return new ClientEndpointConfig.Configurator() {
            @Override
            public synchronized void beforeRequest(Map<String, List<String>> headers) {
                if(Files.exists(cookieFile)) {
                    try {
                        String cookieStr = new String(Files.readAllBytes(cookieFile), Charset.defaultCharset());
                        List<HttpCookie> cookies = HttpCookie.parse(cookieStr);
                        List<String> cookieStrList = cookies.stream().map(HttpCookie::toString).collect(Collectors.toList());
                        headers.put("Cookie", cookieStrList);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Could not read from cookie file " + cookieFile.toAbsolutePath().toString() + ".", e);
                    } catch (IllegalArgumentException e) {
                        logger.log(Level.WARNING, "Cookie file " + cookieFile.toAbsolutePath().toString() + " is malformed.", e);
                    }
                }
            }

            @Override
            public synchronized void afterResponse(HandshakeResponse response) {
                if(response.getHeaders().containsKey("Set-Cookie")) {
                    String cookies = response.getHeaders().get("Set-Cookie").get(0);
                    try {
                        Files.write(cookieFile, cookies.getBytes());
                        logger.info("Written cookie to cookie file " + cookieFile.toAbsolutePath().toString() + ".");
                    } catch(IOException e) {
                        logger.log(Level.WARNING, "Could not write cookie to cookie file " + cookieFile.toAbsolutePath().toString() + ".", e);
                    }
                }
            }
        };
    }

}


