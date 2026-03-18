package com.wattson.di;

import com.wattson.data.remote.AuthInterceptor;
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
public final class NetworkModule_ProvideAuthInterceptorFactory implements Factory<AuthInterceptor> {
  private final Provider<TokenProvider> tokenProvider;

  public NetworkModule_ProvideAuthInterceptorFactory(Provider<TokenProvider> tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public AuthInterceptor get() {
    return provideAuthInterceptor(tokenProvider.get());
  }

  public static NetworkModule_ProvideAuthInterceptorFactory create(
      Provider<TokenProvider> tokenProvider) {
    return new NetworkModule_ProvideAuthInterceptorFactory(tokenProvider);
  }

  public static AuthInterceptor provideAuthInterceptor(TokenProvider tokenProvider) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAuthInterceptor(tokenProvider));
  }
}
