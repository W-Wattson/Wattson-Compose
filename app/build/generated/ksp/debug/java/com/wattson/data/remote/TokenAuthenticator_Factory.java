package com.wattson.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class TokenAuthenticator_Factory implements Factory<TokenAuthenticator> {
  private final Provider<TokenProvider> tokenProvider;

  public TokenAuthenticator_Factory(Provider<TokenProvider> tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public TokenAuthenticator get() {
    return newInstance(tokenProvider.get());
  }

  public static TokenAuthenticator_Factory create(Provider<TokenProvider> tokenProvider) {
    return new TokenAuthenticator_Factory(tokenProvider);
  }

  public static TokenAuthenticator newInstance(TokenProvider tokenProvider) {
    return new TokenAuthenticator(tokenProvider);
  }
}
