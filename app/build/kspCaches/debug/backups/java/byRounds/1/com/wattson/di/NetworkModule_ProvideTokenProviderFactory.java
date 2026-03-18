package com.wattson.di;

import android.content.Context;
import com.wattson.data.remote.TokenProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NetworkModule_ProvideTokenProviderFactory implements Factory<TokenProvider> {
  private final Provider<Context> contextProvider;

  private final Provider<String> baseUrlProvider;

  public NetworkModule_ProvideTokenProviderFactory(Provider<Context> contextProvider,
      Provider<String> baseUrlProvider) {
    this.contextProvider = contextProvider;
    this.baseUrlProvider = baseUrlProvider;
  }

  @Override
  public TokenProvider get() {
    return provideTokenProvider(contextProvider.get(), baseUrlProvider.get());
  }

  public static NetworkModule_ProvideTokenProviderFactory create(Provider<Context> contextProvider,
      Provider<String> baseUrlProvider) {
    return new NetworkModule_ProvideTokenProviderFactory(contextProvider, baseUrlProvider);
  }

  public static TokenProvider provideTokenProvider(Context context, String baseUrl) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideTokenProvider(context, baseUrl));
  }
}
