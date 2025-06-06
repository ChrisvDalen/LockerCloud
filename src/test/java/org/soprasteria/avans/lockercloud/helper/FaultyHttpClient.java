package org.soprasteria.avans.lockercloud.helper;

import java.io.IOException;
import java.net.URI;
import java.net.ProxySelector;
import java.net.CookieHandler;
import java.net.Authenticator;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple HttpClient wrapper that throws an IOException for the first
 * configured calls to simulate network failures such as connection resets.
 */
public class FaultyHttpClient extends HttpClient {
    private final HttpClient delegate;
    private final AtomicInteger remainingFails;

    public FaultyHttpClient(HttpClient delegate, int failAttempts) {
        this.delegate = delegate;
        this.remainingFails = new AtomicInteger(failAttempts);
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        if (remainingFails.getAndDecrement() > 0) {
            throw new IOException("Simulated network reset");
        }
        return delegate.send(request, responseBodyHandler);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        if (remainingFails.getAndDecrement() > 0) {
            CompletableFuture<HttpResponse<T>> cf = new CompletableFuture<>();
            cf.completeExceptionally(new IOException("Simulated network reset"));
            return cf;
        }
        return delegate.sendAsync(request, responseBodyHandler);
    }

    // Delegate the remaining methods
    @Override public Optional<CookieHandler> cookieHandler() { return delegate.cookieHandler(); }
    @Override public Optional<Duration> connectTimeout() { return delegate.connectTimeout(); }
    @Override public Redirect followRedirects() { return delegate.followRedirects(); }
    @Override public ProxySelector proxy() { return delegate.proxy(); }
    @Override public SSLContext sslContext() { return delegate.sslContext(); }
    @Override public SSLParameters sslParameters() { return delegate.sslParameters(); }
    @Override public Optional<Authenticator> authenticator() { return delegate.authenticator(); }
    @Override public Version version() { return delegate.version(); }
    @Override public Optional<Executor> executor() { return delegate.executor(); }
}
