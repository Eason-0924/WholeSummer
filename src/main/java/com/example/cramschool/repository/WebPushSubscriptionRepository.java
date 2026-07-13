package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.cramschool.entity.WebPushSubscription;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

	Optional<WebPushSubscription> findByEndpointHash(String endpointHash);

	Optional<WebPushSubscription> findByUserIdAndEndpointHash(Long userId, String endpointHash);

	List<WebPushSubscription> findByUserIdAndEnabledTrueAndVapidKeyHashOrderByUpdatedAtDesc(Long userId,
			String vapidKeyHash);

	@Modifying
	@Query("""
			UPDATE WebPushSubscription subscription
			SET subscription.enabled = false
			WHERE subscription.userId = :userId
				AND subscription.enabled = true
				AND (subscription.vapidKeyHash IS NULL OR subscription.vapidKeyHash <> :vapidKeyHash)
			""")
	void disableSubscriptionsWithDifferentVapidKey(@Param("userId") Long userId,
			@Param("vapidKeyHash") String vapidKeyHash);
}
