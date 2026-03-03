package com.wattson.ui.screens.product;

import androidx.lifecycle.SavedStateHandle;
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
public final class ProductDetailViewModel_Factory implements Factory<ProductDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ProductRepository> productRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public ProductDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ProductRepository> productRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.productRepositoryProvider = productRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ProductDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), productRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static ProductDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ProductRepository> productRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new ProductDetailViewModel_Factory(savedStateHandleProvider, productRepositoryProvider, authRepositoryProvider);
  }

  public static ProductDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      ProductRepository productRepository, AuthRepository authRepository) {
    return new ProductDetailViewModel(savedStateHandle, productRepository, authRepository);
  }
}
