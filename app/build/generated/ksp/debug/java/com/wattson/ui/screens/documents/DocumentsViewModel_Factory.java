package com.wattson.ui.screens.documents;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
  @Override
  public DocumentsViewModel get() {
    return newInstance();
  }

  public static DocumentsViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DocumentsViewModel newInstance() {
    return new DocumentsViewModel();
  }

  private static final class InstanceHolder {
    private static final DocumentsViewModel_Factory INSTANCE = new DocumentsViewModel_Factory();
  }
}
