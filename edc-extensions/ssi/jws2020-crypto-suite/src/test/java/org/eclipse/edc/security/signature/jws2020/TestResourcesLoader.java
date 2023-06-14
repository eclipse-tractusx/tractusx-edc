package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;

import java.io.IOException;
import java.net.URI;

class TestResourcesLoader implements DocumentLoader {
    private final String base;
    private final DocumentLoader baseLoader;
    private final String resourcePath;

    public TestResourcesLoader(String base, String resourcePath, DocumentLoader baseLoader) {
        this.base = base;
        this.resourcePath = resourcePath;
        this.baseLoader = baseLoader;
    }

    @Override
    public Document loadDocument(URI uri, DocumentLoaderOptions options) throws JsonLdError {
        Document document;
        var url = uri.toString();
        if (url.startsWith(base)) {
            try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(rewrite(uri))) {
                document = JsonDocument.of(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            document = baseLoader.loadDocument(uri, options);
        }
        return document;
    }

    private String rewrite(URI url) {
        var path = resourcePath + url.toString().replace(base, "");
        if (!path.endsWith(".json")) {
            path += ".json";
        }
        return path;
    }
}
