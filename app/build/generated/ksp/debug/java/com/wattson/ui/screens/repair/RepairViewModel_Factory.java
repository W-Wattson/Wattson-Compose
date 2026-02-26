package com.wattson.ui.screens.repair;

import com.wattson.data.repository.AuthRepository;
import com.wattson.data.repository.RepairChatRepository;
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
public final class RepairViewModel_Factory implements Factory<RepairViewModel> {
  private final Provider<RepairChatRepository> repairChatRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public RepairViewModel_Factory(Provider<RepairChatRepository> repairChatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.repairChatRepositoryProvider = repairChatRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public RepairViewModel get() {
    return newInstance(repairChatRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static RepairViewModel_Factory create(
      Provider<RepairChatRepository> repairChatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new RepairViewModel_Factory(repairChatRepositoryProvider, authRepositoryProvider);
  }

  public static RepairViewModel newInstance(RepairChatRepository repairChatRepository,
      AuthRepository authRepository) {
    return new RepairViewModel(repairChatRepository, authRepository);
  }
}
