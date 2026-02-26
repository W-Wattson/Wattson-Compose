package com.wattson.di;

import com.wattson.data.local.AppDatabase;
import com.wattson.data.local.dao.RepairConversationDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DatabaseModule_ProvideRepairConversationDaoFactory implements Factory<RepairConversationDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_ProvideRepairConversationDaoFactory(
      Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public RepairConversationDao get() {
    return provideRepairConversationDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideRepairConversationDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_ProvideRepairConversationDaoFactory(databaseProvider);
  }

  public static RepairConversationDao provideRepairConversationDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRepairConversationDao(database));
  }
}
