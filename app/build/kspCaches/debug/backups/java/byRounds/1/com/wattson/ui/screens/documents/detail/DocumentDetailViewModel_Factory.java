package com.wattson.ui.screens.documents.detail;

import androidx.lifecycle.SavedStateHandle;
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
public final class DocumentDetailViewModel_Factory implements Factory<DocumentDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public DocumentDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public DocumentDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get());
  }

  public static DocumentDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new DocumentDetailViewModel_Factory(savedStateHandleProvider);
  }

  public static DocumentDetailViewModel newInstance(SavedStateHandle savedStateHandle) {
    return new DocumentDetailViewModel(savedStateHandle);
  }
}
