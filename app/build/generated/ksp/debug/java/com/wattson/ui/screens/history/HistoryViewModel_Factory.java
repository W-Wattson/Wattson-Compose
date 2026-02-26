package com.wattson.ui.screens.history;

import com.wattson.data.repository.AuthRepository;
import com.wattson.data.repository.ProductRepository;
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
public final class HistoryViewModel_Factory implements Factory<HistoryViewModel> {
  private final Provider<ProductRepository> productRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public HistoryViewModel_Factory(Provider<ProductRepository> productRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.productRepositoryProvider = productRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public HistoryViewModel get() {
    return newInstance(productRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static HistoryViewModel_Factory create(
      Provider<ProductRepository> productRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new HistoryViewModel_Factory(productRepositoryProvider, authRepositoryProvider);
  }

  public static HistoryViewModel newInstance(ProductRepository productRepository,
      AuthRepository authRepository) {
    return new HistoryViewModel(productRepository, authRepository);
  }
}
