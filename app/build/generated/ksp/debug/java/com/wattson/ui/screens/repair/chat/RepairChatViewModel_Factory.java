package com.wattson.ui.screens.repair.chat;

import androidx.lifecycle.SavedStateHandle;
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
public final class RepairChatViewModel_Factory implements Factory<RepairChatViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<RepairChatRepository> repairChatRepositoryProvider;

  public RepairChatViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<RepairChatRepository> repairChatRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.repairChatRepositoryProvider = repairChatRepositoryProvider;
  }

  @Override
  public RepairChatViewModel get() {
    return newInstance(savedStateHandleProvider.get(), repairChatRepositoryProvider.get());
  }

  public static RepairChatViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<RepairChatRepository> repairChatRepositoryProvider) {
    return new RepairChatViewModel_Factory(savedStateHandleProvider, repairChatRepositoryProvider);
  }

  public static RepairChatViewModel newInstance(SavedStateHandle savedStateHandle,
      RepairChatRepository repairChatRepository) {
    return new RepairChatViewModel(savedStateHandle, repairChatRepository);
  }
}
