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
public final class DocumentRepository_Factory implements Factory<DocumentRepository> {
  private final Provider<WattsonApi> apiProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<Context> contextProvider;

  public DocumentRepository_Factory(Provider<WattsonApi> apiProvider,
      Provider<AuthRepository> authRepositoryProvider, Provider<Context> contextProvider) {
    this.apiProvider = apiProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public DocumentRepository get() {
    return newInstance(apiProvider.get(), authRepositoryProvider.get(), contextProvider.get());
  }

  public static DocumentRepository_Factory create(Provider<WattsonApi> apiProvider,
      Provider<AuthRepository> authRepositoryProvider, Provider<Context> contextProvider) {
    return new DocumentRepository_Factory(apiProvider, authRepositoryProvider, contextProvider);
  }

  public static DocumentRepository newInstance(WattsonApi api, AuthRepository authRepository,
      Context context) {
    return new DocumentRepository(api, authRepository, context);
  }
}
