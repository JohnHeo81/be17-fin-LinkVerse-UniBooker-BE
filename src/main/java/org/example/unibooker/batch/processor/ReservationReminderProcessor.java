package org.example.unibooker.batch.processor;

import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.batch.reader.ReservationReminderReader.ReminderItem;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * 리마인더 Processor
 * - 현재는 그대로 통과 (추후 필터링 로직 추가 가능)
 */
@Slf4j
@Component
public class ReservationReminderProcessor implements ItemProcessor<ReminderItem, ReminderItem> {

    @Override
    public ReminderItem process(ReminderItem item) {
        // 추후 필터링 로직 추가 가능
        // 예: 취소된 예약 제외, 특정 조건 제외 등
        return item;
    }
}