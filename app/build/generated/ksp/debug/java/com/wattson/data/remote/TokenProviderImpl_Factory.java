package com.wattson.data.remote;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class TokenProviderImpl_Factory implements Factory<TokenProviderImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<String> baseUrlProvider;

  public TokenProviderImpl_Factory(Provider<Context> contextProvider,
      Provider<String> baseUrlProvider) {
    this.contextProvider = contextProvider;
    this.baseUrlProvider = baseUrlProvider;
  }

  @Override
  public TokenProviderImpl get() {
    return newInstance(contextProvider.get(), baseUrlProvider.get());
  }

  public static TokenProviderImpl_Factory create(Provider<Context> contextProvider,
      Provider<String> baseUrlProvider) {
    return new TokenProviderImpl_Factory(contextProvider, baseUrlProvider);
  }

  public static TokenProviderImpl newInstance(Context context, String baseUrl) {
    return new TokenProviderImpl(context, baseUrl);
  }
}
