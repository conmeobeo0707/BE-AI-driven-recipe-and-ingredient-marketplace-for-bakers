package com.manhnv.capstonebakerai.notification.mapper;

import com.manhnv.capstonebakerai.notification.dto.response.NotificationResponse;
import com.manhnv.capstonebakerai.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
