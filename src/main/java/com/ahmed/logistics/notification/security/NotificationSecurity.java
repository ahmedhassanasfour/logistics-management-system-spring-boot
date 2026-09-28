package com.ahmed.logistics.notification.security;

import com.ahmed.logistics.customer.entity.Customer;
import com.ahmed.logistics.customer.repository.CustomerRepository;
import com.ahmed.logistics.notification.repository.NotificationRepository;
import com.ahmed.logistics.user.entity.User;
import com.ahmed.logistics.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("notificationSecurity")
@RequiredArgsConstructor
public class NotificationSecurity {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final CustomerRepository customerRepository;

    public boolean canAccessNotification(Long notificationId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || notificationId == null) {
            return false;
        }

        User user;
        try {
            user = userService.findEntityByEmail(authentication.getName());
        } catch (Exception e) {
            return false;
        }

        List<Long> userIds = new ArrayList<>();
        userIds.add(user.getId());
        customerRepository.findByUserId(user.getId())
                .map(Customer::getId)
                .ifPresent(userIds::add);

        return notificationRepository.findById(notificationId)
                .map(notification -> userIds.contains(notification.getUserId()))
                .orElse(false);
    }
}
