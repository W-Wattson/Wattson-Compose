package com.wattson.data.repository;

import com.wattson.data.remote.api.WattsonApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ProductRepository_Factory implements Factory<ProductRepository> {
  private final Provider<WattsonApi> apiProvider;

  public ProductRepository_Factory(Provider<WattsonApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public ProductRepository get() {
    return newInstance(apiProvider.get());
  }

  public static ProductRepository_Factory create(Provider<WattsonApi> apiProvider) {
    return new ProductRepository_Factory(apiProvider);
  }

  public static ProductRepository newInstance(WattsonApi api) {
    return new ProductRepository(api);
  }
}
