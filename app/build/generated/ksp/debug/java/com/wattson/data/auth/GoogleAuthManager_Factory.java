package com.wattson.data.auth;

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
public final class GoogleAuthManager_Factory implements Factory<GoogleAuthManager> {
  @Override
  public GoogleAuthManager get() {
    return newInstance();
  }

  public static GoogleAuthManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GoogleAuthManager newInstance() {
    return new GoogleAuthManager();
  }

  private static final class InstanceHolder {
    private static final GoogleAuthManager_Factory INSTANCE = new GoogleAuthManager_Factory();
  }
}
