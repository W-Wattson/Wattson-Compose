package com.wattson.data.repository;

import android.content.Context;
import com.wattson.data.remote.api.WattsonApi;
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<WattsonApi> apiProvider;

  public AuthRepository_Factory(Provider<Context> contextProvider,
      Provider<WattsonApi> apiProvider) {
    this.contextProvider = contextProvider;
    this.apiProvider = apiProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(contextProvider.get(), apiProvider.get());
  }

  public static AuthRepository_Factory create(Provider<Context> contextProvider,
      Provider<WattsonApi> apiProvider) {
    return new AuthRepository_Factory(contextProvider, apiProvider);
  }

  public static AuthRepository newInstance(Context context, WattsonApi api) {
    return new AuthRepository(context, api);
  }
}
