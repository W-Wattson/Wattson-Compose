package com.wattson.di;

import com.wattson.data.remote.TokenAuthenticator;
import com.wattson.data.remote.TokenProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NetworkModule_ProvideTokenAuthenticatorFactory implements Factory<TokenAuthenticator> {
  private final Provider<TokenProvider> tokenProvider;

  public NetworkModule_ProvideTokenAuthenticatorFactory(Provider<TokenProvider> tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public TokenAuthenticator get() {
    return provideTokenAuthenticator(tokenProvider.get());
  }

  public static NetworkModule_ProvideTokenAuthenticatorFactory create(
      Provider<TokenProvider> tokenProvider) {
    return new NetworkModule_ProvideTokenAuthenticatorFactory(tokenProvider);
  }

  public static TokenAuthenticator provideTokenAuthenticator(TokenProvider tokenProvider) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideTokenAuthenticator(tokenProvider));
  }
}
