package com.wattson.ui.screens.documents.detail;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import com.wattson.data.repository.AuthRepository;
import com.wattson.data.repository.DocumentRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DocumentDetailViewModel_Factory implements Factory<DocumentDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<DocumentRepository> documentRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<Context> appContextProvider;

  public DocumentDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<DocumentRepository> documentRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider, Provider<Context> appContextProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.documentRepositoryProvider = documentRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public DocumentDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), documentRepositoryProvider.get(), authRepositoryProvider.get(), appContextProvider.get());
  }

  public static DocumentDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<DocumentRepository> documentRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider, Provider<Context> appContextProvider) {
    return new DocumentDetailViewModel_Factory(savedStateHandleProvider, documentRepositoryProvider, authRepositoryProvider, appContextProvider);
  }

  public static DocumentDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      DocumentRepository documentRepository, AuthRepository authRepository, Context appContext) {
    return new DocumentDetailViewModel(savedStateHandle, documentRepository, authRepository, appContext);
  }
}
