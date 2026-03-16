package com.wattson.ui.screens.premium;

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
public final class PremiumViewModel_Factory implements Factory<PremiumViewModel> {
  @Override
  public PremiumViewModel get() {
    return newInstance();
  }

  public static PremiumViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PremiumViewModel newInstance() {
    return new PremiumViewModel();
  }

  private static final class InstanceHolder {
    private static final PremiumViewModel_Factory INSTANCE = new PremiumViewModel_Factory();
  }
}
