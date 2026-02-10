package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.chats.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    
    List<PrivateMessage> findByChatRoomIdAndIsDeletedFalseOrderBySentAtAsc(Long chatRoomId);
    
    @Query("SELECT m FROM PrivateMessage m WHERE " +
           "m.receiverId = :userId AND m.status != 'READ' AND m.isDeleted = false")
    List<PrivateMessage> findUnreadMessages(@Param("userId") Long userId);
    
    @Query("SELECT m FROM PrivateMessage m WHERE " +
           "m.chatRoomId = :chatRoomId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND m.isDeleted = false " +
           "ORDER BY m.sentAt DESC")
    List<PrivateMessage> searchInChatRoom(@Param("chatRoomId") Long chatRoomId, 
                                         @Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(m) FROM PrivateMessage m WHERE " +
           "m.receiverId = :userId AND m.senderId = :friendId " +
           "AND m.status != 'READ' AND m.isDeleted = false")
    Long countUnreadFromFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
}