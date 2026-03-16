package com.wattson.data.local.service;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class MockRepairAssistantService_Factory implements Factory<MockRepairAssistantService> {
  @Override
  public MockRepairAssistantService get() {
    return newInstance();
  }

  public static MockRepairAssistantService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MockRepairAssistantService newInstance() {
    return new MockRepairAssistantService();
  }

  private static final class InstanceHolder {
    private static final MockRepairAssistantService_Factory INSTANCE = new MockRepairAssistantService_Factory();
  }
}
