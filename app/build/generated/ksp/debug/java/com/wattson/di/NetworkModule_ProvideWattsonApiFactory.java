package com.wattson.di;

import com.wattson.data.remote.api.WattsonApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideWattsonApiFactory implements Factory<WattsonApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideWattsonApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public WattsonApi get() {
    return provideWattsonApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideWattsonApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideWattsonApiFactory(retrofitProvider);
  }

  public static WattsonApi provideWattsonApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWattsonApi(retrofit));
  }
}
