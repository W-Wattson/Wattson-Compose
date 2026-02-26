package com.wattson.ui.screens.repair;

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
public final class RepairViewModel_Factory implements Factory<RepairViewModel> {
  @Override
  public RepairViewModel get() {
    return newInstance();
  }

  public static RepairViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RepairViewModel newInstance() {
    return new RepairViewModel();
  }

  private static final class InstanceHolder {
    private static final RepairViewModel_Factory INSTANCE = new RepairViewModel_Factory();
  }
}
