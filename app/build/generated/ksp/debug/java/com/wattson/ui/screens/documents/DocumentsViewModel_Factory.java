package com.wattson.ui.screens.documents;

import com.wattson.data.repository.AuthRepository;
import com.wattson.data.repository.DocumentRepository;
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
public final class DocumentsViewModel_Factory implements Factory<DocumentsViewModel> {
  private final Provider<DocumentRepository> documentRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public DocumentsViewModel_Factory(Provider<DocumentRepository> documentRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.documentRepositoryProvider = documentRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public DocumentsViewModel get() {
    return newInstance(documentRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static DocumentsViewModel_Factory create(
      Provider<DocumentRepository> documentRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new DocumentsViewModel_Factory(documentRepositoryProvider, authRepositoryProvider);
  }

  public static DocumentsViewModel newInstance(DocumentRepository documentRepository,
      AuthRepository authRepository) {
    return new DocumentsViewModel(documentRepository, authRepository);
  }
}
