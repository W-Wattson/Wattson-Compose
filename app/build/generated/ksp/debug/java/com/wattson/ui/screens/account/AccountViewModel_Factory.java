package com.wattson.ui.screens.account;

import com.wattson.data.remote.api.WattsonApi;
import com.wattson.data.repository.AuthRepository;
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
public final class AccountViewModel_Factory implements Factory<AccountViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<WattsonApi> apiProvider;

  public AccountViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<WattsonApi> apiProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.apiProvider = apiProvider;
  }

  @Override
  public AccountViewModel get() {
    return newInstance(authRepositoryProvider.get(), apiProvider.get());
  }

  public static AccountViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<WattsonApi> apiProvider) {
    return new AccountViewModel_Factory(authRepositoryProvider, apiProvider);
  }

  public static AccountViewModel newInstance(AuthRepository authRepository, WattsonApi api) {
    return new AccountViewModel(authRepository, api);
  }
}
