package com.wattson.data.repository;

import com.wattson.data.local.dao.ChatMessageDao;
import com.wattson.data.local.dao.RepairConversationDao;
import com.wattson.data.local.service.MockRepairAssistantService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RepairChatRepository_Factory implements Factory<RepairChatRepository> {
  private final Provider<RepairConversationDao> conversationDaoProvider;

  private final Provider<ChatMessageDao> messageDaoProvider;

  private final Provider<MockRepairAssistantService> mockAssistantProvider;

  public RepairChatRepository_Factory(Provider<RepairConversationDao> conversationDaoProvider,
      Provider<ChatMessageDao> messageDaoProvider,
      Provider<MockRepairAssistantService> mockAssistantProvider) {
    this.conversationDaoProvider = conversationDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.mockAssistantProvider = mockAssistantProvider;
  }

  @Override
  public RepairChatRepository get() {
    return newInstance(conversationDaoProvider.get(), messageDaoProvider.get(), mockAssistantProvider.get());
  }

  public static RepairChatRepository_Factory create(
      Provider<RepairConversationDao> conversationDaoProvider,
      Provider<ChatMessageDao> messageDaoProvider,
      Provider<MockRepairAssistantService> mockAssistantProvider) {
    return new RepairChatRepository_Factory(conversationDaoProvider, messageDaoProvider, mockAssistantProvider);
  }

  public static RepairChatRepository newInstance(RepairConversationDao conversationDao,
      ChatMessageDao messageDao, MockRepairAssistantService mockAssistant) {
    return new RepairChatRepository(conversationDao, messageDao, mockAssistant);
  }
}
